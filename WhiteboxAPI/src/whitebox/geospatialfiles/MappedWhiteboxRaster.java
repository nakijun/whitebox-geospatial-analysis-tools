package whitebox.geospatialfiles;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of WhiteboxRasterInterface that uses a memory mapped file for
 * accessing the disk.
 * @author Kevin Green <kevin.a.green@gmail.com>
 */
public class MappedWhiteboxRaster extends WhiteboxRasterBase implements WhiteboxRasterInterface {   
    
    private List<MappedByteBuffer> buffers;
    
    private static final int MAX_BUFFER_SIZE = 16777216; // 2^24 
    
    public MappedWhiteboxRaster(String headerFile, String fileAccess) {
        this(headerFile, fileAccess, false);
    }
    
    public MappedWhiteboxRaster(String headerFile, String fileAccess, boolean overwrite) {
        this.headerFile = headerFile;    
        this.dataFile = headerFile.replace(".dep", ".tas");
        this.statsFile = headerFile.replace(".dep", ".wstat");
        setFileAccess(fileAccess);
        readHeaderFile();
        
        File file = new File(dataFile);
        try {
            if (overwrite || !file.exists()) {
                buffers = createNewDataFile(fileAccess);
            } else {
                buffers = openDataFile(fileAccess);
            }
        } catch (IOException e) {
            // Log and ignore?
            System.out.println(e);
        }
        
        // Need to check if synchronous, if it is fileAccess should be "rwd" for direct

    }
    
    public MappedWhiteboxRaster(String headerFile, String fileAccess, String baseRasterHeader, DataType dataType, double initialValue) {
        this.headerFile = headerFile;    
        this.dataFile = headerFile.replace(".dep", ".tas");
        this.statsFile = headerFile.replace(".dep", ".wstat");
        readHeaderFile();
        setFileAccess(fileAccess);
        
        File file = new File(dataFile);
        try {
            if (!file.exists()) {
                buffers = createNewDataFile(fileAccess);
            } else {
                buffers = openDataFile(fileAccess);
            }
        } catch (IOException e) {
            System.out.println(e);
        }        
        setPropertiesUsingAnotherRaster(baseRasterHeader, dataType);
        
        this.initialValue = initialValue;
        
    }
    
    public MappedWhiteboxRaster(String headerFile, double north, double south, double east, double west, int rows, int cols, DataScale dataScale, DataType dataType, double initialValue, double noData) {
        this.headerFile = headerFile;
        dataFile = headerFile.replace(".dep", ".tas");
        statsFile = headerFile.replace(".dep", ".wstat");
        File f1 = new File(this.headerFile);
        f1.delete();
        f1 = new File(this.dataFile);
        f1.delete();
        f1 = new File(this.statsFile);
        f1.delete();
        
        this.north = north;
        this.south = south;
        this.east = east;
        this.west = west;
        this.numberRows = rows;
        this.numberColumns = cols;
        this.dataScale = dataScale;
        setDataType(dataType);
        this.noDataValue = noData;
        writeHeaderFile();
        
        this.initialValue = initialValue;
        setFileAccess("rw");
        
        try {
            buffers = createNewDataFile(dataFile);
        } catch (IOException e) {
            System.out.println(e);
        }
    }
    
    private List<MappedByteBuffer> openDataFile(String fileAccess) throws IOException {
            
        List<MappedByteBuffer> buffers = new ArrayList<>();

        RandomAccessFile raf = new RandomAccessFile(this.dataFile, fileAccess);

        // Get MapMode based on the provided fileAccess string
        MapMode mapMode = (fileAccess.contains("w") ? MapMode.READ_WRITE : MapMode.READ_ONLY);

        long startPos = 0;
        long size;
        
        while (startPos < raf.length()) {
            size = Math.min(raf.length() - startPos, MAX_BUFFER_SIZE);
            MappedByteBuffer buf = raf.getChannel().map(mapMode, startPos, size);
            buf.order(byteOrder);
            buf.position(0);
            startPos = startPos + size;
            buffers.add(buf);
        }
        
        return buffers;

    }
    
    private List<MappedByteBuffer> createNewDataFile(String fileAccess) throws IOException {
        
        List<MappedByteBuffer> buffers = new ArrayList<>();

        RandomAccessFile raf = new RandomAccessFile(this.dataFile, fileAccess);

        // Get MapMode based on the provided fileAccess string
        MapMode mapMode = (fileAccess.contains("w") ? MapMode.READ_WRITE : MapMode.READ_ONLY);
        
        long fileSize = (numberColumns * numberRows) * cellSizeInBytes;
        long startPos = 0;
        long size;
        
        while (startPos < fileSize) {
            size = Math.min(fileSize - startPos, MAX_BUFFER_SIZE);
            MappedByteBuffer buf = raf.getChannel().map(mapMode, startPos, size);
            buf.order(byteOrder);
            buf.position(0);
            startPos = startPos + size;
            buffers.add(buf);
        }
        
        return buffers;
        
    }

    @Override
    public void close() {
        //buffer.force(); // Flush any changes from memory to disk
        //buffer = null; // There is no way to force a mapping to close
    }

    @Override
    public double getValue(int row, int column) {
        
        // Re-calculate row and column if out of bounds and file is reflected
        if (row < 0 || row > numberRows || column < 0 || column > numberColumns) {
            if (!isReflectedAtEdges) { return noDataValue; }

            // if you get to this point, it is reflected at the edges
            if (row < 0) { row = -row - 1; }
            if (row >= numberRows) { row = numberRows - (row - numberRows) - 1; }
            if (column < 0) { column = -column - 1; }
            if (column >= numberColumns) { column = numberColumns - (column - numberColumns) - 1; }
            // Check if the value is still out of bounds
            if (row < 0 || row > numberRows || column < 0 || column > numberColumns) {
                // it was too off grid to be reflected.
                return noDataValue;
            }
        }

        // Get the cell position in the file
        int cellPos = (row * numberColumns + column) * cellSizeInBytes;
        
        // Get the correct buffer
        int bufIndex = cellPos / MAX_BUFFER_SIZE;
        MappedByteBuffer buffer = buffers.get(bufIndex);
        
        int bufPos = cellPos - (bufIndex * MAX_BUFFER_SIZE);
        
        buffer.position(bufPos);
            
        try {
            
        } catch (IllegalArgumentException e) {
            // The cell number is outside the bounds of this buffer
            return noDataValue;
        }
        
        switch (getDataType()) {
            case BYTE:
                return buffer.get();
            case DOUBLE:
                return buffer.getDouble();
            case FLOAT:
                return buffer.getFloat();
            case INTEGER:
                return buffer.getInt();
        }
        
        return noDataValue;
    }

    @Override
    public void setValue(int row, int column, double value) {
        // Get the cell position in the file
        int cellPos = (row * numberColumns + column) * cellSizeInBytes;
        
        // Get the correct buffer
        int bufIndex = cellPos / MAX_BUFFER_SIZE;
        MappedByteBuffer buffer = buffers.get(bufIndex);
        
        int bufPos = cellPos - (bufIndex * MAX_BUFFER_SIZE);

        switch (getDataType()) {
            case BYTE:
                buffer.put(bufPos, (byte)value);
                break;
            case DOUBLE:
                buffer.putDouble(bufPos, value);
                break;
            case FLOAT:
                buffer.putFloat(bufPos, (float)value);
                break;
            case INTEGER:
                buffer.putInt(bufPos, (int)value);
                break;
        }
    }

    @Override
    public void setRowValues(int row, double[] vals) {
        
        // Get the first val position for the row
        int cellPos = (row * numberColumns) * cellSizeInBytes;
        
        // Get the correct buffer
        int bufIndex = cellPos / MAX_BUFFER_SIZE;
        MappedByteBuffer buffer = buffers.get(bufIndex);
        
        int bufPos = cellPos - (bufIndex * MAX_BUFFER_SIZE);

        switch (getDataType()) {
            case BYTE:
                for (double value : vals) {
                    buffer.put(bufPos, (byte)value);
                }
                break;
            case DOUBLE:
                for (double value : vals) {
                    buffer.putDouble(bufPos, value);
                }
                break;
            case FLOAT:
                for (double value : vals) {
                    buffer.putFloat(bufPos, (float)value);
                }
                
                break;
            case INTEGER:
                for (double value : vals) {
                    buffer.putInt(bufPos, (int)value);
                }
                break;
        }
    }
    
}
