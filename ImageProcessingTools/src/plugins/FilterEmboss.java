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

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.interfaces.WhiteboxPlugin;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.parallel.Parallel;

/**
 * WhiteboxPlugin is used to define a plugin tool for Whitebox GIS.
 * @author johnlindsay
 */
public class FilterEmboss implements WhiteboxPlugin {
    
    private WhiteboxPluginHost myHost = null;
    private String[] args;
    private int completedRows; //used to update progress
    private int totalRows; //used for update progress
    private boolean message = true;
    
    /**
     * Used to retrieve the plugin tool's name. This is a short, unique name containing no spaces.
     * @return String containing plugin name.
     */
    @Override
    public String getName() {
        return "FilterEmboss";
    }
    /**
     * Used to retrieve the plugin tool's descriptive name. This can be a longer name (containing spaces) and is used in the interface to list the tool.
     * @return String containing the plugin descriptive name.
     */
    @Override
    public String getDescriptiveName() {
    	return "Emboss Filter";
    }
    /**
     * Used to retrieve a short description of what the plugin tool does.
     * @return String containing the plugin's description.
     */
    @Override
    public String getToolDescription() {
    	return "Performs a emboss filter on an image.";
    }
    /**
     * Used to identify which toolboxes this plugin tool should be listed in.
     * @return Array of Strings.
     */
    @Override
    public String[] getToolbox() {
    	String[] ret = { "Filters" };
    	return ret;
    }
    /**
     * Sets the WhiteboxPluginHost to which the plugin tool is tied. This is the class
     * that the plugin will send all feedback messages, progress updates, and return objects.
     * @param host The WhiteboxPluginHost that called the plugin tool.
     */  
    @Override
    public void setPluginHost(WhiteboxPluginHost host) {
        myHost = host;
    }
    /**
     * Used to communicate feedback pop-up messages between a plugin tool and the main Whitebox user-interface.
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
     * Used to communicate a return object from a plugin tool to the main Whitebox user-interface.
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
     * Used to communicate a progress update between a plugin tool and the main Whitebox user interface.
     * @param progressLabel A String to use for the progress label.
     * @param progress Float containing the progress value (between 0 and 100).
     */
    private void updateProgress(String progressLabel, int progress) {
        if (myHost != null && ((progress != previousProgress) || 
                (!progressLabel.equals(previousProgressLabel)))) {
            myHost.updateProgress(progressLabel, progress);
        }
        previousProgress = progress;
        previousProgressLabel = progressLabel;
    }
    /**
     * Used to communicate a progress update between a plugin tool and the main Whitebox user interface.
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
     * @param args 
     */
    @Override
    public void setArgs(String[] args) {
        this.args = args.clone();
    }
    
    private boolean cancelOp = false;
    /**
     * Used to communicate a cancel operation from the Whitebox GUI.
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
     * @return a boolean describing whether or not the plugin is actively being used.
     */
    @Override
    public boolean isActive() {
        return amIActive;
    }

    
    private class embossWork implements Runnable {
        int row;
        int col;
        int rows;
        int cols;
        double noData;
        int numPixelsInFilter;
        int[] dX;
        int[] dY;
        double[] weights;
        WhiteboxRaster inputFile;
        WhiteboxRaster outputFile;
 
        //constructor
        public embossWork(int _row, int _rows, int _cols, WhiteboxRaster _inputFile, WhiteboxRaster _outputFile, double _noData, int _numPixelsInFilter, int[] _dX, int[] _dY, double[] _weights) {
            row = _row;
            rows = _rows;
            cols = _cols;
            noData = _noData;
            numPixelsInFilter = _numPixelsInFilter;
            dX = _dX;
            dY = _dY;
            weights = _weights;
            inputFile = _inputFile;
            outputFile = _outputFile;
        }
        
        static final int chunkSize = 500;
        @Override
        public void run() {
            double centreValue, sum, z, progress;
            int a, x, y, i, j;
            
            int[][] savedRows = new int[chunkSize][cols];
            int[][] savedCols = new int[chunkSize][cols];
            double[][] savedSum = new double[chunkSize][cols];
            int savedNum = 0;
            
            for (row = row; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    //synchronized (inputFile) {
                        centreValue = inputFile.getValue(row, col);
                    //}
                    if (centreValue != noData) {
                        sum = 0;
                        for (a = 0; a < numPixelsInFilter; a++) {
                            x = col + dX[a];
                            y = row + dY[a];
                           // synchronized (inputFile) {
                                z = inputFile.getValue(y, x);
                            //}
                            if (z == noData) { z = centreValue; }
                            sum += z * weights[a];
                        }
                        savedRows[savedNum][col] = row;
                        savedCols[savedNum][col] = col;
                        savedSum[savedNum][col] = sum;

                    } else {
                       savedRows[savedNum][col] = row;
                       savedCols[savedNum][col] = col;
                       savedSum[savedNum][col] = noData;
                    }

                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                
                
                savedNum +=1;
                
                //Write chunks of rows at one to reduce lock contetention
                if(savedNum == chunkSize || row == rows-1){
                    synchronized (outputFile) {
                        for(i = 0; i < savedNum; i++){
                            for(j = 0; j < cols; j++){
                                outputFile.setValue(savedRows[i][j], savedCols[i][j], savedSum[i][j]);
                            }
                        }
                        completedRows += savedNum;
                        progress = (float) (100f * completedRows / (totalRows - 1));
                        updateProgress((int) progress);
                        
                    }
                    savedNum = 0;
                }
            }            
        }
    }
    
    @Override
    @SuppressWarnings("empty-statement")
    public void run() {
        amIActive = true;
        
        String inputHeader = null;
        String outputHeader = null;
        int row;
        int[] dX;
        int[] dY;
        double[] weights;
        int numPixelsInFilter;
        boolean reflectAtBorders = true;
        String direction = "n";
        
        //Create thread pool
        int threads = Parallel.getPluginProcessors();
        System.out.println("Number of threads: " + threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
    
        long start = System.nanoTime();
        
        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }
        
        for (int i = 0; i < args.length; i++) {
            if (i == 0) {
                inputHeader = args[i];
            } else if (i == 1) {
                outputHeader = args[i];
            } else if (i == 2) {
                direction = args[i].toLowerCase();
            }
        }

        // check to see that the inputHeader and outputHeader are not null.
        if ((inputHeader == null) || (outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            WhiteboxRaster[] inputFile = new WhiteboxRaster[threads];
            inputFile[0] = new WhiteboxRaster(inputHeader, "r");
            inputFile[0].isReflectedAtEdges = reflectAtBorders;

            int rows = inputFile[0].getNumberRows();
            int cols = inputFile[0].getNumberColumns();
            double noData = inputFile[0].getNoDataValue();

            WhiteboxRaster outputFile = new WhiteboxRaster(outputHeader, "rw", inputHeader, WhiteboxRaster.DataType.FLOAT, noData);
            outputFile.setPreferredPalette("grey.pal");
            
            if (direction.equals("n")) {
                weights = new double[]{0, -1, 0, 0, 0, 0, 0, 1, 0};
            } else if (direction.equals("s")) {
                weights = new double[]{0, 1, 0, 0, 0, 0, 0, -1, 0};
            } else if (direction.equals("e")) {
                weights = new double[]{0, 0, 0, 1, 0, -1, 0, 0, 0};
            } else if (direction.equals("w")) {
                weights = new double[]{0, 0, 0, -1, 0, 1, 0, 0, 0};
            } else if (direction.equals("ne")) {
                weights = new double[]{0, 0, -1, 0, 0, 0, 1, 0, 0};
            } else if (direction.equals("nw")) {
                weights = new double[]{-1, 0, 0, 0, 0, 0, 0, 0, 1};
            } else if (direction.equals("se")) {
                weights = new double[]{1, 0, 0, 0, 0, 0, 0, 0, -1};
            } else { // sw
                weights = new double[]{0, 0, 1, 0, 0, 0, -1, 0, 0};
            }
            
            dX = new int[]{-1, 0, 1, -1, 0, 1, -1, 0, 1};
            dY = new int[]{-1, -1, -1, 0, 0, 0, 1, 1, 1};
            
            numPixelsInFilter = dX.length;
            
            //calculate how many rows each thread does (just dividing by number)
            int rowBlockSize = rows/threads;
            
            //Set up variables for progress updating
            totalRows = rows;
            completedRows = 0;
            
            //Start all of the threads
            row = 0;
            for (int i = 0; i < threads-1; i++) {
                pool.execute(new embossWork(row, row+rowBlockSize, cols, inputFile[i], outputFile, noData, numPixelsInFilter, dX, dY, weights));
                row += rowBlockSize;
                inputFile[i+1] = new WhiteboxRaster(inputHeader, "r");
            }
            pool.execute(new embossWork(row, rows, cols, inputFile[threads-1],outputFile, noData, numPixelsInFilter, dX, dY, weights));

            //Wait for the threads to finish
            pool.shutdown();
            while(!pool.awaitTermination(1, TimeUnit.SECONDS));
                    
            outputFile.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
            outputFile.addMetadataEntry("Created on " + new Date());
            
            //Close files
            for (int i = 0; i < threads; i++) {
                inputFile[i].close();
            }
            outputFile.close();

            // returning a header file string displays the image.
            returnData(outputHeader);

        } catch (Exception e) {
            showFeedback(e.getMessage());
        } finally {
            updateProgress("Progress: ", 0);
            // tells the main application that this process is completed.
            amIActive = false;
            myHost.pluginComplete();
        }
        
        long end = System.nanoTime();
        System.out.println("Time Elapsed: " + (end-start)/1000000000.0);
        
        //Shutdown the executor service
        pool.shutdown();
    }
}