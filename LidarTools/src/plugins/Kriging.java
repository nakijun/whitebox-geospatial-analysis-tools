/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plugins;

import Jama.*;
//import com.sun.org.apache.xml.internal.resolver.helpers.Debug;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import net.finmath.optimizer.LevenbergMarquardt;
import net.finmath.optimizer.SolverException;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.renderer.xy.*;
import org.jfree.chart.plot.*;
import org.jfree.data.xy.XYBarDataset;
import org.jfree.data.xy.XYDataset;
import whitebox.geospatialfiles.ShapeFile;
import whitebox.geospatialfiles.WhiteboxRaster;
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
import whitebox.structures.KdTree;
import whitebox.structures.KdTree.Entry;


        
/**
 *
 * @author Ehsan.Roshani
 */
public class Kriging {
    
    
    
    public double resolution;
    //public double 

    public double BMinX;    //Bounding box Minimum X
    public double BMinY;
    public double BMaxX;
    public double BMaxY;
    public void SetBoundary(double MinimumX,double MaximumX,double MinimumY,double MaximumY){
        BMinX = MinimumX;
        BMinY = MinimumY;
        BMaxX = MaximumX;
        BMaxY = MaximumY;
    }
    public double MinX;     //Minimum X Coordinat in the Points 
    public double MinY;     //Minimum Y Coordinat in the Points
    public double MaxX;     //Maximum X Coordinat in the Points
    public double MaxY;     //Maximum Y Coordinat in the Points
    public int NumberOfLags;
    public double LagSize;
    //public double 
    private KdTree<Double> pointsTree;      //This is the point tree which will be filled in the calcPair method
    public Matrix DistanceMatrix;   //The matrix that contains the distance of each known point to all other known points
    public int nKown;               //Number of known points
    //public double[][] Points;       //Array of points location x=0, y = 1, z = 2
    public double MaximumDistance;
    public bin[][] BinSurface;       //n*3 matrix to store all the binnes
    
    public class point
    {
        double x;
        double y;
        double z;
    }
    public point point(double x , double y, double z){
        point p = new point();
        p.x = x;
        p.y = y;
        p.z = z;
        return p;
    }
    public List<point> Points = new ArrayList();
    
    public class pair
    {
        int FirstP;
        int SecondP;
        double Distance;
        double Direction;
        double MomentI;             //Moment of Inertia
        double VerDistance;         //Vertical Distance (Y Axes)
        double HorDistance;         //Horizontal Distance (X Axes)
    }
    
    //List<bin> Binnes = new ArrayList();
    bin[][] Binnes; // = new bin[]
    public class bin
    {
        double GridHorDistance;
        double GridVerDistance;
        double HorDistance;
        double VerDistance;
        double Distance;
        double Value;
        int Size;
    }
    public List<pair> Pairs = new ArrayList();
    
    public SemiVariogramType SemiVariogramModel;
    public enum SemiVariogramType {
        Spherical, Exponential, Gaussian 
    }
    public double Range;        //SemiVariogram a value
    public double Sill;        //SemiVariogram h value
    public double Nugget;       //Semivariogram Nugget value
    public boolean ConsiderNugget;  //If nugget should be considered or not
    public SemiVariogramType SVType;
    
    
    
    public class Variogram{
        public double Range;
        public double Sill;
        public double Nugget;
        public SemiVariogramType Type;
    }
    
    
    
    
    /**
     * It calculates the average values for each bin.
     * 
     * @param Range 
     */
    void CalcBinnesOld(double Range)
    {
        double[][] b = new double [NumberOfLags+1][3]; //0 = Distance, 1 = Value, 2 = Size
//        if (LagSize == 0) {
//            LagSize = MaximumDistance/(NumberOfLags*2);
//        }
        int resd = 0;
        for (int i = 0; i < Pairs.size(); i++) {
            if (Pairs.get(i).Distance<= Range) {
                resd =(int)((Pairs.get(i).Distance - (Pairs.get(i).Distance%LagSize))/LagSize);
                //System.out.println(LagSize +" : "+ resd + " : " + Pairs.get(i).Distance);
                b[resd][0]+=Pairs.get(i).Distance;
                b[resd][1]+=Pairs.get(i).MomentI;
                b[resd][2]+=1;
            }
        }
        for (int i = 0; i < NumberOfLags; i++) {
            bin bi = new bin();
            bi.Distance = b[i][0]/b[i][2];
            bi.Value = b[i][1]/b[i][2];
            bi.Size = (int)b[i][2];
            if (bi.Size>0) {
                //Binnes.add(bi);
            }
        }
    }
    
    
     void CalcBinnes(double Range)
    {
        //Math.floor(MaxX);
        
        //Binnes = new bin[this.NumberOfLags][this.NumberOfLags];
        //Binnes Category on the axies 
        //2 . 1
        //3   4         //Only 1 and 3 are calculated the rest are mirror
        int ad = 0;
        if (Range%this.LagSize == 0) {
            ad = 1;
        }
        bin[][] Binnes1 = new bin[(int)Math.ceil(2*Range/this.LagSize)+ad]
                [(int)Math.ceil(Range/this.LagSize+ad)];
        bin[][] Binnes4 = new bin[(int)Math.ceil(2*Range/this.LagSize)+ad]
                [(int)Math.ceil(Range/this.LagSize+ad)];

        
        bin[][] Binnes1c = new bin[(int)Math.ceil(2*Range/this.LagSize)+ad]
                [(int)Math.ceil(Range/this.LagSize+ad)];
        bin[][] Binnes4c = new bin[(int)Math.ceil(2*Range/this.LagSize)+ad]
                [(int)Math.ceil(Range/this.LagSize+ad)];

        
        BinSurface = new bin[2 * ((int)Math.ceil(2*Range/this.LagSize)+ad)][2*((int)Math.ceil(Range/this.LagSize+ad))];


        int c = 0;      //Columns
        int r = 0;      //Rows
        for (int i = 0; i < Pairs.size(); i++) {
            if (Pairs.get(i).VerDistance>=0 && Pairs.get(i).HorDistance>=0 
                    & Pairs.get(i).VerDistance<= 2* Range & Pairs.get(i).HorDistance<= Range) {
                
                r = Math.abs((int)Math.floor(Pairs.get(i).VerDistance/this.LagSize));
                c = Math.abs((int)Math.floor(Pairs.get(i).HorDistance/this.LagSize));
                if (Binnes1[r][c] == null) {
                    bin bb = new bin();
                    bin bbc = new bin();

                    Binnes1[r][c] = bb;
                    Binnes1c[r][c] = bbc;
                }
                Binnes1[r][c].HorDistance += Pairs.get(i).HorDistance;
                Binnes1[r][c].VerDistance+= Pairs.get(i).VerDistance;
                Binnes1[r][c].Value+= Pairs.get(i).MomentI;
                Binnes1[r][c].Size+= 1;
            
                Binnes1c[r][c].HorDistance += -Pairs.get(i).HorDistance;
                Binnes1c[r][c].VerDistance+= -Pairs.get(i).VerDistance;
                Binnes1c[r][c].Value+= Pairs.get(i).MomentI;
                Binnes1c[r][c].Size+= 1;

            
            
            }
        }
        
        for (int i = 0; i < Binnes1.length; i++) {
            for (int j = 0; j < Binnes1[i].length; j++) {
                if (Binnes1[i][j] == null) {
                    bin bb = new bin();
                    Binnes1[i][j] = bb;
                    Binnes1[i][j].HorDistance = i*this.LagSize;
                    Binnes1[i][j].VerDistance=j*this.LagSize;
                    Binnes1[i][j].Value = -1;
                
                    bin bbc = new bin();
                    Binnes1c[i][j] = bbc;
                    Binnes1c[i][j].HorDistance = -i*this.LagSize;
                    Binnes1c[i][j].VerDistance=-j*this.LagSize;
                    Binnes1c[i][j].Value = -1;
                }
                else{
                    Binnes1[i][j].HorDistance = Binnes1[i][j].HorDistance/Binnes1[i][j].Size;
                    Binnes1[i][j].VerDistance=Binnes1[i][j].VerDistance/Binnes1[i][j].Size;
                    Binnes1[i][j].Value = Binnes1[i][j].Value/Binnes1[i][j].Size;
                
                    Binnes1c[i][j].HorDistance = Binnes1c[i][j].HorDistance/Binnes1c[i][j].Size;
                    Binnes1c[i][j].VerDistance=Binnes1c[i][j].VerDistance/Binnes1c[i][j].Size;
                    Binnes1c[i][j].Value = Binnes1c[i][j].Value/Binnes1c[i][j].Size;
                }
                Binnes1[i][j].GridHorDistance=0.5*this.LagSize + j*this.LagSize;
                Binnes1[i][j].GridVerDistance=0.5*this.LagSize+i*this.LagSize;

                Binnes1c[i][j].GridHorDistance=-0.5*this.LagSize - j*this.LagSize;
                Binnes1c[i][j].GridVerDistance=-0.5*this.LagSize-i*this.LagSize;

                //dddd= Pairs.get(i).HorDistance;
                
                //System.out.println( (0.5*this.LagSize + j*this.LagSize) + " , " + (0.5*this.LagSize+i*this.LagSize) + " , " +
                //        Binnes1[i][j].HorDistance + " , " + Binnes1[i][j].VerDistance + " , " + Binnes1[i][j].Value);
            }
        }
        //==========================
        
        for (int i = 0; i < Pairs.size(); i++) {
            if (Pairs.get(i).VerDistance<0 && Pairs.get(i).HorDistance>=0
                    & Pairs.get(i).VerDistance >= -2*Range & Pairs.get(i).HorDistance<= Range) {
                
                r = Math.abs((int)Math.ceil(Pairs.get(i).VerDistance/this.LagSize));
                c = Math.abs((int)Math.floor(Pairs.get(i).HorDistance/this.LagSize));
                if (Binnes4[r][c] == null) {
                    bin bb = new bin();
                    Binnes4[r][c] = bb;
                    
                    bin bbc = new bin();
                    Binnes4c[r][c] = bbc;

                }
                
                Binnes4[r][c].HorDistance += Pairs.get(i).HorDistance;
                Binnes4[r][c].VerDistance+= Pairs.get(i).VerDistance;
                Binnes4[r][c].Value+= Pairs.get(i).MomentI;
                Binnes4[r][c].Size+= 1;
            
                Binnes4c[r][c].HorDistance += -Pairs.get(i).HorDistance;
                Binnes4c[r][c].VerDistance+= -Pairs.get(i).VerDistance;
                Binnes4c[r][c].Value+= Pairs.get(i).MomentI;
                Binnes4c[r][c].Size+= 1;

            
            
            }
        }
        
        for (int i = 0; i < Binnes4.length; i++) {
            for (int j = 0; j < Binnes4[i].length; j++) {
                if (Binnes4[i][j] == null) {
                    bin bb = new bin();
                    Binnes4[i][j] = bb;
                    Binnes4[i][j].HorDistance = i*this.LagSize;
                    Binnes4[i][j].VerDistance = -j*this.LagSize;
                    Binnes4[i][j].Value = -1;
                
                    bin bbc = new bin();
                    Binnes4c[i][j] = bbc;
                    Binnes4c[i][j].HorDistance = -i*this.LagSize;
                    Binnes4c[i][j].VerDistance = j*this.LagSize;
                    Binnes4c[i][j].Value = -1;

                
                
                }
                else{
                    Binnes4[i][j].HorDistance = Binnes4[i][j].HorDistance/Binnes4[i][j].Size;
                    Binnes4[i][j].VerDistance=Binnes4[i][j].VerDistance/Binnes4[i][j].Size;
                    Binnes4[i][j].Value = Binnes4[i][j].Value/Binnes4[i][j].Size;
                
                    Binnes4c[i][j].HorDistance = Binnes4c[i][j].HorDistance/Binnes4c[i][j].Size;
                    Binnes4c[i][j].VerDistance=Binnes4c[i][j].VerDistance/Binnes4c[i][j].Size;
                    Binnes4c[i][j].Value = Binnes4c[i][j].Value/Binnes4c[i][j].Size;

                }
                Binnes4[i][j].GridHorDistance=(0.5*this.LagSize+j*this.LagSize);
                Binnes4[i][j].GridVerDistance=(-0.5*this.LagSize+-i*this.LagSize);
                
                Binnes4c[i][j].GridHorDistance=-(0.5*this.LagSize+j*this.LagSize);
                Binnes4c[i][j].GridVerDistance=-(-0.5*this.LagSize+-i*this.LagSize);

                
                //System.out.println( (0.5*this.LagSize+j*this.LagSize) + " , " + (-0.5*this.LagSize+-i*this.LagSize) + " , " +
                //        Binnes4[i][j].HorDistance + " , " + Binnes4[i][j].VerDistance + " , " + Binnes4[i][j].Value);
                //System.out.println(i + " , " + j);
            }
        }
        int stI = BinSurface.length/2;
        int stJ = BinSurface[0].length/2;
        
        
        
        //Binnes1c = Binnes1.clone();
        for (int i = 0; i < Binnes1.length; i++) {
            for (int j = 0; j < Binnes1[i].length; j++) {
                BinSurface[stI+i][stJ+j] = Binnes1[i][j];
                BinSurface[stI-1-i][stJ-1-j] = Binnes1c[i][j];
            }
        }
        
        stI = BinSurface.length/2;
        stJ = BinSurface[0].length/2;
        for (int i = 0; i < Binnes4.length; i++) {
            for (int j = 0; j < Binnes4[i].length; j++) {
                BinSurface[stI-1-i][stJ+j] = Binnes4[i][j];
                BinSurface[stI+i][stJ-1-j] = Binnes4c[i][j];
            }
        }
        
        
        for (int i = 0; i < BinSurface.length; i++) {
            for (int j = 0; j < BinSurface[i].length; j++) {
                System.out.println(BinSurface[i][j].GridHorDistance + " , " + BinSurface[i][j].GridVerDistance
                        + " , " + BinSurface[i][j].HorDistance+ " , " + BinSurface[i][j].VerDistance
                        + " , " + BinSurface[i][j].Value);
            }
        }
        
        
        
        
        
        
        
        int resd = 0;
    }
//    
//    Variogram TheoryVariogram(SemiVariogramType semiType)
//    {
//        SVType = semiType;
//        LevenbergMarquardt optimizer = new LevenbergMarquardt() {
//                // Override your objective function here
//                
//            public void setValues(double[] parameters, double[] values) {
//                //parameters[0] = sill, parameters[1] Range, parameters[2] nugget    
//                double [] x = new double[Binnes.size()];
//                    for (int i = 0; i < Binnes.size(); i++) {
//                        x[i]=Binnes.get(i).Distance;
//                    }
//                    switch (SVType){
//                        case Exponential:
//                            for (int i = 0; i < x.length; i++) {
//                                if (x[i]!=0) {
//                                    values[i]= (ConsiderNugget ? parameters[2] : 0 ) + parameters[0]*(1-Math.exp(-x[i]/parameters[1]));
//                                }
//                                else{
//                                    values[i]= 0;
//                                }
//                            }
//                            break;
//                        case Gaussian:
//                            for (int i = 0; i < x.length; i++) {
//                                if (x[i]!=0) {
//                                    values[i]=(ConsiderNugget ? parameters[2] : 0 ) + parameters[0]*(1-Math.exp(-(Math.pow(x[i], 2))/(Math.pow( parameters[1],2))));
//                                }
//                                else{
//                                    values[i]=0;
//                                }
//                            }
//                            break;
//                        case Spherical:
//                            for (int i = 0; i < x.length; i++) {
//                                if (x[0]>parameters[1]) {
//                                    values[i]= (ConsiderNugget ? parameters[2] : 0 ) + parameters[0];
//                                    
//                                }
//                                else if (0<x[0] && x[0] <=parameters[1]) {
//                                    values[i]= (ConsiderNugget ? parameters[2] : 0 ) + parameters[0]*(1.5*x[i]/parameters[1]-0.5*Math.pow((x[i]/parameters[1]),3));
//                                }
//                                else
//                                {
//                                    values[i]= 0;
//                                }
//                            }
//                            break;
//                    }
//                }
//        };
// 
//
//        // Set solver parameters
//        
//        double [] y = new double[Binnes.size()];
//        for (int i = 0; i < y.length; i++) {
//            y[i]=Binnes.get(i).Value;
//            //System.out.println(Binnes.get(i).Distance);
//        }
//        double [] iniPar = new double[y.length];
//        double [] w = new double[y.length];
//        for (int i = 0; i < y.length; i++) {
//            iniPar[i]=1;
//            w[i]=1;
//        }
//        double tmp = 0;
//        for (int i = 0; i < y.length; i++) {
//            if ( !Double.isNaN(y[i])) {
//                tmp += y[i];
//            }
//        }
//        iniPar[1]=this.LagSize;
//        iniPar[0]=tmp/y.length;
//        optimizer.setInitialParameters(iniPar);
//        optimizer.setWeights(w);
//        optimizer.setMaxIteration(100);
//        optimizer.setTargetValues(y);
//        try {
//            optimizer.run();
//        } catch (SolverException ex) {
//            Logger.getLogger(Kriging.class.getName()).log(Level.SEVERE, null, ex);
//        }
// 
//        double[] bestParameters = optimizer.getBestFitParameters();
////        this.Sill = bestParameters[0];
////        this.Range=bestParameters[1];
////        this.Nugget = (ConsiderNugget ? bestParameters[2] : 0 );
//        
//        Variogram var = new Variogram();
//        var.Sill = bestParameters[0];
//        var.Range=bestParameters[1];
//        var.Nugget =  (ConsiderNugget ? bestParameters[2] : 0 );
//        var.Type = semiType;
//        return var;
//    }
//    
    
    public double getTheoreticalSVValue(double Distance, Variogram vario){
        
        double res= 0.0;
        switch (vario.Type){
            case Exponential:
                if (Distance!=0) {
                    res = vario.Nugget + vario.Sill*(1-Math.exp(-Distance/vario.Range));
                }else{
                    res = 0;
                }
                break;
            case Gaussian:
                if (Distance!=0) {
                    res = vario.Nugget + vario.Sill*(1-Math.exp(-3*(Math.pow(Distance, 2))/(Math.pow(vario.Range,2))));
                }else{
                    res = 0;
                }
                break;
            case Spherical:
                
                if (Distance>vario.Range) {
                    res = vario.Nugget + vario.Sill;
                }
                else if (0<Distance && Distance<=vario.Range) {
                    res = vario.Nugget + vario.Sill*(1.5*Distance/vario.Range-0.5*Math.pow((Distance/vario.Range),3));
                }
                else
                {
                    res = 0;
                }
                break;
        }
        return res;
    }
    
    
    /**
     * Calculates the points for drawing the theoretical variogram
     * @param SVType
     * @return 
     */
    double[][] CalcTheoreticalSVValues(Variogram vario , double MaximumDisplyDistanst){
        double[][] res = new double[2*NumberOfLags+1][2];       //0=X,  1= Y
        for (int i = 0; i < res.length; i++) {
            res[i][0]=i*MaximumDisplyDistanst/(2*NumberOfLags);
            switch (vario.Type){
                case Exponential:
                    if (res[i][0]!=0) {
                        res[i][1]=vario.Nugget + vario.Sill*(1-Math.exp(-res[i][0]/vario.Range));
                    }
                    else{
                        res[i][1]=0;
                    }
                    
                    break;
                case Gaussian:
                    if (res[i][0]!=0) {
                        res[i][1]=vario.Nugget + vario.Sill*(1-Math.exp(-3*(Math.pow(res[i][0], 2))/(Math.pow(vario.Range,2))));
                    }else{
                        res[i][1]=0;
                    }
                    break;
                case Spherical:
                    if (res[i][0]>vario.Range) {
                        res[i][1]=vario.Nugget + vario.Sill;
                    }
                    else if (res[i][0]>0 && res[i][0]<=vario.Range) {
                        res[i][1]=vario.Nugget + vario.Sill*(1.5*res[i][0]/vario.Range-0.5*Math.pow((res[i][0]/vario.Range),3));
                    }
                    else
                    {
                        res[i][1] =0;
                    }
                    break;
            }
        }
        return res;
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
    
    /**
     * It calculates the location of each grid cell. the resolution should be set before calling this method
     * @return a point list
     */
    public List<point> calcInterpolationPoints(){
        double north, south, east, west;
        int nrows, ncols;
        double northing, easting;
        west = BMinX - 0.5 * resolution;
        north = BMaxY + 0.5 * resolution;
        nrows = (int) (Math.ceil((north - BMinY) / resolution));
        ncols = (int) (Math.ceil((BMaxX - west) / resolution));
        south = north - nrows * resolution;
        east = west + ncols * resolution;
        int row, col;
        List<point> pnts = new ArrayList();
        // Create the whitebox raster object.
        double halfResolution = resolution / 2;
        for (row = 0; row < nrows; row++) {
            for (col = 0; col < ncols; col++) {
                easting = (col * resolution) + (west + halfResolution);
                northing = (north - halfResolution) - (row * resolution);
                pnts.add(point(easting, northing, 0));
            }
        }
        return pnts;
    }
    
    public void BuildRaster(String outputRaster, List<point> pnts){
        double north, south, east, west;
        int nrows, ncols;
        double northing, easting;
        west = BMinX - 0.5 * resolution;
        north = BMaxY + 0.5 * resolution;
        nrows = (int) (Math.ceil((north - BMinY) / resolution));
        ncols = (int) (Math.ceil((BMaxX - west) / resolution));
        south = north - nrows * resolution;
        east = west + ncols * resolution;
        String outputHeader = outputRaster;
        FileWriter fw = null;
        BufferedWriter bw = null;
        PrintWriter out = null;
        String str1;
        double noData = -32768;
        // see if the output files already exist, and if so, delete them.
        if ((new File(outputHeader)).exists()) {
            (new File(outputHeader)).delete();
            (new File(outputHeader.replace(".dep", ".tas"))).delete();
        }
        try {
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
            str1 = "Preferred Palette:\t" + "rgb.pal";
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

        } catch (Exception e) {
            
            return;
        }
        int row, col;
        // Create the whitebox raster object.
        WhiteboxRaster image = new WhiteboxRaster(outputHeader, "rw");
        
        double halfResolution = resolution / 2;
    
        int nn = 0;
        for (row = 0; row < nrows; row++) {
            for (col = 0; col < ncols; col++) {
                easting = (col * resolution) + (west + halfResolution);
                northing = (north - halfResolution) - (row * resolution);

                image.setValue(row, col, pnts.get(nn).z);
                nn++;
            }
//                            if (cancelOp) {
//                                cancelOperation();
//                                return;
//                            }
//                            progress = (int) (100f * row / (nrows - 1));
//                            updateProgress("Interpolating point data:", progress);
        }
        image.addMetadataEntry("Created by the Kriging Interpolation Tool.");
        image.addMetadataEntry("Created on " + new Date());

        image.close();
    }
    
    /**
     * Gets the variogram and unknown point list and returns the interpolated values for the unknown points
     * @param variogram
     * @param pnts
     * @return 
     */
    public List<point> InterpolatePoints(Variogram variogram, List<point> pnts, int NumberOfNearestPoints)
    {
        
        double[] res = new double[NumberOfNearestPoints];
        double[][] D = new double[NumberOfNearestPoints + 1][1];
        
        List<point> NNPoitns = new ArrayList();
        
        for (int n = 0; n < pnts.size(); n++) {
            NNPoitns = getNNpoints(this.pointsTree, pnts.get(n), NumberOfNearestPoints);

            double[][] C = CalcConstantCoef(variogram, NNPoitns);
            double[] tm =  CalcVariableCoef(variogram, pnts.get(n),NNPoitns); ///------------
            for (int i = 0; i < tm.length; i++) {
                D[i][0]=tm[i];
            }
            //double[][] d = {{1,2,3},{4,5,6,},{7,8,10}};
            Matrix tmp = Matrix.constructWithCopy(C);
            Matrix VariableCoef = Matrix.constructWithCopy(D);
            Matrix w = tmp.solve(VariableCoef);
            double[][] Wi =  w.getArray();
            double s = 0;
            for (int i = 0; i < Wi.length-1; i++) {
                s = s + Wi[i][0]*NNPoitns.get(i).z;
            }
            pnts.get(n).z = s;
            //res[n]=s;
            s = 0;
        }
        
        return pnts;
    }
    /**
     * Returns the list of nearest neighbor points
     * @param Tree
     * @param pnt
     * @param numPointsToUse
     * @return 
     */
    private List<point> getNNpoints(KdTree<Double> Tree, point pnt, int numPointsToUse){
        double[] entry;
        //double[] outentry;
        entry = new double[]{pnt.y, pnt.x};
        List<KdTree.Entry<Double>> results;
        results = Tree.nearestNeighbor(entry, numPointsToUse, false);
        List<point> pnts = new ArrayList();
        List<point> res = new ArrayList();
        for (int i = 0; i < results.size(); i++) {
            point tmp = new point();
            //int id = results.get(i).value.intValue();
            res.add(Points.get(results.get(i).value.intValue()));
        }
        return res;
    }
    
    
    /**
     * calculates the D matrix for Kriging system
     * @param variogram
     * @param p is the unknown point
     * @param NNPoints is the list of nearest neighbor points
     * @return 
     */
    private double[] CalcVariableCoef(Variogram variogram, point p, List<point> NNPoints){
        int n = NNPoints.size();
        double[] mat = new double[n+1];
        double dist = 0.0;
        for (int i = 0; i < n; i++) {
            dist = Math.sqrt(Math.abs(Math.pow(NNPoints.get(i).x-p.x, 2))+
                    Math.abs(Math.pow(NNPoints.get(i).y-p.y, 2))); 
            mat[i]=getTheoreticalSVValue(dist, variogram);
        }
        mat[n]=1;
        return mat;
    }
    /**
     * This prepares the known points matrix for ordinary Kriging
     * @param variogarm
     * @return 
     */
    private double[][] CalcConstantCoef(Variogram variogarm, List<point> NNPoints ){
        int n = NNPoints.size();
        double[][] mat = new double[n+1][n+1];
        double dist = 0.0;
        for (int i = 0; i < n; i++) {
            for (int j = i; j < n; j++) {
                dist = Math.sqrt(Math.abs(Math.pow(NNPoints.get(i).x-NNPoints.get(j).x, 2))+
                        Math.abs(Math.pow(NNPoints.get(i).y-NNPoints.get(j).y, 2))); 
                mat[i][j]=getTheoreticalSVValue(dist, variogarm);
                mat[j][i]=mat[i][j];
            }
        }
        for (int i = 0; i < n; i++) {
            mat[i][n]=1;
            mat[n][i]=1;
        }
        
//        
//        String s= new String();
//        try {
//            PrintWriter pr = new PrintWriter("G:\\test.txt");
//            for (int i = 0; i < mat.length; i++) {
//                for (int j = 0; j < mat.length; j++) {
//                    s = s + "," + mat[i][j];
//                }
//                pr.println(s);
//                s = "";
//            }
//            pr.close();
//        } catch (FileNotFoundException ex) {
//            Logger.getLogger(Kriging.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        
        return mat;        
                
        
    }
    
    /**
     * Creates the pairs. calcs the distance and moment of inertia for each pair
     * It also calculates the min and max points and boundary
     * It also build the KDTree object to be used with the Kriging
     */
    void CalPairsOld () throws FileNotFoundException{
        MaximumDistance = 0;
        MinX = Double.POSITIVE_INFINITY;
        MinY = Double.POSITIVE_INFINITY;
        MaxX = Double.NEGATIVE_INFINITY;
        MaxY = Double.NEGATIVE_INFINITY;
        pointsTree = new KdTree.SqrEuclid<Double>(2, new Integer(this.Points.size()));
        double[] entry;
        
        
        String s= new String();
        PrintWriter pw ;
        pw = new PrintWriter("G:\\test.txt");

        
        
        for (int i = 0; i < this.Points.size(); i++) {
            
            if (this.Points.get(i).x<MinX) { MinX = this.Points.get(i).x;}
            if (this.Points.get(i).y<MinY) { MinY = this.Points.get(i).y;}
            if (this.Points.get(i).x>MaxX) { MaxX = this.Points.get(i).x;}
            if (this.Points.get(i).y>MaxY) { MaxY = this.Points.get(i).y;}
            
            entry = new double[]{this.Points.get(i).y, this.Points.get(i).x};
            pointsTree.addPoint(entry, (double)i);
            
            for (int j = 0; j < this.Points.size(); j++) {
                pair pr = new pair();
                pr.FirstP = i;
                pr.SecondP = j;
                pr.Distance = Math.sqrt(Math.pow((Points.get(i).x-Points.get(j).x),2)+
                    Math.pow((Points.get(i).y-Points.get(j).y),2));
                
                if (MaximumDistance<pr.Distance) {
                    MaximumDistance = pr.Distance;
                }
                
                if (Points.get(j).x>Points.get(i).x) {
                    if (Points.get(j).y>Points.get(i).y) {
                        pr.Direction = Math.asin((Points.get(j).y-Points.get(i).y)/pr.Distance);
                    }
                    else
                    {
                        pr.Direction = Math.asin((Points.get(j).y-Points.get(i).y)/pr.Distance)+2*Math.PI;
                    }
                }
                else 
                {
                
                    if (Points.get(j).y>Points.get(i).y) {
                        pr.Direction =Math.PI - Math.asin((Points.get(j).y-Points.get(i).y)/pr.Distance);
                    }
                    else
                    {
                        pr.Direction = - Math.asin((Points.get(j).y-Points.get(i).y)/pr.Distance)+ Math.PI;
                    }
                }
                pr.Direction = pr.Direction*180/Math.PI;
                
                pr.MomentI =Math.pow((Points.get(i).z - Points.get(j).z),2)/2;
                Pairs.add(pr);
                
                
                s =  Double.toString(pr.Distance) + "," + Double.toString(pr.Direction)+
                        "," + Double.toString(pr.MomentI)+ "," + Integer.toString(pr.FirstP)+
                        "," + Integer.toString(pr.SecondP);
                
                pw.println(s);
                
                
                
            }
        }
        pw.close();
        //LagSize  = MaximumDistance/NumberOfLags;
        BMaxX = MaxX;
        BMaxY = MaxY;
        BMinX = MinX;
        BMinY = MinY;

    }
    
    
     void CalPairs () throws FileNotFoundException{
        MaximumDistance = 0;
        MinX = Double.POSITIVE_INFINITY;
        MinY = Double.POSITIVE_INFINITY;
        MaxX = Double.NEGATIVE_INFINITY;
        MaxY = Double.NEGATIVE_INFINITY;
        pointsTree = new KdTree.SqrEuclid<Double>(2, new Integer(this.Points.size()));
        double[] entry;
        
        
        String s= new String();
        PrintWriter pw ;
        pw = new PrintWriter("G:\\test.txt");

        
        
        for (int i = 0; i < this.Points.size(); i++) {
            
            if (this.Points.get(i).x<MinX) { MinX = this.Points.get(i).x;}
            if (this.Points.get(i).y<MinY) { MinY = this.Points.get(i).y;}
            if (this.Points.get(i).x>MaxX) { MaxX = this.Points.get(i).x;}
            if (this.Points.get(i).y>MaxY) { MaxY = this.Points.get(i).y;}
            
            entry = new double[]{this.Points.get(i).y, this.Points.get(i).x};
            pointsTree.addPoint(entry, (double)i);
            
            for (int j = 0; j < this.Points.size(); j++) {
                pair pr = new pair();
                
                if (Points.get(i).x<=Points.get(j).x && i != j) {
                    


                    pr.FirstP = i;
                    pr.SecondP = j;
                    pr.Distance = Math.sqrt(Math.pow((Points.get(i).x-Points.get(j).x),2)+
                        Math.pow((Points.get(i).y-Points.get(j).y),2));

                    pr.HorDistance = (Points.get(j).x-Points.get(i).x);
                    pr.VerDistance = (Points.get(j).y-Points.get(i).y);

                    if (MaximumDistance<pr.Distance) {
                        MaximumDistance = pr.Distance;
                    }

                    if (Points.get(j).x>Points.get(i).x) {
                        if (Points.get(j).y>Points.get(i).y) {
                            pr.Direction = Math.asin((Points.get(j).y-Points.get(i).y)/pr.Distance);
                        }
                        else
                        {
                            pr.Direction = Math.asin((Points.get(j).y-Points.get(i).y)/pr.Distance)+2*Math.PI;
                        }
                    }
                    else 
                    {

                        if (Points.get(j).y>Points.get(i).y) {
                            pr.Direction =Math.PI - Math.asin((Points.get(j).y-Points.get(i).y)/pr.Distance);
                        }
                        else
                        {
                            pr.Direction = - Math.asin((Points.get(j).y-Points.get(i).y)/pr.Distance)+ Math.PI;
                        }
                    }
                    pr.Direction = pr.Direction*180/Math.PI;

                    pr.MomentI =Math.pow((Points.get(i).z - Points.get(j).z),2)/2;
                    Pairs.add(pr);


                    s =  Double.toString(pr.Distance) + "," + Double.toString(pr.Direction)+
                            "," + Double.toString(pr.MomentI)+
                            "," + Double.toString(pr.HorDistance)+
                            ","+Double.toString(pr.VerDistance)+
                            "," + Integer.toString(pr.FirstP)+
                            "," + Integer.toString(pr.SecondP);

                    pw.println(s);
                }
                
                
            }
        }
        pw.close();
        //LagSize  = MaximumDistance/NumberOfLags;
        BMaxX = MaxX;
        BMaxY = MaxY;
        BMinX = MinX;
        BMinY = MinY;

    }
    
    /**
     * It gets the semivariogram type and binnes list and draw a graph for them
     * TheoryVariogram should be called first
     * @param Binnes
     * @param Type 
     */
    public void DrawSemiVariogram(List<bin> Binnes, Variogram variogram){
        XYSeriesCollection sampleCollct = new XYSeriesCollection();
        XYSeries series = new XYSeries("Sample Variogram");
        int j = 0;
        for (Iterator<bin> i = Binnes.iterator(); i.hasNext(); )
        {
            series.add(Binnes.get(j).Distance,Binnes.get(j).Value);
            i.next();
            j++;
        }
        sampleCollct.addSeries(series);
        
        double[][] res =  CalcTheoreticalSVValues(variogram, Binnes.get(Binnes.size()-1).Distance);
        
        XYSeries seriesTSV = new XYSeries("Theoretical Variogram");
        
        for (int i = 0; i < res.length; i++) {
            seriesTSV.add(res[i][0],res[i][1]);
        }

        XYSeriesCollection theorCollct = new XYSeriesCollection();
        
        theorCollct.addSeries(seriesTSV);
        
        int t = 0;
        
        XYLineAndShapeRenderer xylineshapRend = new XYLineAndShapeRenderer(false, true);
        
        XYDataset xydataset = sampleCollct;
        
        XYPlot xyplot1 = new XYPlot(xydataset,new NumberAxis(),null, xylineshapRend);
        
        xyplot1.setDataset(1,theorCollct);
        XYLineAndShapeRenderer lineshapRend = new XYLineAndShapeRenderer(true, false);
        xyplot1.setRenderer(1, lineshapRend);
        xyplot1.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
        
        //NumberAxis numberaxis = new NumberAxis("Value");
        
        //numberaxis.setAutoRangeIncludesZero(true);
        
        CombinedRangeXYPlot combinedrangexyplot = new CombinedRangeXYPlot();
        combinedrangexyplot.add (xyplot1);
        
        JFreeChart chart = new JFreeChart("Semivariogram",
				JFreeChart.DEFAULT_TITLE_FONT, combinedrangexyplot, true);
        
//        JFreeChart chart = ChartFactory.createScatterPlot(
//            "Semivariogram", // chart title
//            "Distance", // x axis label
//            "Moment of Inertia", // y axis label
//            result, // data  
//            PlotOrientation.VERTICAL,
//            true, // include legend
//            true, // tooltips
//            false // urls
//            );

        // create and display a frame...
        ChartFrame frame = new ChartFrame("First", chart);
        frame.pack();
        frame.setVisible(true);
    }
    
    /**
     * This Calculates the Sill and Range Value for the Theoretical Semi Variogram
     * Points list should be filled first
     * This function fills the Sill and Range in the Kriging object 
     * @param Type
     * @param DistanseRatio is the ratio of the maximum distance in point to the maximum distance of the variogram
     * @param NumberOfLags 
     */
    public Variogram SemiVariogram(SemiVariogramType Type, double DistanseRatio, int NumberOfLags){
        this.NumberOfLags = NumberOfLags;
        try {
            CalPairs();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Kriging.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (this.LagSize ==0) {
            this.LagSize = (this.MaximumDistance*DistanseRatio)/this.NumberOfLags;
        }
        
        CalcBinnes(this.LagSize*this.NumberOfLags);
        return null; //TheoryVariogram(Type);
    }
    
    public static void main(String[] args) 
    {
        Kriging k = new Kriging();
        
        //k.Points  =  k.ReadPointFile("G:\\Papers\\AGU 2013\\Sample\\Sample.shp","V");
        k.Points  =  k.ReadPointFile("G:\\Papers\\AGU 2013\\WakerLake\\WakerLake.shp","V");
        k.ConsiderNugget = false;
        k.LagSize = 5;
        Variogram var = k.SemiVariogram(SemiVariogramType.Exponential, 0.27, 10);
        k.resolution = 2.5;
        List<point> pnts = k.calcInterpolationPoints();
        
        
        //var.Range = 43.56409;
        //var.Sill = 96147.91;
        //var.Type = SemiVariogramType.Exponential;
        //Kriging.point p = k.point(65, 137, 0);
//        List<Kriging.point> pnts = new ArrayList();
//        pnts.add(p);
        
        pnts = k.InterpolatePoints(var, pnts,5);
        k.BuildRaster("G:\\Papers\\AGU 2013\\WakerLake\\WakerLakeOut50.dep", pnts);
        
        //k.DrawSemiVariogram(k.Binnes, var);
    }
}
