package whitebox.geospatialfiles;

/**
 *
 * @author Kevin Green
 */
public interface WhiteboxRasterInterface {
    
     /**
     * Retrieves the value contained at a specified cell in the raster grid.
     * @param row The zero-based row number.
     * @param column The zero-based column number.
     * @return The value contained in the raster grid at the specified grid cell.
     */
     public double getValue(int row, int column);
     
     /**
     * Sets the value of a specified cell in the raster grid.
     * @param row The zero-based row number.
     * @param column The zero-based column number.
     * @param value The value to place in the grid cell.
     */
     public void setValue(int row, int column, double value);
     
    /**
     * Retrieves the value contained at a specified cell in the raster grid.
     * @param row The zero-based row number.
     * @param column The zero-based column number.
     * @return The value contained in the raster grid at the specified grid cell.
     */
     public void setRowValues(int row, double[] vals);
     
     /**
     * This method should be used when you need to access an entire row of data
     * at a time. It has less overhead that the getValue method and can be used
     * to efficiently scan through a raster image row by row.
     * @param row An int stating the zero-based row to be returned.
     * @return An array of doubles containing the values store in the specified row.
     */
     public double[] getRowValues(int row);
     
     /**
     * Used to perform closing functionality when a WhiteboxRaster is no longer needed.
     */
     public abstract void close();
}
