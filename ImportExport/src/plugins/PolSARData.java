/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plugins;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import whitebox.geospatialfiles.WhiteboxRaster;
/**
 *
 * @author eroshani
 */
public class PolSARData {
    private String annFile;
    public String OutputFile; //ReadOnly
    
    public double grd_mag_row_addr; //Center Latitude of Upper Left Pixel of Image
    public double grd_mag_col_addr; //Center Longitude of Upper Left Pixel of Image
    public double grd_mag_row_mult; //GRD Latitude Pixel Spacing  reselotion
    public double grd_mag_col_mult; //GRD Longitude Pixel Spacing
    public int grd_mag_set_rows;
    public int grd_mag_set_cols;

    public double mlc_mag_row_addr; //Center Latitude of Upper Left Pixel of Image
    public double mlc_mag_col_addr; //Center Longitude of Upper Left Pixel of Image
    public double mlc_mag_row_mult; //MLC Latitude Pixel Spacing  reselotion
    public double mlc_mag_col_mult; //MLC Longitude Pixel Spacing
    public int mlc_mag_set_rows;
    public int mlc_mag_set_cols;
    
    
    
    public int cols;
    public int rows;
    public double resolution;
    
    public String path;
    
    //GRD files
    public String grdHHHHfile;
    public String grdHVHVfile;
    public String grdVVVVfile;
    public String grdHHHVfile;
    public String grdHHVVfile;
    public String grdHVVVfile;

    //MLC files
    public String mlcHHHHfile;
    public String mlcHVHVfile;
    public String mlcVVVVfile;
    public String mlcHHHVfile;
    public String mlcHHVVfile;
    public String mlcHVVVfile;

    
    public enum UAVSARFile{grdHHHH,grdHVHV,grdVVVV,grdHHHV,grdHHVV,grdHVVV, mlcHHHH,mlcHVHV,mlcVVVV,mlcHHHV,mlcHHVV,mlcHVVV}
    
    public String getAnnFile(){
        return annFile;
    }
    
    public PolSARData (String annotationFile){
        annFile = annotationFile;
        BufferedReader reader;
        String[] tmp;
        try {
            reader = new BufferedReader(new FileReader(annFile));
            path = annFile.substring(0, annFile.lastIndexOf('\\')+1);
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith(";") && line.length()>2) {
                    //Read the GRD file names
                    tmp = line.split(" = ");
                    switch (tmp[0]){
                        case "grdHHHH": grdHHHHfile = tmp[1].split(" ")[0];
                            break;
                        case "grdHVHV": grdHVHVfile = tmp[1].split(" ")[0];
                            break;
                        case "grdVVVV": grdVVVVfile = tmp[1].split(" ")[0];
                            break;
                        case "grdHHHV": grdHHHVfile = tmp[1].split(" ")[0];
                            break;
                        case "grdHHVV": grdHHVVfile = tmp[1].split(" ")[0];
                            break;
                        case "grdHVVV": grdHVVVfile = tmp[1].split(" ")[0];
                            break;
                        
                        case "mlcHHHH": mlcHHHHfile = tmp[1].split(" ")[0];
                            break;
                        case "mlcHVHV": mlcHVHVfile = tmp[1].split(" ")[0];
                            break;
                        case "mlcVVVV": mlcVVVVfile = tmp[1].split(" ")[0];
                            break;
                        case "mlcHHHV": mlcHHHVfile = tmp[1].split(" ")[0];
                            break;
                        case "mlcHHVV": mlcHHVVfile = tmp[1].split(" ")[0];
                            break;
                        case "mlcHVVV": mlcHVVVfile = tmp[1].split(" ")[0];
                            break;
                            
                    }
                    if (tmp[0].startsWith("grd_mag.set_rows")) {
                        grd_mag_set_rows = Integer.parseInt(tmp[1].split(" ")[0]);
                    }
                    
                    if (tmp[0].startsWith("grd_mag.set_cols")) {
                        grd_mag_set_cols = Integer.parseInt(tmp[1].split(" ")[0]);
                    }

                    if (tmp[0].startsWith("grd_mag.row_addr")) {
                        grd_mag_row_addr = Double.parseDouble(tmp[1].split(" ")[0]);
                    }
                    if (tmp[0].startsWith("grd_mag.col_addr")) {
                        grd_mag_col_addr = Double.parseDouble(tmp[1].split(" ")[0]);
                    }
                    
                    if (tmp[0].startsWith("grd_mag.row_mult")) {
                        grd_mag_row_mult = Double.parseDouble(tmp[1].split(" ")[0]);
                    }
                    if (tmp[0].startsWith("grd_mag.col_mult")) {
                        grd_mag_col_mult = Double.parseDouble(tmp[1].split(" ")[0]);
                    }
                    
                    //reads the mlc raster spec
                    if (tmp[0].startsWith("mlc_mag.set_rows")) {
                        mlc_mag_set_rows = Integer.parseInt(tmp[1].split(" ")[0]);
                    }
                    
                    if (tmp[0].startsWith("mlc_mag.set_cols")) {
                        mlc_mag_set_cols = Integer.parseInt(tmp[1].split(" ")[0]);
                    }

                    if (tmp[0].startsWith("mlc_mag.row_addr")) {
                        mlc_mag_row_addr = Double.parseDouble(tmp[1].split(" ")[0]);
                    }
                    if (tmp[0].startsWith("mlc_mag.col_addr")) {
                        mlc_mag_col_addr = Double.parseDouble(tmp[1].split(" ")[0]);
                    }
                    
                    if (tmp[0].startsWith("mlc_mag.row_mult")) {
                        mlc_mag_row_mult = Double.parseDouble(tmp[1].split(" ")[0]);
                    }
                    if (tmp[0].startsWith("mlc_mag.col_mult")) {
                        mlc_mag_col_mult = Double.parseDouble(tmp[1].split(" ")[0]);
                    }
                }
            }
            reader.close();
            //ConvertToDep(grd_mag_row_addr, grd_mag_col_addr,Math.abs(grd_mag_row_mult),Math.abs(grd_mag_col_mult), grd_mag_set_rows, grd_mag_set_cols, UAVSARFile.grdVVVV,false);
            //ConvertToDep(mlc_mag_row_addr, mlc_mag_col_addr,Math.abs(mlc_mag_row_mult),Math.abs(mlc_mag_col_mult), mlc_mag_set_rows, mlc_mag_set_cols, UAVSARFile.mlcHHHH,false);
            

        } catch (FileNotFoundException ex) {
            Logger.getLogger(PolSARData.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PolSARData.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
public void ConvertToDep (double firstRowAddr, double firstColAddr, double rowResolution,double colResolution , int rows, int cols, UAVSARFile datafile, boolean imaginary){
    RandomAccessFile rIn = null;
    ByteBuffer buf = null;
    float x;
    double north, south, east, west;
    String str1;
    double minX = firstColAddr;
    double maxY = firstRowAddr;
    double maxX = minX+cols*colResolution;
    double minY = maxY-rows*rowResolution;
    int nrows, ncols;
    FileWriter fw = null;
    BufferedWriter bw = null;
    PrintWriter out = null;
    double noData = -32768;
    String filename= "";
    //String inputHeader = "C:\\PDF\\Radar\\winnip_31606_12061_006_120717_L090_CX_01_grd\\winnip_31606_12061_006_120717_L090HHHH_CX_01.grd";
    //String inputHeader = "C:\\PDF\\Radar\\winnip_31606_12061_006_120717_L090_CX_01_grd\\winnip_31606_12061_006_120717_L090HVHV_CX_01.grd";
    int n = 1;  //multiplier to read 4 byte and 8 byte data
    String ext = "";
    switch(datafile){
        case grdHHHH: 
            filename = grdHHHHfile;
            n = 1;
            ext="grd";
            break;
        case grdHVHV: 
            filename = grdHVHVfile;
            n = 1;
            ext="grd";
            break;
        case grdVVVV: 
            filename = grdVVVVfile;
            n = 1;
            ext="grd";
            break;
        case grdHHHV:
            filename = grdHHHVfile;
            n = 2;
            ext="grd";
            break;
        case grdHHVV:
            filename = grdHHVVfile;
            n = 2;
            ext="grd";
            break;
        case grdHVVV:
            filename = grdHVVVfile;
            n = 2;
            ext="grd";
            break;
            
        case mlcHHHH: 
            filename = mlcHHHHfile;
            n = 1;
            ext="mlc";
            break;
        case mlcHVHV: 
            filename = mlcHVHVfile;
            n = 1;
            ext="mlc";
            break;
        case mlcVVVV: 
            filename = mlcVVVVfile;
            n = 1;
            ext="mlc";
            break;
        case mlcHHHV:
            filename = mlcHHHVfile;
            n = 2;
            ext="mlc";
            break;
        case mlcHHVV:
            filename = mlcHHVVfile;
            n = 2;
            ext="mlc";
            break;
        case mlcHVVV:
            filename = mlcHVVVfile;
            n = 2;
            ext="mlc";
            break;
            
    }
    String inputHeader = path + filename;
    double minZ = Double.MAX_VALUE;
    double maxZ = -Double.MAX_VALUE;

    try{
        String outputHeader;
        if (imaginary) {
            outputHeader = inputHeader.replace("." + ext , "Imaginary.dep");
        }
        else{
            outputHeader = inputHeader.replace("." + ext , ".dep");
        }
        OutputFile = outputHeader;

        // see if the output files already exist, and if so, delete them.
        if ((new File(outputHeader)).exists()) {
            (new File(outputHeader)).delete();
            (new File(outputHeader.replace(".dep", ".tas"))).delete();
        }


        // What are north, south, east, and west and how many rows and 
        // columns should there be?
        west = minX - 0.5 * colResolution;
        north = maxY + 0.5 * rowResolution;
        nrows = rows; //(int) (Math.ceil((north - minY) / resolution));
        ncols = cols; //(int) (Math.ceil((maxX - west) / resolution));
        south = north - nrows * rowResolution;
        east = west + ncols * colResolution;

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
                str1 = "Preferred Palette:\t" + "grey.pal";
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
            System.out.print(e.getMessage().toString());
            return;
        }
        WhiteboxRaster image = new WhiteboxRaster(outputHeader, "rw");
        buf = ByteBuffer.allocate(n*cols*4);
        image.createNewDataFile();
        rIn = new RandomAccessFile(
                inputHeader, "r");

        FileChannel inChannel = rIn.getChannel();

        double[] v = new double[cols];
        for (int r = 0; r < rows; r++) {
            inChannel.position(r*n*cols*4);
            inChannel.read(buf);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.rewind();
            
            for (int c = 0; c < cols;c++) {
                v[c] = buf.getFloat(((imaginary)?(4):(0))+c*n*4);
                if (v[c]>maxZ) {maxZ=v[c];}
                if (v[c]<minZ) {minZ=v[c];}
            }
            image.setRowValues(r, v);
            buf.clear();
        }
        image.addMetadataEntry("Created by the "
        + " tool.");
        image.addMetadataEntry("Created on " + new Date());
        
        image.close(minZ,maxZ);
        //double ddd = 0;
    }catch (Exception e) {
        System.out.print(e.getMessage().toString());
    } finally {
        if (rIn != null) {
            try {
                rIn.close();
            } catch (Exception e) {
            }
        }
    }
}

//public static void main(String[] args) {
//    PolSARData psar = new PolSARData
//            ("C:\\PDF\\Radar\\winnip_31606_12061_006_120717_L090_CX_01_grd\\winnip_31606_12061_006_120717_L090_CX_01.ann");
////    PolSARData psar = new PolSARData
////            ("C:\\PDF\\Radar\\winnip_31606_12061_006_120717_L090_CX_01_mlc\\winnip_31606_12061_006_120717_L090_CX_01.ann");
//    
//    }   
}
