package whitebox.geospatialfiles;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implementation of WhiteboxRasterInterface that uses a memory mapped file for
 * accessing the disk.
 * @author Kevin Green <kevin.a.green@gmail.com>
 */
public class MappedWhiteboxRaster extends WhiteboxRasterBase implements WhiteboxRasterInterface {   
    
    private List<MappedByteBuffer> buffers;
    
    private static final int MAX_BUFFER_SIZE = 16777216; // 16MB = 2^24
    
    private int bufferSize;
    
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
        File f1 = new File(this.headerFile);
        f1.delete();
        f1 = new File(this.dataFile);
        f1.delete();
        f1 = new File(this.statsFile);
        f1.delete();
        this.initialValue = initialValue;
        setFileAccess(fileAccess);
        setPropertiesUsingAnotherRaster(baseRasterHeader, dataType);
 
        try {
            buffers = createNewDataFile(fileAccess);
        } catch (IOException e) {
            System.out.println(e);
        }        
        
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
        
        // Cells shouldn't be split between two files
        this.bufferSize = MAX_BUFFER_SIZE - (MAX_BUFFER_SIZE % cellSizeInBytes);
        
        while (startPos < raf.length()) {
            size = Math.min(raf.length() - startPos, this.bufferSize);
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
        
        long fileSize = ((long)numberColumns * numberRows) * cellSizeInBytes;
        long startPos = 0;
        int size;
        
        // Make sure a data cells won't be split between buffers
        this.bufferSize = MAX_BUFFER_SIZE - (MAX_BUFFER_SIZE % cellSizeInBytes);
        
        // Represent initialValue as byte array
        double[] initialValues = new double[this.bufferSize / 8];
        
        if (initialValue != 0.0) {
            Arrays.fill(initialValues, this.initialValue);
        }
        
        while (startPos < fileSize) {
            size = (int) Math.min(fileSize - startPos, this.bufferSize);
            MappedByteBuffer buf = raf.getChannel().map(mapMode, startPos, size);
            buf.order(byteOrder);
            buf.position(0);
            buf.limit(size);
            // sizeof(double) == 8
            buf.asDoubleBuffer().put(initialValues, 0, size / 8);
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
        if (row < 0 || row >= numberRows || column < 0 || column >= numberColumns) {
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
        long cellPos = ((long)row * numberColumns + column) * cellSizeInBytes;
        
        // Get the correct buffer
        int bufIndex = (int)(cellPos / this.bufferSize);
        if (buffers == null) {
            return noDataValue;
        }
        
        MappedByteBuffer buffer = buffers.get(bufIndex);

        int bufPos = (int)(cellPos - ((long)bufIndex * this.bufferSize));
        
        buffer.position(bufPos);

        switch (getDataType()) {
            case BYTE:
                return buffer.get();
            case DOUBLE:
                return buffer.getDouble();
            case FLOAT:
                return buffer.getFloat();
            case INTEGER:
                return buffer.getShort();
        }

        
        return noDataValue;
    }

    @Override
    public void setValue(int row, int column, double value) {
        // Get the cell position in the file
        long cellPos = ((long)row * numberColumns + column) * cellSizeInBytes;
        
        // Get the correct buffer
        int bufIndex = (int)(cellPos / this.bufferSize);
        if (buffers == null) {
            return;
        }
        
        MappedByteBuffer buffer = buffers.get(bufIndex);

        int bufPos = (int)(cellPos - ((long)bufIndex * this.bufferSize));

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
                buffer.putShort(bufPos, (short)value);
                break;
        }

    }

    @Override
    public void setRowValues(int row, double[] vals) {
        
        // Get the first val position for the row
        long cellPos = ((long)row * numberColumns) * cellSizeInBytes;
        
        // Get the correct buffer
        int bufIndex = (int)(cellPos / this.bufferSize);
        MappedByteBuffer buffer = buffers.get(bufIndex);
        
        int bufPos = (int)(cellPos - ((long)bufIndex * this.bufferSize));

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

    @Override
    public void flush() {
        for (MappedByteBuffer mbb : buffers) {
            mbb.force();
        }
    }
    
}
