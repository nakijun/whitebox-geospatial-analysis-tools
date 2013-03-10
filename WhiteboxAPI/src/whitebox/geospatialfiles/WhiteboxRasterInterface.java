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
     * Used to perform closing functionality when a WhiteboxRaster is no longer needed.
     */
     public abstract void close();
}
