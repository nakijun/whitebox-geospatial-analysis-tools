/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plugins;

import Jama.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import whitebox.geospatialfiles.ShapeFile;
import whitebox.geospatialfiles.WhiteboxRasterBase;
import whitebox.geospatialfiles.shapefile.MultiPoint;
import whitebox.geospatialfiles.shapefile.MultiPointM;
import whitebox.geospatialfiles.shapefile.MultiPointZ;
import whitebox.geospatialfiles.shapefile.PointM;
import whitebox.geospatialfiles.shapefile.PointZ;
import whitebox.geospatialfiles.shapefile.ShapeFileRecord;
import whitebox.geospatialfiles.shapefile.ShapeType;
import static whitebox.geospatialfiles.shapefile.ShapeType.MULTIPOINT;
import static whitebox.geospatialfiles.shapefile.ShapeType.MULTIPOINTM;
import static whitebox.geospatialfiles.shapefile.ShapeType.MULTIPOINTZ;
import static whitebox.geospatialfiles.shapefile.ShapeType.POINT;
import static whitebox.geospatialfiles.shapefile.ShapeType.POINTM;
import static whitebox.geospatialfiles.shapefile.ShapeType.POINTZ;
import whitebox.geospatialfiles.shapefile.attributes.AttributeTable;
import whitebox.geospatialfiles.shapefile.attributes.DBFException;
import whitebox.geospatialfiles.shapefile.attributes.DBFField;



        
/**
 *
 * @author Ehsan.Roshani
 */
public class Kriging {
    public Matrix DistanceMatrix;   //The matrix that contains the distance of each known point to all other known points
    public int nKown;               //Number of known points
    //public double[][] Points;       //Array of points location x=0, y = 1, z = 2
    public class point
    {
        double x;
        double y;
        double z;
    }
    
    public SemiVariogramType SemiVariogramModel;
    public enum SemiVariogramType {
        Spherical, Exponential, Gaussian 
    }
    public double a;        //SemiVariogram a value
    public double h;        //SemiVariogram h value
    
    
    public double VariogramValue(SemiVariogramType semiType, double a, double h)
    {
        
        return 2.0;
    }
    
    //Reads the points coordinates in a shapefile
    public List<point> ReadPointFile(String inputFile, String fieldName)
    {
        int fieldNum = 0;
        WhiteboxRasterBase.DataType dataType = WhiteboxRasterBase.DataType.INTEGER;
        boolean useRecID = false;
        // initialize the shapefile input
        ShapeFile input = null;
        try {
            input = new ShapeFile(inputFile);
        } catch (IOException ex) {
            Logger.getLogger(Kriging.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (input.getShapeType() != ShapeType.POINT && 
                input.getShapeType() != ShapeType.POINTZ && 
                input.getShapeType() != ShapeType.POINTM && 
                input.getShapeType() != ShapeType.MULTIPOINT && 
                input.getShapeType() != ShapeType.MULTIPOINTZ && 
                input.getShapeType() != ShapeType.MULTIPOINTM) {
            //showFeedback("The input shapefile must be of a 'point' data type.");
            JOptionPane.showMessageDialog(null,"The input shapefile must be of a 'point' data type.");
            return null;
        }
        ///////////////
        // what type of data is contained in fieldName?
        AttributeTable reader = input.getAttributeTable(); //new DBFReader(input.getDatabaseFile());
        int numberOfFields = reader.getFieldCount();

        for (int i = 0; i < numberOfFields; i++) {
            DBFField field = reader.getField( i);

            if (field.getName().equals(fieldName)) {
                fieldNum = i;
                if (field.getDataType() == DBFField.DBFDataType.NUMERIC ||  
                        field.getDataType() == DBFField.DBFDataType.FLOAT) {
                    if (field.getDecimalCount() == 0) {
                        dataType = WhiteboxRasterBase.DataType.INTEGER;
                    } else {
                        dataType = WhiteboxRasterBase.DataType.FLOAT;
                    }
                } else {
                    useRecID = true;
                }
            }
        }
        if (fieldNum < 0) {
                useRecID = true;
        }
        //////////////////////
        Object[] data = null;
        double[][] geometry;
        List<point> Points = new ArrayList<point>();
        
        for (ShapeFileRecord record : input.records) {
            try {
                data = reader.nextRecord();
            } catch (DBFException ex) {
                Logger.getLogger(Kriging.class.getName()).log(Level.SEVERE, null, ex);
            }
            geometry = getXYFromShapefileRecord(record);
            for (int i = 0; i < geometry.length; i++) {
                point p = new point();
                p.x = geometry[i][0];
                p.y = geometry[i][1];
                p.z = Double.valueOf(data[fieldNum].toString());
                Points.add(p);
            }
        }
        return Points;
   }

    private double[][] getXYFromShapefileRecord(ShapeFileRecord record) {
        double[][] ret;
        ShapeType shapeType = record.getShapeType();
        switch (shapeType) {
            case POINT:
                whitebox.geospatialfiles.shapefile.Point recPoint = 
                        (whitebox.geospatialfiles.shapefile.Point)(record.getGeometry());
                ret = new double[1][2];
                ret[0][0] = recPoint.getX();
                ret[0][1] = recPoint.getY();
                break;
            case POINTZ:
                PointZ recPointZ = (PointZ)(record.getGeometry());
                ret = new double[1][2];
                ret[0][0] = recPointZ.getX();
                ret[0][1] = recPointZ.getY();
                break;
            case POINTM:
                PointM recPointM = (PointM)(record.getGeometry());
                ret = new double[1][2];
                ret[0][0] = recPointM.getX();
                ret[0][1] = recPointM.getY();
                break;
            case MULTIPOINT:
                MultiPoint recMultiPoint = (MultiPoint)(record.getGeometry());
                return recMultiPoint.getPoints();
            case MULTIPOINTZ:
                MultiPointZ recMultiPointZ = (MultiPointZ)(record.getGeometry());
                return recMultiPointZ.getPoints();
            case MULTIPOINTM:
                MultiPointM recMultiPointM = (MultiPointM)(record.getGeometry());
                return recMultiPointM.getPoints();
            default:
                ret = new double[1][2];
                ret[1][0] = -1;
                ret[1][1] = -1;
                break;
        }
        
        return ret;
    }
    
    
    public void test ()
    {
        
        double[][] d = {{1,2,3},{4,5,6,},{7,8,10}};
        Matrix tmp = Matrix.random(1000, 1000);
        //Matrix Dis = new Matrix(d);
        Matrix Point = Matrix.random(1000, 1);
        Matrix w = tmp.solve(Point);
        double[][] Wi =  w.getArray();
        int i = 0;
    }
    
//    public Matrix DistanceMatrix(double[][] Points)
//    {
//        double[][] dis = new double[nKown][nKown];
//        for (int i = 0; i < nKown; i++) {
//            for (int j = i+1; j < nKown; j++) {
//                
//            }
//        }
//        
//        return ;
//    }
    
    //Get the distance matrix and point 
    public double[][] CalculateWeights(double[] UnknowPoint)
    {
        for (int i = 0; i < nKown; i++) 
        {
            
        }
        
        return null;
        //Matrix Point = new Matrix(PointMatrix);
        //Matrix w = DistanceMatrix.solve(Point);
        //return w.getArray();
    }
    public static void main(String[] args) 
    {
        Kriging k = new Kriging();
        List<point> pnts =  k.ReadPointFile("G:\\Papers\\AGU 2013\\WakerLake\\WakerLake.shp","V");
        
        XYSeriesCollection result = new XYSeriesCollection();
        XYSeries series = new XYSeries("Random");
        int j = 0;
        for (Iterator<point> i = pnts.iterator(); i.hasNext(); )
        {
            series.add(pnts.get(j).x,pnts.get(j).y);
            i.next();
            j++;
        }
        result.addSeries(series);
        
        int t = 0;
        
        JFreeChart chart = ChartFactory.createScatterPlot(
            "Scatter Plot", // chart title
            "X", // x axis label
            "Y", // y axis label
            result, // data  ***-----PROBLEM------***
            PlotOrientation.VERTICAL,
            true, // include legend
            true, // tooltips
            false // urls
            );

        // create and display a frame...
        ChartFrame frame = new ChartFrame("First", chart);
        frame.pack();
        frame.setVisible(true);
    }
}
