/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plugins;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import whitebox.geospatialfiles.ShapeFile;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.geospatialfiles.shapefile.ShapeType;
import whitebox.geospatialfiles.shapefile.attributes.DBFException;
import whitebox.geospatialfiles.shapefile.attributes.DBFField;
import whitebox.geospatialfiles.shapefile.attributes.DBFWriter;
import whitebox.structures.BoundingBox;

/**
 *
 * @author Ehsan Roshani
 */
public class TreeFinder {
    private String inputRaster;
    private String outputShapefile;
    private String outputRaster;        //The density raster file
    private double a = 2.51503;
    private double b = 0;
    private double c = 0.00901;
    private double threshold = 0;
    private double maxHeight = 50;
    private double densitySearchRadius = 100;
    private double outpurCellSize = 1;
    private WhiteboxRaster image;
    private int cols;
    private int rows;
    private double colSize;
    private double rowSize;
    double[][] value;
    
    //bonding cells for the local search area
    private class BBox{
        int minR;
        int maxR;
        int minC;
        int maxC;
        int cenR;
        int cenC;
        double Diameter;
    }
    
    public class localMax{
        public boolean IsMax;
        public double Diameter;
        public double Height;
    }
    
    public String getInputRaster(){
        return inputRaster;
    }
    public String getShapefile(){
        return outputShapefile;
    }
    
    public String getOutputfile(){
        return outputRaster;
    }
    public double getA (){
        return a;
    }
    public double getB (){
        return b;
    }
    public double getC (){
        return c;
    }
    public double getThreshold (){
        return threshold;
    }
    public double getMaxHeight (){
        return maxHeight;
    }
    public double getOutputCellSize (){
        return outpurCellSize;
    }
    public double getDensitySearchRadius (){
        return densitySearchRadius;
    }
    
    public void setA(double a){
        this.a=a;
    }
    public void setB(double b){
        this.b=b;
    }
    public void setC(double c){
        this.c=c;
    }
    public void setThreshold(double Threshold){
        this.threshold=Threshold;
    }
    public void setMaxHeight(double MaxHeight){
        this.maxHeight=MaxHeight;
    }
    public void setDensitySearchRadius(double SearchRadius){
        this.densitySearchRadius = SearchRadius;
    }
    public void setOutputCellSize(double OutputCellSize){
        this.outpurCellSize = OutputCellSize;
    }
    
    //Constractor for the object
    public TreeFinder (String inputRaster, String outputShapefile){
        this.inputRaster = inputRaster;
        this.outputShapefile = outputShapefile;
        
        image = new WhiteboxRaster(inputRaster, "r");
        
        rows = image.getNumberRows();
        cols = image.getNumberColumns();
        rowSize = image.getCellSizeY();
        colSize = image.getCellSizeX();
        value = new double[rows][cols];
        for (int r = 0; r < rows; r++) {
            value[r]=image.getRowValues(r);
        }
        //Search();
    }
    
    //Object Constructor with coef. for the imperical equation and other input values
    public TreeFinder (String inputRaster, String outputShapefile, double a, double b, double c
            , double threshold, double maxHeight){
        this.inputRaster = inputRaster;
        this.outputShapefile = outputShapefile;
        this.a = a;
        this.b = b;
        this.c = c;
        this.threshold = threshold;
        this.maxHeight = maxHeight;
        image = new WhiteboxRaster(inputRaster, "r");
        
        rows = image.getNumberRows();
        cols = image.getNumberColumns();
        rowSize = image.getCellSizeY();
        colSize = image.getCellSizeX();
        value = new double[rows][cols];
        for (int r = 0; r < rows; r++) {
            value[r]=image.getRowValues(r);
        }
        //Search();
    }
    
    //The main method that searches all of the cells and builds the shape file which consists of
    //point files for each tree location, Tree height, and crown diameter
    public void Search(){
        try {
            ShapeFile output = new ShapeFile(outputShapefile, ShapeType.POINT);
            DBFField fields[] = new DBFField[3];

            fields[0] = new DBFField();
            fields[0].setName("FID");
            fields[0].setDataType(DBFField.DBFDataType.NUMERIC);
            fields[0].setFieldLength(10);
            fields[0].setDecimalCount(0);

            fields[1] = new DBFField();
            fields[1].setName("Diameter");
            fields[1].setDataType(DBFField.DBFDataType.NUMERIC);
            fields[1].setFieldLength(10);
            fields[1].setDecimalCount(3);

            fields[2] = new DBFField();
            fields[2].setName("Height");
            fields[2].setDataType(DBFField.DBFDataType.NUMERIC);
            fields[2].setFieldLength(8);
            fields[2].setDecimalCount(0);

            String DBFName = output.getDatabaseFile();
            DBFWriter writer = new DBFWriter(new File(DBFName)); 
            writer.setFields(fields);

            
            
            localMax lm = new localMax();
            int numPoints = 0;
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    lm =LocalMax(r, c); 
                    if (lm.IsMax) {
                        whitebox.geospatialfiles.shapefile.Point wbGeometry = new whitebox.geospatialfiles.shapefile.Point(image.getXCoordinateFromColumn(c), image.getYCoordinateFromRow(r));
                        output.addRecord(wbGeometry);

                        Object[] rowData = new Object[3];
                        rowData[0] = new Double(numPoints);
                        rowData[1] = new Double(lm.Diameter);
                        rowData[2] = new Double(lm.Height);
                        writer.addRecord(rowData);
                        numPoints++;
                    }
                }
            }
                output.write();
                writer.write();

        } catch (DBFException ex) {
            Logger.getLogger(TreeFinder.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(TreeFinder.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    //Check to see if the passed cell is a local maximum or not and if so it returns the tree diameter and height
    private localMax LocalMax (int row, int col){
        
        BBox bb = BoundingBox(row, col);
        localMax res = new localMax();
        if (bb == null) {
            res.IsMax = false;
            res.Diameter = 0;
            res.Height = 0;
            return res;
        }
        double dis = 0;
        for (int r = bb.minR; r <= bb.maxR; r++) {
            for (int c = bb.minC; c <= bb.maxC; c++) {
                dis = Math.sqrt(Math.pow((r-row)/rowSize,2)+Math.pow((c-col)/colSize, 2));
                if (bb.Diameter>= dis) {
                    if (value[row][col]<value[r][c]) {
                        res.IsMax = false;
                        res.Diameter = 0;
                        res.Height = 0;
                        return res;
                    }
                }
            }
        }
        res.IsMax = true;
        res.Diameter = bb.Diameter;
        res.Height = value[row][col];
        return res;
    }
    
    
    //calculate the search space diameter based on the height of the cell
    private BBox BoundingBox(int row, int col){
        
        double d = diameter(row, col);
        if (d >0) {
            BBox res = new BBox();
            int dr = (int)(d/rowSize)+1;
            int dc = (int)(d/colSize)+1;

            res.minR= (row-dr>0)?(row-dr):(0);
            res.maxR= (row+dr>rows)?(rows-1):(row+dr);
            res.minC= (col-dc>0)?(col-dc):(0);
            res.maxC= (col+dc>cols)?(cols-1):(col+dc);

            //These are the new row and col in the local area block
            res.cenR = row - res.minR;
            res.cenC = col - res.minC;

            res.Diameter = d;
            return res;
        }
        return null;
    }
    
    //calculate the tree crown diameter of the tree based on the imperical equation.
    private double diameter(int row, int col){
        double h = value[row][col];
        if (h < maxHeight && h >= threshold) {
            //return a + b * h + c * h * h;
            return (a + b * h + c * h * h)/2;
        }
        else{
            return 0;
        }
        
    }
    
    public static void main(String[] args) {
        TreeFinder tf = new TreeFinder("C:\\PDF\\John Example\\Data\\Data\\out.dep",
                "C:\\PDF\\John Example\\Data\\Data\\outT.shp");
    }
}
