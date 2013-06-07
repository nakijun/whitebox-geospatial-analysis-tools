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
 * 
 * NOTE: Some public methods from the original WhiteboxRaster class have not (yet)
 * been implemented because no plugins are currently calling them and/or their
 * functionality does not exist in this class:
 *  decrementValue()
 *  incrementValue()
 *  getBLockSize()
 *  getBufferSize()
 *  getNumberOfDataFileReads()
 *  getNumberOfDataFileWrites()
 * 
 * @author Kevin Green <kevin.a.green@gmail.com>
 */
public class MappedWhiteboxRaster extends WhiteboxRasterBase implements WhiteboxRasterInterface {   
    
    private List<MappedByteBuffer> buffers;
    
    /* How big to make each MappedByteBuffer, i.e., file segment? A segment gets
     * its own range of virtual addresses, and physically adjacent disk segments
     * will not necessarily be mapped adjacently in virtual memory.
     * 
     * Absolute max is max positive integer (Integer.MAX_VALUE = 2^31-1 ~ 2GB) since
     * methods such as Buffer.position(p) take an int.
     * 
     * Below that, the straightforward tradeoff is between many small segments vs.
     * few large segments.  The total number of virtual memory pages is the same
     * either way, and most plugins are eventually going to read or write the entire
     * file.  The constant below reflects a recommended segment size, and experiments
     * could be done about changing it.
     * 
     * MAX_BUFFER_SIZE needs to be rounded down to meet two constraints:
     * 1) Individual image cells must not break across segments (get/setValue())
     * 2) Entire rows must not break across segments in order to simplify
     *    get/setRowValues(), implying that MAX_BUFFER_SIZE > largest expected
     *    row size.
     * 1) is satisfied by 2). 
     */
    // 16MB = 2^24, allows for over 2 million columns of 8-byte (double) pixels
    private static final int MAX_BUFFER_SIZE = 16777216;
    
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
            
            // Rows shouldn't be split between two buffers
            this.bufferSize = MAX_BUFFER_SIZE - ( MAX_BUFFER_SIZE % (cellSizeInBytes*numberColumns) );
            
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
            
            // Rows shouldn't be split between two buffers
            this.bufferSize = MAX_BUFFER_SIZE - ( MAX_BUFFER_SIZE % (cellSizeInBytes*numberColumns) );
            
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
                flush();
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

        // Position file to start of cell in buffer
        MappedByteBuffer buffer = seekCell( row, column );
        if (buffer == null) {
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
        // Position file to start of cell in buffer
        MappedByteBuffer buffer = seekCell( row, column );
        if (buffer == null) {
            return;
        }

        switch (getDataType()) {
            case BYTE:
                buffer.put( (byte)value );
                break;
            case DOUBLE:
                buffer.putDouble( value );
                break;
            case FLOAT:
                buffer.putFloat( (float)value );
                break;
            case INTEGER:
                buffer.putShort( (short)value );
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
        if (!saveChanges) { return; }
        if (vals.length != numberColumns) { return; } 
        
        // update the minimum and maximum values
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (int i = 0; i < numberColumns; i++) {
            if (vals[i] < min && vals[i] != noDataValue) { min = vals[i]; }
            if (vals[i] > max && vals[i] != noDataValue) { max = vals[i]; }
        }
        if (max > maximumValue) { maximumValue = max; }
        if (min < minimumValue) { minimumValue = min; }
        
        // Position file to start of cell in buffer
        MappedByteBuffer buffer = seekCell( row, 0 );

        switch (getDataType()) {
            case BYTE:
                // Down convert double vals to bytes in image
                for (int i = 0; i < vals.length; ++i) {
                    buffer.put( (byte)vals[i] );
                }
                break;
            case DOUBLE:
                // Double to double, no conversion needed
                buffer.asDoubleBuffer().put( vals );
                break;
            case FLOAT:
                // Convert double vals to floats in image
                FloatBuffer fb = buffer.asFloatBuffer();
                for (int i = 0; i < vals.length; ++i) {
                    fb.put( (float)vals[i] );
                }
                break;
            case INTEGER:
                // Convert double vals to ints in image
                ShortBuffer sb = buffer.asShortBuffer();
                for (int i = 0; i < vals.length; ++i) {
                    sb.put( (short)vals[i] );
                }
                break;
        }
    }
    
     /**
     * This method should be used when you need to access an entire row of data
     * at a time. It has less overhead that the getValue method and can be used
     * to efficiently scan through a raster image row by row.
     * @param row An int stating the zero-based row to be returned.
     * @return An array of doubles containing the values store in the specified row.
     */
    @Override
    public double[] getRowValues(int row) {
        if (row < 0 || row >= numberRows) { return null; }
        
        double[] vals = new double[numberColumns];

        // Position file to start of cell in buffer
        MappedByteBuffer buffer = seekCell( row, 0 );
        
        /* There's a case where the .dep file has been created, but not the
         * corresponding .tas file, and yet the .dep file needs to know the min/max
         * values in the (non-existant) .tas file, so it's trying to get all the
         * rows in order to find min/max. This will come back from seekCell() as
         * null buffer (due to no .tas file yet). The original behaviour of this
         * method returns the vals array (zeroed by default), so we achieve that
         * by simply skipping the "get".
         */
        if ( buffer != null ) switch (getDataType()) {
            case BYTE:
                // convert bytes in image to double vals
                for (int i = 0; i < numberColumns; ++i) {
                    vals[i] = buffer.get();
                }
                break;
            case DOUBLE:
                // double to double, no conversion needed
                buffer.asDoubleBuffer().get( vals );
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

    /**
     * Synchronizes state of underlying disk file to current memory contents of
     * file's MappedByteBuffer(s), i.e., writes out any changes.
     */
    @Override
    public void flush() {
        for (MappedByteBuffer mbb : buffers) {
            mbb.force();
        }
    }
    
    /**
     * Find the MappedByteBuffer containing a given (row,col) cell, and position
     * to the start of that cell for subsequent get/put operations.
     * @param row The zero-based row number.
     * @param col The zero-based column number.
     * @return The MappedByteBuffer containing the given (row,col) cell, or null
     * if the file has no buffers.
     */
    private MappedByteBuffer seekCell( int row, int col ) {
        if ( buffers == null ) { return null; }
        
        // Calculate the cell position from start of image
        long cellPos = ((long)row * numberColumns + col) * cellSizeInBytes;
        
        // Get the correct buffer
        MappedByteBuffer buffer = buffers.get( (int)(cellPos / bufferSize) );
        
        // Position buffer at the start of the cell
        buffer.position( (int)(cellPos % bufferSize) );
        
        return buffer;        
    }

}
