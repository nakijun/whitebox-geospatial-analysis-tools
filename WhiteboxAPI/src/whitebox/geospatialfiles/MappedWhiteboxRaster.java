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

/**
 * Implementation of WhiteboxRasterInterface that uses a memory mapped file for
 * accessing the disk.
 * @author Kevin Green <kevin.a.green@gmail.com>
 */
public class MappedWhiteboxRaster extends WhiteboxRasterBase implements WhiteboxRasterInterface {   
    
    private MappedByteBuffer buffer;
    
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
        if (overwrite || !file.exists()) {
            buffer = createNewDataFile(fileAccess);
        } else {
            buffer = openDataFile(fileAccess);
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
        if (!file.exists()) {
            buffer = createNewDataFile(fileAccess);
        } else {
            buffer = openDataFile(fileAccess);
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
    }
    
    private MappedByteBuffer openDataFile(String fileAccess) {
        try {
            RandomAccessFile raf = new RandomAccessFile(this.dataFile, fileAccess);
            MappedByteBuffer buf = raf.getChannel().map(MapMode.READ_WRITE, 0, raf.length());
            buf.position(0);
            buf.order(byteOrder);
            return buf;
        } catch (IOException e) {
            System.out.println(e);
        }
        
        return null;
    }
    
    private MappedByteBuffer createNewDataFile(String fileAccess) {
        
        try {
            RandomAccessFile raf = new RandomAccessFile(this.dataFile, fileAccess);

            // Although the size can't be > max int, use a long to prevent overflow error
            long numCells = numberColumns * numberRows;
            
            MappedByteBuffer buf = raf.getChannel().map(MapMode.READ_WRITE, 0, numCells * cellSizeInBytes);
            buf.order(byteOrder);

            /*
            if (getDataType() == DataType.FLOAT) {
                byte[] defaultRow = new byte[numberColumns * cellSizeInBytes];
                
                //while (buf.position() < buf.limit()) {
                //    buf.put(defaultRow);
                //}
            }*/
            
            buf.position(0);
            
            return buf;
            
        } catch (IOException e) {
            System.out.println(e);
        }
        
        return null;
    }

    @Override
    public void close() {
        buffer.force(); // Flush any changes from memory to disk
        buffer = null; // There is no way to force a mapping to close
    }

    @Override
    public double getValue(int row, int column) {
        int cellNum = row * numberColumns + column;
        
        try {
            buffer.position(cellNum * cellSizeInBytes);
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
        int cellNum = row * numberColumns + column;

        switch (getDataType()) {
            case BYTE:
                buffer.put(cellNum * cellSizeInBytes, (byte)value);
                break;
            case DOUBLE:
                buffer.putDouble(cellNum * cellSizeInBytes, value);
                break;
            case FLOAT:
                buffer.putFloat(cellNum * cellSizeInBytes, (float)value);
                break;
            case INTEGER:
                buffer.putInt(cellNum * cellSizeInBytes, (int)value);
                break;
        }
    }

    @Override
    public void setRowValues(int row, double[] vals) {
        
        int firstCellNum = row * numberColumns;
        
        buffer.position(firstCellNum);
        
        //buffer.putDouble(vals);
    }
    
}
