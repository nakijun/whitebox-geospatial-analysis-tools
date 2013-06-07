/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package whitebox.geospatialfiles;

/**
 * WhiteboxRaster is a wrapper class for whichever implementation is being used.
 * The newly-developed MappedWhiteboxRaster is selected below, but this could
 * be changed to RandomAccessWhiteboxRaster to get the old implementation.
 * There is no need to change any plugin code, because they are all doing
 * "new WhiteboxRaster"; they get whichever implementation is selected here.
 * 
 * @author Kevin Green <kevin.a.green@gmail.com>
 */
public class WhiteboxRaster extends MappedWhiteboxRaster {
    
    public WhiteboxRaster(String HeaderFile, String FileAccess) {
        super(HeaderFile, FileAccess);
    }
    
    public WhiteboxRaster(String HeaderFile, String FileAccess, double BufferSize) {
        super(HeaderFile, FileAccess, BufferSize);
    }
    
     public WhiteboxRaster(String HeaderFile, String FileAccess, String BaseRasterHeader, DataType dataType, double InitialValue) {
         super(HeaderFile, FileAccess, BaseRasterHeader, dataType, InitialValue);
     }
        
     public WhiteboxRaster(String HeaderFile, String FileAccess, String BaseRasterHeader, DataType dataType, double InitialValue, double BufferSize) {
         super(HeaderFile, FileAccess, BaseRasterHeader, dataType, InitialValue);
     }  
     
     public WhiteboxRaster(String HeaderFile, double north, double south, double east, double west, int rows, int cols, DataScale dataScale, DataType dataType, double initialValue, double noData) {
         super(HeaderFile, north, south, east, west, rows, cols, dataScale, dataType, initialValue, noData);
     }
}
