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
public class BurnStreams implements WhiteboxPlugin {
    
    private WhiteboxPluginHost myHost = null;
    private String[] args;
    WhiteboxRaster DEM;
    WhiteboxRaster streams;
    WhiteboxRaster output;
    String outputHeader = null;
    String streamsHeader = null;
    String inputHeader = null;
    int rows = 0;
    int cols = 0;
    double noData = -32768;
    double gridRes = 0;
        
    @Override
    public String getName() {
        return "BurnStreams";
    }

    @Override
    public String getDescriptiveName() {
    	return "Burn Streams";
    }

    @Override
    public String getToolDescription() {
    	return "Decrements the elevations in a DEM along a stream network.";
    }

    @Override
    public String[] getToolbox() {
    	String[] ret = { "DEMPreprocessing" };
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
        
        int row, col;
        int i;
        float progress = 0;
        double decrement = 0;
        boolean applyGradientTowardStreams = false;
        double decayCoefficient = 0;
        double elevation = 0;
        double infVal = 9999999;
    
        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }
        
        for (i = 0; i < args.length; i++) {
            if (i == 0) {
                inputHeader = args[i];
            } else if (i == 1) {
                streamsHeader = args[i];
            } else if (i == 2) {
                outputHeader = args[i];
            } else if (i == 3) {
                decrement = Double.parseDouble(args[i]);
            } else if (i ==4) {
                if (!args[i].toLowerCase().contains("not specified")) {
                    decayCoefficient = Double.parseDouble(args[i]);
                    if (decayCoefficient < 0) { decayCoefficient = 0; }
                }
            }
        }

        // check to see that the inputHeader and outputHeader are not null.
        if ((inputHeader == null) || (outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            DEM = new WhiteboxRaster(inputHeader, "r");
            rows = DEM.getNumberRows();
            cols = DEM.getNumberColumns();
            noData = DEM.getNoDataValue();
            gridRes = (DEM.getCellSizeX() + DEM.getCellSizeY()) / 2;

            streams = new WhiteboxRaster(streamsHeader, "r");
            
            if (streams.getNumberColumns() != cols || streams.getNumberRows() != rows) {
                showFeedback("The input files must have the same dimensions.");
                return;
            }
            
            output = new WhiteboxRaster(outputHeader, "rw", inputHeader, WhiteboxRaster.DataType.FLOAT, infVal);
            output.setPreferredPalette(DEM.getPreferredPalette());
           
            if (decayCoefficient > 0) {
                if (!CalculateDistance()) {
                    showFeedback("An error was encountered calculating distances.");
                    return;
                }
                double minVal = Float.MAX_VALUE;
                double maxVal = Float.MIN_VALUE;
                double distVal = 0;
                double[] data;
                for (row = 0; row < rows; row++) {
                    data = DEM.getRowValues(row);
                    for (col = 0; col < cols; col++) {
                        if (data[col] != noData) {
                            distVal = output.getValue(row, col);
                            elevation = data[col] - 
                                    (Math.pow((gridRes / (gridRes + distVal)), 
                                    decayCoefficient) * decrement);
                            if (elevation < minVal) { minVal = elevation; }
                            if (elevation > maxVal) { maxVal = elevation; }
                            output.setValue(row, col, elevation);
                        } else {
                            output.setValue(row, col, noData);
                        }
                    }
                    if (cancelOp) {
                        cancelOperation();
                        return;
                    }
                    progress = (float) (100f * row / (rows - 1));
                    updateProgress("Burning Streams:", (int) progress);
                }
                
            } else {
                double[] data;
                for (row = 0; row < rows; row++) {
                    data = DEM.getRowValues(row);
                    for (col = 0; col < cols; col++) {
                        if (data[col] != noData) {
                            elevation = data[col] - decrement;
                            output.setValue(row, col, elevation);
                        } else {
                            output.setValue(row, col, noData);
                        }
                    }
                    if (cancelOp) {
                        cancelOperation();
                        return;
                    }
                    progress = (float) (100f * row / (rows - 1));
                    updateProgress("Burning Streams:", (int) progress);
                }

            }
            
//            String ret = String.valueOf(output.getNumberOfDataFileReads()) + "\t" + 
//                    String.valueOf(output.getNumberOfDataFileWrites());
//            returnData(ret);
//            
            output.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
            output.addMetadataEntry("Created on " + new Date());
            
            DEM.close();
            output.close();
            
            // returning a header file string displays the DEM.
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
    
    private boolean CalculateDistance() {
        try {
            int row, col;
            float progress = 0;
            double z, z2, zMin;
            int x, y, a, b, i;
            double h = 0;
            int whichCell;
            double infVal = 9999999;
            int[] dX = new int[]{-1, -1, 0, 1, 1, 1, 0, -1};
            int[] dY = new int[]{0, -1, -1, -1, 0, 1, 1, 1};
            int[] Gx = new int[]{1, 1, 0, 1, 1, 1, 0, 1};
            int[] Gy = new int[]{0, 1, 1, 1, 0, 1, 1, 1};
            
            WhiteboxRaster Rx = new WhiteboxRaster(outputHeader.replace(".dep", "_temp1.dep"), "rw", inputHeader, WhiteboxRaster.DataType.FLOAT, 0);
            Rx.isTemporaryFile = true;
            WhiteboxRaster Ry = new WhiteboxRaster(outputHeader.replace(".dep", "_temp2.dep"), "rw", inputHeader, WhiteboxRaster.DataType.FLOAT, 0);
            Ry.isTemporaryFile = true;
            
            double[] data;
            for (row = 0; row < rows; row++) {
                data = streams.getRowValues(row);
                for (col = 0; col < cols; col++) {
                    if (data[col] !=0) { 
                        output.setValue(row, col, 0);
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return false;
                }
                progress = (float) (100f * row / (rows - 1));
                updateProgress("Calculating Distance From Streams:", (int) progress);
            }

            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    z = output.getValue(row, col);
                    if (z != 0) {
                        zMin = infVal;
                        whichCell = -1;
                        for (i = 0; i <= 3; i++) {
                            x = col + dX[i];
                            y = row + dY[i];
                            z2 = output.getValue(y, x);
                            if (z2 != noData) {
                                switch (i) {
                                    case 0:
                                        h = 2 * Rx.getValue(y, x) + 1;
                                        break;
                                    case 1:
                                        h = 2 * (Rx.getValue(y, x) + Ry.getValue(y, x) + 1);
                                        break;
                                    case 2:
                                        h = 2 * Ry.getValue(y, x) + 1;
                                        break;
                                    case 3:
                                        h = 2 * (Rx.getValue(y, x) + Ry.getValue(y, x) + 1);
                                        break;
                                }
                                z2 += h;
                                if (z2 < zMin) {
                                    zMin = z2;
                                    whichCell = i;
                                }
                            }
                        }
                        if (zMin < z) {
                            output.setValue(row, col, zMin);
                            x = col + dX[whichCell];
                            y = row + dY[whichCell];
                            Rx.setValue(row, col, Rx.getValue(y, x) + Gx[whichCell]);
                            Ry.setValue(row, col, Ry.getValue(y, x) + Gy[whichCell]);
                        }
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return false;
                }
                progress = (float) (100f * row / (rows - 1));
                updateProgress("Calculating Distance From Streams:", (int) progress);
            }

            
            for (row = rows - 1; row >= 0; row--) {
                for (col = cols - 1; col >= 0; col--) {
                    z = output.getValue(row, col);
                    if (z != 0) {
                        zMin = infVal;
                        whichCell = -1;
                        for (i = 4; i <= 7; i++) {
                            x = col + dX[i];
                            y = row + dY[i];
                            z2 = output.getValue(y, x);
                            if (z2 != noData) {
                                switch (i) {
                                    case 5:
                                        h = 2 * (Rx.getValue(y, x) + Ry.getValue(y, x) + 1);
                                        break;
                                    case 4:
                                        h = 2 * Rx.getValue(y, x) + 1;
                                        break;
                                    case 6:
                                        h = 2 * Ry.getValue(y, x) + 1;
                                        break;
                                    case 7:
                                        h = 2 * (Rx.getValue(y, x) + Ry.getValue(y, x) + 1);
                                        break;
                                }
                                z2 += h;
                                if (z2 < zMin) {
                                    zMin = z2;
                                    whichCell = i;
                                }
                            }
                        }
                        if (zMin < z) {
                            output.setValue(row, col, zMin);
                            x = col + dX[whichCell];
                            y = row + dY[whichCell];
                            Rx.setValue(row, col, Rx.getValue(y, x) + Gx[whichCell]);
                            Ry.setValue(row, col, Ry.getValue(y, x) + Gy[whichCell]);
                        }
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return false;
                }
                progress = (float) (100f * (rows - 1 - row) / (rows - 1));
                updateProgress("Calculating Distance From Streams:", (int) progress);
            }
            
            for (row = 0; row < rows; row++) {
                for (col = 0; col < cols; col++) {
                    z = streams.getValue(row, col);
                    if (z != noData) {
                        z = output.getValue(row, col);
                        output.setValue(row, col, Math.sqrt(z) * gridRes);
                    } else {
                        output.setValue(row, col, noData);
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return false;
                }
                progress = (float) (100f * row / (rows - 1));
                updateProgress("Calculating Distance From Streams:", (int) progress);
            }
            
            streams.close();
            Rx.close();
            Ry.close();
            
            
            return true;
        } catch (Exception e) {
            return false;
        }
        
    }
}