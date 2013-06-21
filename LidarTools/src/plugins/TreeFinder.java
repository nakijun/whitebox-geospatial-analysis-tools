/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plugins;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import whitebox.geospatialfiles.LASReader;
import whitebox.geospatialfiles.ShapeFile;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.geospatialfiles.shapefile.ShapeFileRecord;
import whitebox.geospatialfiles.shapefile.ShapeType;
import whitebox.geospatialfiles.shapefile.attributes.AttributeTable;
import whitebox.geospatialfiles.shapefile.attributes.DBFException;
import whitebox.geospatialfiles.shapefile.attributes.DBFField;
import whitebox.geospatialfiles.shapefile.attributes.DBFWriter;
import whitebox.structures.BoundingBox;
import whitebox.structures.KdTree;

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
    private double outputCellSize = 1;
    private WhiteboxRaster image;
    private int cols;
    private int rows;
    private double colSize;
    private double rowSize;
    double[][] value;
    private double numberOfTrees;
    private double searchRadius;
    
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
        return outputCellSize;
    }
    public double getDensitySearchRadius (){
        return densitySearchRadius;
    }
    public double getNumberOfTrees(){
        return numberOfTrees;
    }
    public double getSearchRadius()
    {
        return searchRadius;
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
        this.outputCellSize = OutputCellSize;
    }
    public void setSearchRadius(double SearchRadius){
        this.searchRadius = SearchRadius;
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
            this.numberOfTrees = numPoints;

        } catch (DBFException ex) {
            Logger.getLogger(TreeFinder.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(TreeFinder.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void Density()
    {
        String  outputHeader;
        String str1 = null;
        double north, south, east, west;
        int nrows, ncols;
        FileWriter fw = null;
        BufferedWriter bw = null;
        PrintWriter out = null;
        List<KdTree.Entry<Double>> results;
        double noData = -32768;
        double northing, easting;

        int a, i;
        
        try {
            ShapeType shapeType;
            ShapeFile input = new ShapeFile(this.outputShapefile);
            shapeType = input.getShapeType();
            KdTree<Double> pointsTree = new KdTree.SqrEuclid<Double>(2, new Integer((int)numberOfTrees));
            double minX, maxX, minY, maxY;
            int row, col;
            if (shapeType.getBaseType() == ShapeType.POINT
                    && shapeType.getBaseType() != ShapeType.MULTIPOINT) {
                AttributeTable reader = input.getAttributeTable();
                int numFields = reader.getFieldCount();

                DBFField[] fields = reader.getAllFields();
                double[] entry;
                double x,y,z;
                double maxDist = Double.POSITIVE_INFINITY;
                minX = Double.POSITIVE_INFINITY;
                maxX = Double.NEGATIVE_INFINITY;
                minY = Double.POSITIVE_INFINITY;
                maxY = Double.NEGATIVE_INFINITY;
                double resolution = this.outputCellSize;
                maxDist = (this.searchRadius * 2) * (this.searchRadius * 2); // actually squared
                
                for (ShapeFileRecord record : input.records) 
                {
                    if (record.getShapeType() != ShapeType.NULLSHAPE) 
                    {
                        double[][] point =  record.getGeometry().getPoints();
                        x = point[0][0];
                        y = point[0][1];
                        z = 0;
                        entry = new double[]{y,x};
                        pointsTree.addPoint(entry, z);
                        if (x < minX) {
                            minX = x;
                        }
                        if (x > maxX) {
                            maxX = x;
                        }
                        if (y < minY) {
                            minY = y;
                        }
                        if (y > maxY) {
                            maxY = y;
                        }
                    }
                }
                this.outputRaster = this.outputShapefile.replace(".shp", ".dep");
                outputHeader = this.outputRaster;
                
                // see if the output files already exist, and if so, delete them.
                if ((new File(outputHeader)).exists()) {
                    (new File(outputHeader)).delete();
                    (new File(outputHeader.replace(".dep", ".tas"))).delete();
                }
            
                // What are north, south, east, and west and how many rows and 
                // columns should there be?
                
                west = minX - 0.5 * resolution;
                north = maxY + 0.5 * resolution;
                nrows = (int)(Math.ceil((north - minY) / resolution));
                ncols = (int)(Math.ceil((maxX - west) / resolution));
                south = north - nrows * resolution;
                east = west + ncols * resolution;
            
                // create the whitebox header file.
                fw = new FileWriter(outputHeader, false);
                bw = new BufferedWriter(fw);
                out = new PrintWriter(bw, true);

                str1 = "Min:\t" + Double.toString(Integer.MAX_VALUE);
                out.println(str1);
                str1 = "Max:\t" + Double.toString(Integer.MIN_VALUE);
                out.println(str1);
                str1 = "North:\t" + Double.toString(north);
                out.println(str1);
                str1 = "South:\t" + Double.toString(south);
                out.println(str1);
                str1 = "East:\t" + Double.toString(east);
                out.println(str1);
                str1 = "West:\t" + Double.toString(west);
                out.println(str1);
                str1 = "Cols:\t" + Integer.toString(ncols);
                out.println(str1);
                str1 = "Rows:\t" + Integer.toString(nrows);
                out.println(str1);
                str1 = "Data Type:\t" + "float";
                out.println(str1);
                str1 = "Z Units:\t" + "not specified";
                out.println(str1);
                str1 = "XY Units:\t" + "not specified";
                out.println(str1);
                str1 = "Projection:\t" + "not specified";
                out.println(str1);
                str1 = "Data Scale:\tcontinuous"; 
                out.println(str1);
                str1 = "Preferred Palette:\t" + "spectrum.pal";
                out.println(str1);
                str1 = "NoData:\t" + noData;
                out.println(str1);
                if (java.nio.ByteOrder.nativeOrder() == java.nio.ByteOrder.LITTLE_ENDIAN) {
                    str1 = "Byte Order:\t" + "LITTLE_ENDIAN";
                } else {
                    str1 = "Byte Order:\t" + "BIG_ENDIAN";
                }
                out.println(str1);

                out.close();

                 // Create the whitebox raster object.
                WhiteboxRaster image = new WhiteboxRaster(outputHeader, "rw");
                int numPointsToUse = 10;
                int numPointsInArea = 0;
                boolean flag = false;
                int maxIteration = 20;
                int k = 0;
                double halfResolution = resolution / 2;
                double area = Math.PI * maxDist; // maxDist is already the squared radius
                for (row = 0; row < nrows; row++) {
                    for (col = 0; col < ncols; col++) {
                        easting = (col * resolution) + (west + halfResolution);
                        northing = (north - halfResolution) - (row * resolution);
                        entry = new double[]{northing, easting};
                        
                        // keep increasing the numPointsToUse, until you have a point
                        // that is at a greater distance than maxDist.
                        numPointsToUse = 10;
                        flag = false;
                        k = 0;
                        do {
                            k++;
                            results = pointsTree.nearestNeighbor(entry, numPointsToUse, true);
                            //results = pointsTree.neighborsWithinRange(entry,2500);
                            for (i = 0; i < results.size(); i++) {
                                if (results.get(i).distance > maxDist) {
                                    flag = true;
                                }
                            }
                            if (!flag) {
                                numPointsToUse = numPointsToUse * 2;
                            }
                        } while (!flag && k < maxIteration);
                        
                        // how many points are within the radius?
                        numPointsInArea = 0;
                        for (i = 0; i < results.size(); i++) {
                            if (results.get(i).distance <= maxDist) {
                                numPointsInArea++;
                            }
                        }
                        
                        image.setValue(row, col, numPointsInArea / area);
                        
                    }
//                    if (cancelOp) {
//                        cancelOperation();
//                        return;
//                    }
//                    progress = (int) (100f * row / (nrows - 1));
//                    updateProgress("Calculating point density:", progress);
                }

                image.addMetadataEntry("Created by the "
                        + " tool.");
                image.addMetadataEntry("Created on " + new Date());

                image.close();

                
                //int i = 0;
                
            }
            
            

        } catch (OutOfMemoryError oe) {
            //showFeedback("The Java Virtual Machine (JVM) is out of memory");
        } catch (Exception e) {
            //showFeedback(e.getMessage());
        } finally {
            //updateProgress("Progress: ", 0);
            // tells the main application that this process is completed.
            //amIActive = false;
            //myHost.pluginComplete();
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
