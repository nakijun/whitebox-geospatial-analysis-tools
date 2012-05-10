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
import java.text.DecimalFormat;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.interfaces.WhiteboxPlugin;
import whitebox.interfaces.WhiteboxPluginHost;
/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class ElongationRatio implements WhiteboxPlugin {

    private WhiteboxPluginHost myHost;
    private String[] args;

    public String getName() {
        return "ElongationRatio";
    }

    public String getDescriptiveName() {
    	return "Elongation Ratio";
    }

    public String getToolDescription() {
    	return "The ratio between the difference "
                + "in the long and short axis of the minimum bounding box for "
                + "each polygon in a raster image to the sum of the long and short axis.";
    }

    public String[] getToolbox() {
    	String[] ret = { "PatchShapeTools" };
    	return ret;
    }

    
    public void setPluginHost(WhiteboxPluginHost host) {
        myHost = host;
    }

    private void showFeedback(String feedback) {
        if (myHost != null) {
            myHost.showFeedback(feedback);
        } else {
            System.out.println(feedback);
        }
    }

    private void returnData(Object ret) {
        if (myHost != null) {
            myHost.returnData(ret);
        }
    }

    private void updateProgress(String progressLabel, int progress) {
        if (myHost != null) {
            myHost.updateProgress(progressLabel, progress);
        } else {
            System.out.println(progressLabel + " " + progress + "%");
        }
    }

    private void updateProgress(int progress) {
        if (myHost != null) {
            myHost.updateProgress(progress);
        } else {
            System.out.print("Progress: " + progress + "%");
        }
    }
    
    public void setArgs(String[] args) {
        this.args = args.clone();
    }
    
    private boolean cancelOp = false;
    public void setCancelOp(boolean cancel) {
        cancelOp = cancel;
    }
    
    private void cancelOperation() {
        showFeedback("Operation cancelled.");
        updateProgress("Progress: ", 0);
    }
    
    private boolean amIActive = false;
    public boolean isActive() {
        return amIActive;
    }
    
    public void run() {
        amIActive = true;

        String inputHeader = null;
        String outputHeader = null;
        int col, row, numCols, numRows, i;
        int[] dX = {1, 1, 1, 0, -1, -1, -1, 0};
        int[] dY = {-1, 0, 1, 1, 1, 0, -1, -1};
        int a;
        float progress;
        int minValue, maxValue, range;
        boolean blnTextOutput = false;
        boolean zeroAsBackground = false;
        double gridRes;
        int z;
        int cN, rN;
        float zN;
        int n;
        int m;

        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }

        for (i = 0; i < args.length; i++) {
            if (i == 0) {
                inputHeader = args[i];
            } else if (i == 1) {
                outputHeader = args[i];
            } else if (i == 2) {
                blnTextOutput = Boolean.parseBoolean(args[i]);
            } else if (i == 3) {
                zeroAsBackground = Boolean.parseBoolean(args[i]);
            }
        }

        // check to see that the inputHeader and outputHeader are not null.
        if ((inputHeader == null) || (outputHeader == null)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

        try {
            WhiteboxRaster image = new WhiteboxRaster(inputHeader, "r");

            numRows = image.getNumberRows();
            numCols = image.getNumberColumns();
            double noData = image.getNoDataValue();
            gridRes = (image.getCellSizeX() + image.getCellSizeY()) / 2;
            
            WhiteboxRaster output = new WhiteboxRaster(outputHeader, "rw", inputHeader, WhiteboxRaster.DataType.FLOAT, noData);
            output.setPreferredPalette("spectrum.pal");
            output.setDataScale(WhiteboxRaster.DataScale.CONTINUOUS);

            minValue = (int) (image.getMinimumValue());
            maxValue = (int) (image.getMaximumValue());
            range = maxValue - minValue;

            updateProgress("Finding minimum bounding boxes:", 0);
            // find the axis-aligned minimum bounding box and the number of edge cells.
            double[][] boundingBox = new double[4][range + 1];
            int[] numEdgeCells = new int[range + 1];
            long[] area = new long[range + 1];
            double[] elongation = new double[range + 1];

            // initialize the boundingbox
            for (a = 0; a <= range; a++) {
                boundingBox[0][a] = Integer.MAX_VALUE; // west
                boundingBox[1][a] = Integer.MIN_VALUE; // east
                boundingBox[2][a] = Integer.MAX_VALUE; // north
                boundingBox[3][a] = Integer.MIN_VALUE; // south
            }

            // now fill it with the cartesian-aligned minimum bounding box
            for (row = 0; row < numRows; row++) {
                for (col = 0; col < numCols; col++) {
                    z = (int) (image.getValue(row, col));
                    if (z != noData) {
                        a = z - minValue;
                        if (col < boundingBox[0][a]) {
                            boundingBox[0][a] = col;
                        }
                        if (col > boundingBox[1][a]) {
                            boundingBox[1][a] = col;
                        }
                        if (row < boundingBox[2][a]) {
                            boundingBox[2][a] = row;
                        }
                        if (row > boundingBox[3][a]) {
                            boundingBox[3][a] = row;
                        }
                        area[a]++;

                        // scan each neighbour to see it is an edge cells.
                        for (i = 0; i < 8; i++) {
                            cN = col + dX[i];
                            rN = row + dY[i];
                            zN = (int) (image.getValue(rN, cN));
                            if (zN != z) {
                                numEdgeCells[a]++;
                                break;
                            }
                        }
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (float) (100f * row / (numRows - 1));
                updateProgress("Finding minimum bounding boxes:", (int) progress);
            }

            double R = 0;
            double Psi = 0;
            double Theta = 0;
            double DegreeToRad = Math.PI / 180;
            double x, y;
            double[] newBoundingBox = new double[4];
            int[][] edgeCells = null;
            double[][] edgeCellsRotated = null;
            double[][] axes = new double[2][range + 1];
            double newXAxis = 0;
            double newYAxis = 0;
            double longAxis;
            double shortAxis;
            int startingPatch = 0;
            if (zeroAsBackground) {
                startingPatch = 1;
            }
            for (a = startingPatch; a <= range; a++) {
                if (area[a] > 1) {
                    axes[0][a] = boundingBox[1][a] - boundingBox[0][a] + 1;
                    axes[1][a] = boundingBox[3][a] - boundingBox[2][a] + 1;

                    // read the edge cells into the edgeCells array.
                    edgeCells = new int[2][numEdgeCells[a]];
                    edgeCellsRotated = new double[2][numEdgeCells[a]];
                    n = 0;
                    for (row = (int) boundingBox[2][a]; row <= (int) boundingBox[3][a]; row++) {
                        for (col = (int) boundingBox[0][a]; col <= (int) boundingBox[1][a]; col++) {
                            z = (int) (image.getValue(row, col));
                            if ((z - minValue) == a) {
                                // scan each neighbour to see it is an edge cells.
                                for (i = 0; i < 8; i++) {
                                    cN = col + dX[i];
                                    rN = row + dY[i];
                                    zN = (int) (image.getValue(rN, cN));
                                    if (zN != z) {
                                        edgeCells[0][n] = col;
                                        edgeCells[1][n] = row;
                                        n++;
                                        break;
                                    }
                                }
                            }

                        }
                    }

                    // Rotate the edge cells in 1 degree increments.
                    for (m = 1; m < 180; m++) {
                        Psi = m * 0.5 * DegreeToRad;
                        // Rotate each edge cell in the array by m degrees.
                        for (n = 0; n < numEdgeCells[a]; n++) {
                            x = edgeCells[0][n];
                            y = edgeCells[1][n];
                            R = Math.sqrt(x * x + y * y);
                            Theta = Math.atan2(y, x);
                            x = R * Math.cos(Theta + Psi);
                            y = R * Math.sin(Theta + Psi);
                            edgeCellsRotated[0][n] = x;
                            edgeCellsRotated[1][n] = y;
                        }
                        // calcualte the minimum bounding box in this coordinate 
                        // system and see if it is less
                        newBoundingBox[0] = Double.MAX_VALUE; // west
                        newBoundingBox[1] = Double.MIN_VALUE; // east
                        newBoundingBox[2] = Double.MAX_VALUE; // north
                        newBoundingBox[3] = Double.MIN_VALUE; // south

                        for (n = 0; n < numEdgeCells[a]; n++) {
                            x = edgeCellsRotated[0][n];
                            y = edgeCellsRotated[1][n];
                            if (x < newBoundingBox[0]) {
                                newBoundingBox[0] = x;
                            }
                            if (x > newBoundingBox[1]) {
                                newBoundingBox[1] = x;
                            }
                            if (y < newBoundingBox[2]) {
                                newBoundingBox[2] = y;
                            }
                            if (y > newBoundingBox[3]) {
                                newBoundingBox[3] = y;
                            }

                        }
                        newXAxis = newBoundingBox[1] - newBoundingBox[0] + 1;
                        newYAxis = newBoundingBox[3] - newBoundingBox[2] + 1;

                        if ((axes[0][a] * axes[1][a]) > (newXAxis * newYAxis)) {

                            axes[0][a] = newXAxis;
                            axes[1][a] = newYAxis;
                        }
                    }
                    longAxis = Math.max(axes[0][a], axes[1][a]);
                    shortAxis = Math.min(axes[0][a], axes[1][a]);
                    elongation[a] = (double) ((longAxis - shortAxis) / (longAxis + shortAxis));
                } else {
                    elongation[a] = 1;
                }

                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (float) (a * 100f / range);
                updateProgress("Finding minimum bounding boxes:", (int) progress);
            }
            
            if (zeroAsBackground) {
                elongation[0] = noData;
                area[0] = (long) noData;
            }

            for (row = 0; row < numRows; row++) {
                for (col = 0; col < numCols; col++) {
                    z = (int) (image.getValue(row, col));
                    if (z != noData) {
                        a = z - minValue;
                        output.setValue(row, col, elongation[a]);
                    } else {
                        output.setValue(row, col, noData);
                    }
                }
                if (cancelOp) {
                    cancelOperation();
                    return;
                }
                progress = (float) (100f * row / (numRows - 1));
                updateProgress("Finding minimum bounding boxes:", (int) progress);
            }

            output.addMetadataEntry("Created by the "
                    + getDescriptiveName() + " tool.");
            output.addMetadataEntry("Created on " + new Date());

            image.close();
            output.close();

            if (blnTextOutput) {
                DecimalFormat df;
                df = new DecimalFormat("0.0000");

                String retstr = "Elongation Ratio\nPatch ID\tValue";

                for (a = 0; a <= range; a++) {
                    if (area[a] > 0) {
                        retstr = retstr + "\n" + (a + minValue) + "\t"
                                + df.format(elongation[a]);
                    }
                }

                returnData(retstr);
            }
            
            // returning a header file string displays the image.
            returnData(outputHeader);
            
            
        } catch (Exception e) {
            showFeedback(e.getMessage());
            showFeedback(e.getCause().toString());
        } finally {
            updateProgress("Progress: ", 0);
            // tells the main application that this process is completed.
            amIActive = false;
            myHost.pluginComplete();
        }
    }
}