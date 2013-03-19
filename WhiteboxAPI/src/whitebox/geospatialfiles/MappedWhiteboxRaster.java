package whitebox.geospatialfiles;

import java.io.File;
import java.io.FileNotFoundException;
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
    private FloatBuffer fb;
    
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

            int numCells = numberColumns * numberRows;
            
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
        
        buffer.position(cellNum * cellSizeInBytes);
        
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
            case DOUBLE:
                buffer.putDouble(cellNum * cellSizeInBytes, value);
            case FLOAT:
                buffer.putFloat(cellNum * cellSizeInBytes, (float)value);
            case INTEGER:
                buffer.putInt(cellNum * cellSizeInBytes, (int)value);
        }
    }

    @Override
    public void setRowValues(int row, double[] vals) {
        
        int firstCellNum = row * numberColumns;
        
        buffer.position(firstCellNum);
        
        //buffer.putDouble(vals);
    }
    
}
