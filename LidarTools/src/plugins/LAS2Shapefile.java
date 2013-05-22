/*
 * Copyright (C) 2011-2012 Dr. John Lindsay <jlindsay@uoguelph.ca>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package plugins;

import java.io.File;
import whitebox.geospatialfiles.LASReader;
import whitebox.geospatialfiles.LASReader.PointRecord;
import whitebox.geospatialfiles.ShapeFile;
import whitebox.geospatialfiles.shapefile.DBF.DBFField;
import whitebox.geospatialfiles.shapefile.DBF.DBFWriter;
import whitebox.geospatialfiles.shapefile.ShapeType;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import whitebox.interfaces.WhiteboxPlugin;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.parallel.Parallel;

/**
 * WhiteboxPlugin is used to define a plugin tool for Whitebox GIS.
 * 
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class LAS2Shapefile implements WhiteboxPlugin {

    private WhiteboxPluginHost myHost = null;
    private String[] args;
    private long progress = 0;
    private long maxWork = 0;
    private boolean message = true;
    
    /**
     * Used to retrieve the plugin tool's name. This is a short, unique name
     * containing no spaces.
     *
     * @return String containing plugin name.
     */
    @Override
    public String getName() {
        return "LAS2Shapefile";
    }

    /**
     * Used to retrieve the plugin tool's descriptive name. This can be a longer
     * name (containing spaces) and is used in the interface to list the tool.
     *
     * @return String containing the plugin descriptive name.
     */
    @Override
    public String getDescriptiveName() {
        return "Convert LAS to Shapefile (LAS2Shapefile)";
    }

    /**
     * Used to retrieve a short description of what the plugin tool does.
     *
     * @return String containing the plugin's description.
     */
    @Override
    public String getToolDescription() {
        return "Converts a LAS file into a Shapefile.";
    }

    /**
     * Used to identify which toolboxes this plugin tool should be listed in.
     *
     * @return Array of Strings.
     */    
    @Override
    public String[] getToolbox() {
        String[] ret = {"LidarTools", "ConversionTools"};
        return ret;
    }

    /**
     * Sets the WhiteboxPluginHost to which the plugin tool is tied. This is the
     * class that the plugin will send all feedback messages, progress updates,
     * and return objects.
     *
     * @param host The WhiteboxPluginHost that called the plugin tool.
     */    
    @Override
    public void setPluginHost(WhiteboxPluginHost host) {
        myHost = host;
    }

    /**
     * Used to communicate feedback pop-up messages between a plugin tool and
     * the main Whitebox user-interface.
     *
     * @param feedback String containing the text to display.
     */
    private void showFeedback(String message) {
        if (myHost != null) {
            myHost.showFeedback(message);
        } else {
            System.out.println(message);
        }
    }

    /**
     * Used to communicate a return object from a plugin tool to the main
     * Whitebox user-interface.
     *
     * @return Object, such as an output WhiteboxRaster.
     */
    private void returnData(Object ret) {
        if (myHost != null) {
            myHost.returnData(ret);
        }
    }
    private int previousProgress = 0;
    private String previousProgressLabel = "";

    /**
     * Used to communicate a progress update between a plugin tool and the main
     * Whitebox user interface.
     *
     * @param progressLabel A String to use for the progress label.
     * @param progress Float containing the progress value (between 0 and 100).
     */
    private void updateProgress(String progressLabel, int progress) {
        if (myHost != null && ((progress != previousProgress)
                || (!progressLabel.equals(previousProgressLabel)))) {
            myHost.updateProgress(progressLabel, progress);
        }
        previousProgress = progress;
        previousProgressLabel = progressLabel;
    }

    /**
     * Used to communicate a progress update between a plugin tool and the main
     * Whitebox user interface.
     *
     * @param progress Float containing the progress value (between 0 and 100).
     */
    private void updateProgress(int progress) {
        if (myHost != null && progress != previousProgress) {
            myHost.updateProgress(progress);
        }
        previousProgress = progress;
    }

    /**
     * Sets the arguments (parameters) used by the plugin.
     *
     * @param args
     */
    @Override
    public void setArgs(String[] args) {
        this.args = args.clone();
    }
    private boolean cancelOp = false;

    /**
     * Used to communicate a cancel operation from the Whitebox GUI.
     *
     * @param cancel Set to true if the plugin should be canceled.
     */
    @Override
    public void setCancelOp(boolean cancel) {
        cancelOp = cancel;
    }

    private void cancelOperation() {
        if (message){
            message = false;
            showFeedback("Operation cancelled.");
            updateProgress("Progress: ", 0);
        }
    }
    private boolean amIActive = false;

    /**
     * Used by the Whitebox GUI to tell if this plugin is still running.
     *
     * @return a boolean describing whether or not the plugin is actively being
     * used.
     */
    @Override
    public boolean isActive() {
        return amIActive;
    }

    @Override
    public void run() {
        amIActive = true;

        String inputFilesString = null;
        String[] pointFiles;
                
        // get the arguments
        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }
        inputFilesString = args[0];
        
        // check to see that the inputHeader and outputHeader are not null.
        if ((inputFilesString.length() <= 0)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        int threads = Parallel.getPluginProcessors();
        // TODO: remove after testing is done
        System.out.println("Number of threads" + threads);
        
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        
        
        try {
            
            pointFiles = inputFilesString.split(";");
            int numPointFiles = pointFiles.length;

            // this is only for the progress bar and is very wasteful
            LASReader las;
            for (int i = 0; i < numPointFiles; i++) {
                las = new LASReader(pointFiles[i]);
                maxWork += las.getNumPointRecords();
            }
                        
            // add files to the thread pool
            for (int j = 0; j < numPointFiles; j++) {
                pool.execute(new Las2ShapeWork(pointFiles[j]));               
            }

            pool.shutdown();
            while(!pool.awaitTermination(1, TimeUnit.SECONDS)) { };
                        
            returnData(pointFiles[0].replace(".las", ".shp"));
            
        } catch (OutOfMemoryError oe) {
            showFeedback("The Java Virtual Machine (JVM) is out of memory");
        } catch (Exception e) {
            showFeedback(e.getMessage());
        } finally {
            updateProgress("Progress: ", 0);
            // tells the main application that this process is completed.
            amIActive = false;
            myHost.pluginComplete();
        }
    
        //TODO: remove after testing is done
        System.out.println("Done");
        // just in case
        pool.shutdown();
    
    }
    
    private class Las2ShapeWork implements Runnable {
        
        private String pointFile;
        
        Las2ShapeWork(String pointFile) {
            this.pointFile = pointFile;
        }
        
        
        @Override
        public void run() {
            
            // if we are already cancelled dont bother doing anything
            if (cancelOp) { return; }
            
            long numPointsInFile = 0;
            int numPoints = 0;
            double x, y;
            double z;
            int intensity;
            byte classValue, numReturns, returnNum;
            int n;
            
            PointRecord point;
            
            try {
            
                LASReader las = new LASReader(pointFile);

                long oneHundredthTotal = las.getNumPointRecords() / 100;

                // create the new shapefile
                String outputFile = pointFile.replace(".las", ".shp");
                File file = new File(outputFile);
                if (file.exists()) {
                    file.delete();
                }

                // set up the output files of the shapefile and the dbf
                ShapeFile output = new ShapeFile(outputFile, ShapeType.POINT);

                DBFField fields[] = new DBFField[6];

                fields[0] = new DBFField();
                fields[0].setName("FID");
                fields[0].setDataType(DBFField.FIELD_TYPE_N);
                fields[0].setFieldLength(10);
                fields[0].setDecimalCount(0);

                fields[1] = new DBFField();
                fields[1].setName("Z");
                fields[1].setDataType(DBFField.FIELD_TYPE_N);
                fields[1].setFieldLength(10);
                fields[1].setDecimalCount(3);

                fields[2] = new DBFField();
                fields[2].setName("I");
                fields[2].setDataType(DBFField.FIELD_TYPE_N);
                fields[2].setFieldLength(8);
                fields[2].setDecimalCount(0);

                fields[3] = new DBFField();
                fields[3].setName("CLASS");
                fields[3].setDataType(DBFField.FIELD_TYPE_N);
                fields[3].setFieldLength(4);
                fields[3].setDecimalCount(0);

                fields[4] = new DBFField();
                fields[4].setName("RTN_NUM");
                fields[4].setDataType(DBFField.FIELD_TYPE_N);
                fields[4].setFieldLength(4);
                fields[4].setDecimalCount(0);

                fields[5] = new DBFField();
                fields[5].setName("NUM_RTNS");
                fields[5].setDataType(DBFField.FIELD_TYPE_N);
                fields[5].setFieldLength(4);
                fields[5].setDecimalCount(0);

                String DBFName = output.getDatabaseFile();
                DBFWriter writer = new DBFWriter(new File(DBFName)); /*
                 * this DBFWriter object is now in Syc Mode
                 */

                writer.setFields(fields);

                numPointsInFile = las.getNumPointRecords();
                // first count how many valid points there are.
                numPoints = 0;
                n = 0;
                //progress = 0;
                System.out.println("Running a new file with " + numPointsInFile + " points");
                int lastA = 0;
                for (int a = 0; a < numPointsInFile; a++) {
                    point = las.getPointRecord(a);
                    if (!point.isPointWithheld()) {
                        x = point.getX();
                        y = point.getY();
                        z = point.getZ();
                        intensity = point.getIntensity();
                        classValue = point.getClassification();
                        returnNum = point.getReturnNumber();
                        numReturns = point.getNumberOfReturns();

                        whitebox.geospatialfiles.shapefile.Point wbGeometry = new whitebox.geospatialfiles.shapefile.Point(x, y);
                        output.addRecord(wbGeometry);

                        // throwing all these objects on the heap is probably very slow, but using primitives breaks the code..
                        Object rowData[] = new Object[6];
                        rowData[0] = new Double(numPoints + 1);
                        rowData[1] = new Double(z);
                        rowData[2] = new Double(intensity);
                        rowData[3] = new Double(classValue);
                        rowData[4] = new Double(returnNum);
                        rowData[5] = new Double(numReturns);
                        writer.addRecord(rowData);

                        numPoints++;
                    }
                    n++;
                    if (n >= oneHundredthTotal) {
                        n = 0;
                        if (cancelOp) {
                            cancelOperation();
                            return;
                        }
                        progress += (a - lastA);
                        lastA = a;
                        updateProgress("Converting file(s):", (int)((progress*100)/maxWork));
                    }
                }

                output.write();
                writer.write();
            
            } catch (Exception e) {
                showFeedback(e.getMessage());
            } 
        }
                
    }
    
//    // this is only used for debugging the tool
//    public static void main(String[] args) {
//        LAS2Shapefile L2S = new LAS2Shapefile();
//        args = new String[1];
//        args[0] = "/Users/johnlindsay/Documents/Data/Rondeau LiDAR/LAS classified/403_4696.las;/Users/johnlindsay/Documents/Data/Rondeau LiDAR/LAS classified/403_4695.las";
//        L2S.setArgs(args);
//        L2S.run();
//        
//    }
}