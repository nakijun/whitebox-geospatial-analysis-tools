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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import whitebox.geospatialfiles.LASReader;
import whitebox.geospatialfiles.LASReader.PointRecord;
import whitebox.interfaces.WhiteboxPlugin;
import whitebox.interfaces.WhiteboxPluginHost;

/**
 * WhiteboxPlugin is used to define a plugin tool for Whitebox GIS.
 * 
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class LAS2ASCII implements WhiteboxPlugin {

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
        return "LAS2ASCII";
    }

    /**
     * Used to retrieve the plugin tool's descriptive name. This can be a longer
     * name (containing spaces) and is used in the interface to list the tool.
     *
     * @return String containing the plugin descriptive name.
     */
    @Override
    public String getDescriptiveName() {
        return "Convert LAS to ASCII (LAS2ASCII)";
    }

    /**
     * Used to retrieve a short description of what the plugin tool does.
     *
     * @return String containing the plugin's description.
     */
    @Override
    public String getToolDescription() {
        return "Converts a LAS file into an ASCII text file.";
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
        
        if (message) {
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
    
    private ArrayList<String> sortFiles(String[] pointFiles, long numOfPointFiles){
        ArrayList<String> sortedNames  = new ArrayList<String>();
        ArrayList<Long> sortedSizes  = new ArrayList<Long>();
        long currentSize = 0;
        int j,i;
        
        LASReader las;  
        for (i = 0; i < numOfPointFiles; i++) {
            
            las = new LASReader(pointFiles[i]);
            currentSize = las.getNumPointRecords();
            maxWork += currentSize;
            
            for(j = 0; j < i; j++){
                if(currentSize >= sortedSizes.get(j)){
                    sortedNames.add(j, pointFiles[i]);
                    sortedSizes.add(j, currentSize);
                    break;
                }
            }
            
            //We didn't place it yet
            if (j == i){
                sortedNames.add(pointFiles[i]);
                sortedSizes.add(currentSize);
            }
            
            //System.out.println(sortedNames.get(j));
        }
        
        return sortedNames;
    }

    @Override
    @SuppressWarnings("empty-statement")
    public void run() {
        amIActive = true;

        String inputFilesString;
        String[] pointFiles;
        
        long start = System.nanoTime();
        
        // get the arguments
        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }
        
        int threads = Runtime.getRuntime().availableProcessors();
        // TODO: remove after testing is done
        System.out.println("Number of threads" + threads);
        
        ExecutorService pool = Executors.newFixedThreadPool(threads);
                
        inputFilesString = args[0];
        
        // check to see that the inputHeader and outputHeader are not null.
        if ((inputFilesString.length() <= 0)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            
            pointFiles = inputFilesString.split(";");
            int numPointFiles = pointFiles.length;
            
            // Sort the file sizes to run the largest of them all first
            ArrayList<String> sortedNames;    
            sortedNames = sortFiles(pointFiles, numPointFiles);
                       
            //PointRecColours pointColours;
            for (int j = 0; j < numPointFiles; j++) {
                pool.execute(new Las2AsciiWork(sortedNames.get(j)));               
            }
        
            pool.shutdown();
            while(!pool.awaitTermination(1, TimeUnit.SECONDS)) { };
            
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
        
        long end = System.nanoTime();
        System.out.println( "Time Elapsed: " + (end - start)/1000000000.0 );
    }
    
    
    private class Las2AsciiWork implements Runnable {
        
        private String pointFile;
        //int progress;
        
        public Las2AsciiWork(String pointFile) {
            this.pointFile = pointFile;
        }
                
        @Override
        public void run() {
        
            // if we are already cancelled dont bother doing anything
            if (cancelOp) { return; }
                       
            int intensity;
            double x, y;
            double z;
            byte classValue, numReturns, returnNum;
            int a, n;
            PointRecord point;
            long numPointsInFile = 0;
            
            FileWriter fw = null;
            BufferedWriter bw = null;
            PrintWriter out = null;
                        
            try {
            
                LASReader las = new LASReader(pointFile);

                long oneHundredthTotal = las.getNumPointRecords() / 100;

                // create the new text file
                File file = new File(pointFile.replace(".las", ".txt"));
                if (file.exists()) {
                    file.delete();
                }

                fw = new FileWriter(file, false);
                bw = new BufferedWriter(fw);
                out = new PrintWriter(bw, true);

                // first count how many valid points there are.
                numPointsInFile = las.getNumPointRecords();

                // TODO: remove when testing is done
                System.out.println("Num points in file: " + numPointsInFile);

                n = 0;
                int lastA = 0;
                for (a = 0; a < numPointsInFile; a++) {
                    point = las.getPointRecord(a);
                    if (!point.isPointWithheld()) {
                        x = point.getX();
                        y = point.getY();
                        z = point.getZ();
                        intensity = point.getIntensity();
                        classValue = point.getClassification();
                        returnNum = point.getReturnNumber();
                        numReturns = point.getNumberOfReturns();
                        out.println((a + 1) + " " + x + " " + y + " " + z + " " + intensity + 
                                " " + classValue + " " + returnNum + " " + numReturns);
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
            } catch (Exception e ) {
                showFeedback(e.getMessage());
            } finally {
                if (out != null || bw != null) {
                    out.flush();
                    out.close();
                }              
            }
            
            
        }
     
    }
    
//    // this is only used for debugging the tool
//    public static void main(String[] args) {
//        LAS2ASCII L2A = new LAS2ASCII();
//        args = new String[1];
//        args[0] = "/Users/johnlindsay/Documents/Data/Rondeau LiDAR/LAS classified/403_4696.las";
//        L2A.setArgs(args);
//        L2A.run();
//        
//    }
}