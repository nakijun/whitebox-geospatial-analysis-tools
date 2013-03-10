/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package whitebox.geospatialfiles;

/**
 * WhiteboxRaster is a wrapper class for SequentialWhiteboxRaster. This should
 * be a temporary workaround while MappedWhiteboxRaster is implemented and becomes
 * the standard for all raster image use.
 * @author Kevin Green <kevin.a.green@gmail.com>
 */
public class WhiteboxRaster extends RandomAccessWhiteboxRaster {
    
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
         super(HeaderFile, FileAccess, BaseRasterHeader, dataType, InitialValue, BufferSize);
     }  
     
     public WhiteboxRaster(String HeaderFile, double north, double south, double east, double west, int rows, int cols, DataScale dataScale, DataType dataType, double initialValue, double noData) {
         super(HeaderFile, north, south, east, west, rows, cols, dataScale, dataType, initialValue, noData);
     }
}
