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
import java.util.List;
import whitebox.geospatialfiles.WhiteboxRaster;
import whitebox.geospatialfiles.LASReader;
import whitebox.geospatialfiles.LASReader.PointRecord;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.interfaces.WhiteboxPlugin;
import whitebox.structures.KdTree;
import java.io.*;

/**
 *
 * @author Ehsan Roshani, Ph.D.
    Department of Geography 
    University of Guelph
    Guelph, Ont. N1G 2W1 CANADA
    Phone: (519) 824-4120 x53527
    Email: eroshani@uoguelph.ca
 */

public class Tree_Density implements WhiteboxPlugin {

    private WhiteboxPluginHost myHost = null;
    private String[] args;
    
    /**
     * Used to retrieve the plugin tool's name. This is a short, unique name
     * containing no spaces.
     *
     * @return String containing plugin name.
     */
    @Override
    public String getName() {
        return "Tree_Density";
    }

    /**
     * Used to retrieve the plugin tool's descriptive name. This can be a longer
     * name (containing spaces) and is used in the interface to list the tool.
     *
     * @return String containing the plugin descriptive name.
     */
    @Override
    public String getDescriptiveName() {
        return "Tree Density (LiDAR)";
    }

    /**
     * Used to retrieve a short description of what the plugin tool does.
     *
     * @return String containing the plugin's description.
     */
    @Override
    public String getToolDescription() {
        return "Finds the tree location, crown diameter, and hieght then calculates the spatial pattern of tree density from a LiDAR data set.";
    }
    
    /**
     * Used to identify which toolboxes this plugin tool should be listed in.
     *
     * @return Array of Strings.
     */
    @Override
    public String[] getToolbox() {
        String[] ret = {"LidarTools"};
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
        showFeedback("Operation cancelled.");
        updateProgress("Progress: ", 0);
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
        /* The problem with this algorithm is that the implementation of the
         * k-d tree used here only allows you to return the n-closest neighbours
         * and does not allow you to search for points that are within a specified
         * distance. This will introduce an inaccuracy in point density estimates.
         */
        
        amIActive = true;

        String inputFilesString = null;
        // get the arguments
        if (args.length <= 0) {
            showFeedback("Plugin parameters have not been set.");
            return;
        }
        inputFilesString = args[0];
        
        TreeFinder tf = new TreeFinder(args[0],args[0].replace(".dep", args[1] + ".shp"));
        tf.setThreshold(Double.parseDouble(args[2]));
        tf.setA(Double.parseDouble(args[3]));
        tf.setB(Double.parseDouble(args[4]));
        tf.setC(Double.parseDouble(args[5]));
        tf.setMaxHeight(Double.parseDouble(args[6]));
        tf.setOutputCellSize(Double.parseDouble(args[7]));
        tf.setSearchRadius(Double.parseDouble(args[8]));
        
        updateProgress("Preparing required data ...",5);
        tf.Search();
        updateProgress("Shape file is done! Building the tree density raster ... ",50);
        
        tf.Density();
        
        returnData(tf.getOutputfile());
        
        updateProgress("Tree density raster is done!",100);
        
        
        
        // check to see that the inputHeader and outputHeader are not null.
        if ((inputFilesString.length() <= 0)) {
            showFeedback("One or more of the input parameters have not been set properly.");
            return;
        }

    }
      
//    // this is only used for debugging the tool
    public static void main(String[] args) {
        //LiDAR_PointDensity nn = new LiDAR_PointDensity();
        Tree_Density nn = new Tree_Density();
        args = new String[10];
        args[0] = "C:\\PDF\\John Example\\Data\\Data\\out1.dep";
        //args[0] = "/Users/johnlindsay/Documents/Data/u_5565073250.las";
        args[1] = "T";
        args[2] = "5";
        args[3] = "2.51503";
        args[4] = "0";
        args[5] = "0.00901";
        args[6] = "50";
        args[7] = "1";
        args[8] = "10";
//        args[2] = "intensity";
//        args[3] = "last return";
//        args[4] = "4";
//        args[5] = "1";
        nn.setArgs(args);
        nn.run();
        
    }
}