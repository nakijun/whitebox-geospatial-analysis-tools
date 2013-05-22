package whitebox.geospatialfiles;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.MappedByteBuffer;
import java.nio.ShortBuffer;
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
    
    /**
     * Class constructor. Notice that the data file name will also be set based on the
     * specified header file name.
     * @param headerFile The name of the WhiteboxRaster header file.
     * @param fileAccess Sets the file access. Either "r" (read-only) or "rw" (read/write).
     */
    public MappedWhiteboxRaster(String headerFile, String fileAccess) {
        this.headerFile = headerFile;    
        this.dataFile = headerFile.replace(".dep", ".tas");
        this.statsFile = headerFile.replace(".dep", ".wstat");
        setFileAccess(fileAccess);
        readHeaderFile();
        
        File file = new File(dataFile);
        try {
            if (!file.exists()) {
                buffers = createNewDataFile(fileAccess);
            } else {
                buffers = openDataFile(fileAccess);
            }
        } catch (IOException e) {
            // Log and ignore?
            System.out.println(e);
        }
    }
    
    /**
     * Class constructor. Notice that the data file name will also be set based on the
     * specified header file name.
     * @param headerFile The name of the WhiteboxRaster header file.
     * @param fileAccess Sets the file access. Either "r" (read-only) or "rw" (read/write).
     * @param bufferSize This parameter is ignored but exists for interface compatibility
     */
    public MappedWhiteboxRaster(String headerFile, String fileAccess, double bufferSize) {
        this(headerFile, fileAccess);
    }
    
    /**
     * Class constructor. Notice that the data file name will also be set based on the
     * specified header file name.
     * @param headerFile The name of the WhiteboxRaster header file.
     * @param fileAccess Sets the file access. Either "r" (read-only) or "rw" (read/write).
     * @param baseRasterHeader The name of a WhiteboxRaster header file to base this new object on.
     * @param dataType The data type of the new WhiteboxRaster. Can be 'double', 'float', 'integer', or 'byte'
     * @param initialValue Double indicating the value used to initialize the grid. It is recommended to use the noDataValue.
     */
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
    
    /**
     * Class constructor. Notice that the data file name will also be set based on the
     * specified header file name.
     * @param headerFile The name of the WhiteboxRaster header file.
     * @param north The north coordinate of the raster file
     * @param south The south value of the raster file
     * @param east The east value of the raster file
     * @param west The west value of the raster file
     * @param rows The number of rows
     * @param cols The number of columns
     * @param dataScale The DataScale of the raster file
     * @param dataType The data type of the new WhiteboxRaster. Can be 'double', 'float', 'integer', or 'byte'
     * @param initialValue Double indicating the value used to initialize the grid. It is recommended to use the noDataValue.
     * @param noData Value used to indicate there is no value for a given point
     */
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
    
    /**
     * Creates a List of MappedByteBuffer objects to map an existing file.
     * @param fileAccess File access string for the file
     * @return A list of MappedByteBuffers
     * @throws IOException 
     */
    private List<MappedByteBuffer> openDataFile(String fileAccess) throws IOException {
            
        List<MappedByteBuffer> buffers = new ArrayList<>();
        
        try (RandomAccessFile raf = new RandomAccessFile(this.dataFile, fileAccess)) {
            
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
        }
        
        return buffers;

    }
    
    /**
     * Creates a List of MappedByteBuffer objects to map a new file. The file size is 
     * determined by the number of rows and columns and the data type.
     * @param fileAccess File access string for the new file
     * @return A list of MappedByteBuffers
     * @throws IOException 
     */
    private List<MappedByteBuffer> createNewDataFile(String fileAccess) throws IOException {
        
        List<MappedByteBuffer> buffers = new ArrayList<>();
        
        try (RandomAccessFile raf = new RandomAccessFile(this.dataFile, fileAccess)) {
            
            MapMode mapMode = (fileAccess.contains("w") ? MapMode.READ_WRITE : MapMode.READ_ONLY);
            
            long fileSize = ((long)numberColumns * numberRows) * cellSizeInBytes;
            long startPos = 0;
            int size;
            
            // Make sure a data cells won't be split between buffers
            this.bufferSize = MAX_BUFFER_SIZE - (MAX_BUFFER_SIZE % cellSizeInBytes);
            
            // Represent initialValue as byte array
            
            byte[] initialValueArray = new byte[this.bufferSize];
            ByteBuffer initialValueBuffer = ByteBuffer.wrap(initialValueArray);
            
            switch (getDataType()) {
                case BYTE:
                    byte bv = (byte)this.initialValue;
                    if (initialValueBuffer.hasArray()) {
                        byte[] vals = initialValueBuffer.array();
                        if (bv != 0) {
                            // This fill will populate bb's interal array
                            Arrays.fill(vals, 0, this.bufferSize, bv);
                        }
                    } else {
                        byte[] vals = new byte[this.bufferSize];
                        if (bv != 0) {
                            Arrays.fill(vals, 0, this.bufferSize, bv);
                        }
                        initialValueBuffer.put(vals);
                    }
                    break;
                case INTEGER:
                    short iv = (short)this.initialValue;
                    short[] intVals = new short[this.bufferSize / 2];
                    if (iv != 0) {
                        Arrays.fill(intVals, iv);
                    }
                    initialValueBuffer.asShortBuffer().put(intVals);
                    break;
                case DOUBLE:
                    double[] doubleVals = new double[this.bufferSize / 8];
            
                    if (this.initialValue != 0.0) {
                        Arrays.fill(doubleVals, this.initialValue);
                    }
                    initialValueBuffer.asDoubleBuffer().put(doubleVals);
                    break;
                case FLOAT:
                    float fv = (float)this.initialValue;
                    float[] floatVals = new float[this.bufferSize / 4];
                    
                    if (fv != 0.0F) {
                        Arrays.fill(floatVals, fv);
                    }
                    initialValueBuffer.asFloatBuffer().put(floatVals);
                    break;
                    
            }
            
            
            while (startPos < fileSize) {
                size = (int) Math.min(fileSize - startPos, this.bufferSize);
                MappedByteBuffer buf = raf.getChannel().map(mapMode, startPos, size);
                buf.order(byteOrder);
                buf.position(0);
                buf.limit(size);
                buf.put(initialValueArray, 0, size);
                buf.position(0);
                startPos = startPos + size;
                buffers.add(buf);
            }
        }
        
        return buffers;
        
    }

    @Override
    public void close() {
        if (this.isTemporaryFile) {
            File f1 = new File(this.headerFile);
            f1.delete();
            f1 = new File(this.dataFile);
            f1.delete();
        } else {
            if (saveChanges) {
                for (MappedByteBuffer mbb : buffers) {
                    mbb.force();
                }
                findMinAndMaxVals();
                writeHeaderFile();
            }
        }
    }

    /**
     * Retrieves the value contained at a specified cell in the raster grid.
     * @param row The zero-based row number.
     * @param column The zero-based column number.
     * @return The value contained in the raster grid at the specified grid cell.
     */
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

    /**
     * Sets the value of a specified cell in the raster grid.
     * @param row The zero-based row number.
     * @param column The zero-based column number.
     * @param value The value to place in the grid cell.
     */
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
    
     /**
     * This method should be used when you need to set an entire row of data
     * at a time. It has less overhead that the setValue method (which works
     * on a pixel-by-pixel basis) and can be used to efficiently scan through 
     * a raster image row by row.
     * @param row An int stating the zero-based row to be returned.
     * @param vals An array of doubles containing the values store in the specified row.
     */
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
                // Down convert to byte
                byte[] bvals = new byte[vals.length];
                for (int i = 0; i < vals.length; ++i) {
                    bvals[i] = (byte)vals[i];
                }
                buffer.position(bufPos);
                buffer.put(bvals);
                break;
            case DOUBLE:
                buffer.position(bufPos);
                buffer.asDoubleBuffer().put(vals);
                break;
            case FLOAT:
                float[] fvals = new float[vals.length];
                for (int i = 0; i < vals.length; ++i) {
                    fvals[i] = (float)vals[i];
                }
                buffer.position(bufPos);
                buffer.asFloatBuffer().put(fvals);
                break;
            case INTEGER:
                short[] svals = new short[vals.length];
                for (int i = 0; i < vals.length; ++i) {
                    svals[i] = (short)vals[i];
                }
                buffer.position(bufPos);
                buffer.asShortBuffer().put(svals);
                break;
        }
    }
    
    @Override
    public double[] getRowValues(int row) {
        if (row < 0 || row >= numberRows) { return null; }
        
        // Get the first val position for the row
        long cellPos = ((long)row * numberColumns) * cellSizeInBytes;
        
        // Get the correct buffer
        int bufIndex = (int)(cellPos / this.bufferSize);
        MappedByteBuffer buffer = buffers.get(bufIndex);
        
        // Position it at the start of this row
        buffer.position( (int)(cellPos % this.bufferSize) );

        double[] vals = new double[numberColumns];

        switch (getDataType()) {
            case BYTE:
                // convert bytes in image to double vals
                for (int i = 0; i < numberColumns; ++i) {
                    vals[i] = buffer.get();
                }
                break;
            case DOUBLE:
                // double to double, no conversion needed
                DoubleBuffer db = buffer.asDoubleBuffer();
                db.get( vals );
                break;
            case FLOAT:
                // convert floats in image to double vals
                FloatBuffer fb = buffer.asFloatBuffer();
                for (int i = 0; i < numberColumns; ++i) {
                    vals[i] = fb.get();
                }
                break;
            case INTEGER:
                // convert ints in image to double vals
                ShortBuffer sb = buffer.asShortBuffer();
                for (int i = 0; i < numberColumns; ++i) {
                    vals[i] = sb.get();
                }
                break;
        }
        
        return vals;
    }

    @Override
    public void flush() {
        for (MappedByteBuffer mbb : buffers) {
            mbb.force();
        }
    }
    
}
