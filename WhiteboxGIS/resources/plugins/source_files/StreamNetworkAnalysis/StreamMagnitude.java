/*
 * Copyright (C) 2011 Dr. John Lindsay <jlindsay@uoguelph.ca>
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
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.interfaces.WhiteboxPlugin;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class StreamMagnitude implements WhiteboxPlugin {
    
    private WhiteboxPluginHost myHost = null;
    private String[] args;
    
    // Constants
    private static final double LnOf2 = 0.693147180559945;

    @Override
    public String getName() {
        return "StreamMagnitude";
    }

    @Override
    public String getDescriptiveName() {
    	return "Shreve Stream Magnitude";
    }

    @Override
    public String getToolDescription() {
    	return "Assigns the Shreve stream magnitude to each link in a stream network.";
    }

    @Override
    public String[] getToolbox() {
    	String[] ret = { "StreamAnalysis" };
    	return ret;
    }

    @Override
    public void setPluginHost(WhiteboxPluginHost host) {
        myHost = host;
    }

    private void showFeedback(String message) {
        if (myHost != null) {
            myHost.showFeedback(message);
        } else {
            System.out.println(message);
        }
    }

    private void returnData(Object ret) {
        if (myHost != null) {
            myHost.returnData(ret);
        }
    }

    private int previousProgress = 0;
    private String previousProgressLabel = "";
    private void updateProgress(String progressLabel, int progress) {
        if (myHost != null && ((progress != previousProgress) || 
                (!progressLabel.equals(previousProgressLabel)))) {
            myHost.updateProgress(progressLabel, progress);
        }
        previousProgress = progress;
        previousProgressLabel = progressLabel;
    }

    private void updateProgress(int progress) {
        if (myHost != null && progress != previousProgress) {
            myHost.updateProgress(progress);
        }
        previousProgress = progress;
    }
    
    @Override
    public void setArgs(String[] args) {
        this.args = args.clone();
    }
    
    private boolean cancelOp = false;
    @Override
    public void setCancelOp(boolean cancel) {
        cancelOp = cancel;
    }
    
    private void cancelOperation() {
        showFeedback("Operation cancelled.");
        updateProgress("Progress: ", 0);
    }
    
    private boolean amIActive = false;
    @Override
    public boolean isActive() {
        return amIActive;
    }

    @Override
    public void run() {
        amIActive = true;
        
        String streamsHeader = null;
        String pointerHeader = null;
        String outputHeader = null;
        int row, col, x, y;
        float progress = 0;
        double z;
        int i, c;
        int[] dX = new int[]{1, 1, 1, 0, -1, -1, -1, 0};
        int[] dY = new int[]{-1, 0, 1, 1, 1, 0, -1, -1};
        double[] inflowingVals = new double[]{16, 32, 64, 128, 1, 2, 4, 8};
        boolean flag = false;
        double flowDir = 0;
                
        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }
        
        for (i = 0; i < args.length; i++) {
            if (i == 0) {
                streamsHeader = args[i];
            } else if (i == 1) {
                pointerHeader = args[i];
            } else if (i == 2) {
                outputHeader = args[i];
            }
        }

        // check to see that the inputHeader and outputHeader are not null.
        if ((streamsHeader == null) || (pointerHeader == null) || (outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            WhiteboxRaster streams = new WhiteboxRaster(streamsHeader, "r");
            int rows = streams.getNumberRows();
            int cols = streams.getNumberColumns();
            double noData = streams.getNoDataValue();
            
            WhiteboxRaster pntr = new WhiteboxRaster(pointerHeader, "r");
            
            if (pntr.getNumberRows() != rows || pntr.getNumberColumns() != cols) {
                showFeedback("The input images must be of the same dimensions.");
                return;
            }
            
            WhiteboxRaster output = new WhiteboxRaster(outputHeader, "rw", 
                    streamsHeader, WhiteboxRaster.DataType.INTEGER, 0);
            output.setPreferredPalette("spectrum.pal");
            output.setDataScale(WhiteboxRaster.DataScale.CONTINUOUS);
            
            WhiteboxRaster numInflowingStreamCells = new WhiteboxRaster(outputHeader.replace(".dep", "_temp.dep"), "rw", 
                    streamsHeader, WhiteboxRaster.DataType.INTEGER, noData);
            numInflowingStreamCells.isTemporaryFile = true;
            
            byte numNeighbouringStreamCells = 0;
            double currentValue = 0;
            
            updateProgress("Loop 1 of 2:", 0);
            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    if (streams.getValue(row, col) > 0) {
                        // count the number of neighbouring stream cells
                        numNeighbouringStreamCells = 0;
                        for (c = 0; c < 8; c++) {
                            x = col + dX[c];
                            y = row + dY[c];
                            if (streams.getValue(y, x) > 0 && 
                                    pntr.getValue(y, x) == inflowingVals[c]) { 
                                numNeighbouringStreamCells++; 
                            }
                        }
                        
                        if (numNeighbouringStreamCells == 0) { // it's a headwater
                            output.setValue(row, col, 1);
                            numInflowingStreamCells.setValue(row, col, -1);
                        } else {
                            output.setValue(row, col, 0); // it's not a headwater
                            numInflowingStreamCells.setValue(row, col, numNeighbouringStreamCells);
                        }
                    } else {
                        output.setValue(row, col, noData);
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (float) (100f * row / (rows - 1));
                updateProgress("Loop 1 of 2:", (int) progress);
            }
            
            updateProgress("Loop 2 of 2:", 0);
            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    if (numInflowingStreamCells.getValue(row, col) == -1) {
                        //it's a headwater location so start a downstream flowpath
                        x = col;
                        y = row;
                        flag = true;
                        do {
                            z = output.getValue(y, x);
                            //find the downslope neighbour
                            flowDir = pntr.getValue(y, x);
                            if (flowDir > 0) {
                                c = (int) (Math.log(flowDir) / LnOf2);
                                if (c > 7) {
                                    showFeedback("An unexpected value has "
                                            + "been identified in the pointer "
                                            + "image. This tool requires a "
                                            + "pointer grid that has been "
                                            + "created using either the D8 "
                                            + "or Rho8 tools.");
                                    return;
                                }
                                x += dX[c];
                                y += dY[c];

                                if (streams.getValue(y, x) <= 0) { //it's not a stream cell
                                    flag = false;
                                } else {
                                    //update the numInflowingStreamCells grid
                                    currentValue = numInflowingStreamCells.getValue(y, x) - 1;
                                    numInflowingStreamCells.setValue(y, x, currentValue);
                                    if (currentValue > 0) { 
                                        flag = false; 
                                    }
                                    
                                    //update the output grid
                                    currentValue = output.getValue(y, x);
                                    output.setValue(y, x, z + currentValue);
                                }

                            } else {
                                flag = false;
                            }
                        } while (flag);
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (float) (100f * row / (rows - 1));
                updateProgress("Loop 2 of 2:", (int) progress);
            }

            output.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
            output.addMetadataEntry("Created on " + new Date());
            
            pntr.close();
            streams.close();
            output.close();
            numInflowingStreamCells.close();

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
    }
}