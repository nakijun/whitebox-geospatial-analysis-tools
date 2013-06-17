/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plugins;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import javax.swing.JOptionPane;
import whitebox.interfaces.WhiteboxPlugin;
import whitebox.interfaces.WhiteboxPluginHost;

/**
 * WhiteboxPlugin is used to define a plugin tool for Whitebox GIS.
 * @author Dr. Ehsan Roshani <eroshani@uoguelph.ca>
 */

public class ImportPolSAR implements WhiteboxPlugin {

    private WhiteboxPluginHost myHost = null;
    private String[] args;
    /**
     * Used to retrieve the plugin tool's name. This is a short, unique name containing no spaces.
     * @return String containing plugin name.
     */
    @Override
    public String getName() {
        return "ImportPolSAR";
    }
    /**
     * Used to retrieve the plugin tool's descriptive name. This can be a longer name (containing spaces) and is used in the interface to list the tool.
     * @return String containing the plugin descriptive name.
     */
    @Override
    public String getDescriptiveName() {
        return "Import UAVPolSAR Data";
    }
    /**
     * Used to retrieve a short description of what the plugin tool does.
     * @return String containing the plugin's description.
     */
    @Override
    public String getToolDescription() {
        return "Imports a UAVPolSAR data and covert the to dep files.";
    }
    /**
     * Used to identify which toolboxes this plugin tool should be listed in.
     * @return Array of Strings.
     */
    @Override
    public String[] getToolbox() {
        String[] ret = {"IOTools"};
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
        showFeedback("Operation cancelled.");
        updateProgress("Progress: ", 0);
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

    @Override
    public void run() {
        FileWriter fw = null;
        try {
            amIActive = true;
            String inputFilesString = null;
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
            try {
                
                PolSARData p = new PolSARData(args[0]);
                //updateProgress("Reading Header File (Done): ", 2);
                int numberOfFiles =0;
                for (int f = 2; f < 11; f++) {
                    if (args[f]=="true") {
                        numberOfFiles++;
                    }
                }
                if ((numberOfFiles == 0)) {
                    showFeedback("Select at least on input file.");
                    return;
                }                

                int progressIncr = 100/numberOfFiles;
                int currProgress = 0;
                if (args[1].equals("Calibrated Ground projected files (.grd)")) {
    
                    if (args[2]== "true") {
                        updateProgress("Converting HHHH data: ", currProgress );
                        p.ConvertToDep(p.grd_mag_row_addr, p.grd_mag_col_addr,
                                Math.abs(p.grd_mag_row_mult), Math.abs(p.grd_mag_col_mult),
                                p.grd_mag_set_rows, p.grd_mag_set_cols,
                                PolSARData.UAVSARFile.grdHHHH,false);
                    }
                    
                    if (args[3]== "true") {
                        currProgress += progressIncr;
                        updateProgress("Converting HVHV data: ", currProgress );
                        p.ConvertToDep(p.grd_mag_row_addr, p.grd_mag_col_addr,
                                Math.abs(p.grd_mag_row_mult), Math.abs(p.grd_mag_col_mult),
                                p.grd_mag_set_rows, p.grd_mag_set_cols,
                                PolSARData.UAVSARFile.grdHVHV,false);
                    }
                    if (args[4]== "true") {
                        currProgress += progressIncr;
                        updateProgress("Converting VVVV data: ", currProgress );
                        p.ConvertToDep(p.grd_mag_row_addr, p.grd_mag_col_addr,
                                Math.abs(p.grd_mag_row_mult), Math.abs(p.grd_mag_col_mult),
                                p.grd_mag_set_rows, p.grd_mag_set_cols,
                                PolSARData.UAVSARFile.grdVVVV,false);
                    }
                    if (args[5]== "true") {
                        currProgress += progressIncr;
                        updateProgress("Converting HHHV data: ", currProgress );
                        p.ConvertToDep(p.grd_mag_row_addr, p.grd_mag_col_addr,
                                Math.abs(p.grd_mag_row_mult), Math.abs(p.grd_mag_col_mult),
                                p.grd_mag_set_rows, p.grd_mag_set_cols,
                                PolSARData.UAVSARFile.grdHHHV,false);
                    }
                    if (args[6]== "true") {
                        currProgress += progressIncr;
                        updateProgress("Converting HHHV data: ", currProgress );
                        p.ConvertToDep(p.grd_mag_row_addr, p.grd_mag_col_addr,
                                Math.abs(p.grd_mag_row_mult), Math.abs(p.grd_mag_col_mult),
                                p.grd_mag_set_rows, p.grd_mag_set_cols,
                                PolSARData.UAVSARFile.grdHHHV,true);
                    }
                    
                    if (args[7]== "true") {
                        currProgress += progressIncr;
                        updateProgress("Converting HHVV data: ", currProgress );
                        p.ConvertToDep(p.grd_mag_row_addr, p.grd_mag_col_addr,
                                Math.abs(p.grd_mag_row_mult), Math.abs(p.grd_mag_col_mult),
                                p.grd_mag_set_rows, p.grd_mag_set_cols,
                                PolSARData.UAVSARFile.grdHHVV,false);
                    }
                    if (args[8]== "true") {
                        currProgress += progressIncr;
                        updateProgress("Converting HHVV data: ", currProgress );
                        p.ConvertToDep(p.grd_mag_row_addr, p.grd_mag_col_addr,
                                Math.abs(p.grd_mag_row_mult), Math.abs(p.grd_mag_col_mult),
                                p.grd_mag_set_rows, p.grd_mag_set_cols,
                                PolSARData.UAVSARFile.grdHHVV,true);
                    }
                    
                    if (args[9]== "true") {
                        currProgress += progressIncr;
                        updateProgress("Converting HVVV data: ", currProgress );
                        p.ConvertToDep(p.grd_mag_row_addr, p.grd_mag_col_addr,
                                Math.abs(p.grd_mag_row_mult), Math.abs(p.grd_mag_col_mult),
                                p.grd_mag_set_rows, p.grd_mag_set_cols,
                                PolSARData.UAVSARFile.grdHVVV,false);
                    }
                    if (args[10]== "true") {
                        currProgress += progressIncr;
                        updateProgress("Converting HVVV data: ", currProgress );
                        p.ConvertToDep(p.grd_mag_row_addr, p.grd_mag_col_addr,
                                Math.abs(p.grd_mag_row_mult), Math.abs(p.grd_mag_col_mult),
                                p.grd_mag_set_rows, p.grd_mag_set_cols,
                                PolSARData.UAVSARFile.grdHVVV,true);
                    }
                }
                
                if (args[1].equals("MLC files (.mlc): calibrated multi-looked cross products")) {
    
                    if (args[2]== "true") {
                        updateProgress("Converting HHHH data: ", currProgress );
                        p.ConvertToDep(p.mlc_mag_row_addr, p.mlc_mag_col_addr,
                                Math.abs(p.mlc_mag_row_mult), Math.abs(p.mlc_mag_col_mult),
                                p.mlc_mag_set_rows, p.mlc_mag_set_cols,
                                PolSARData.UAVSARFile.mlcHHHH,false);
                    }
                    
                    if (args[3]== "true") {
                        currProgress += progressIncr;
                        updateProgress("Converting HVHV data: ", currProgress );
                        p.ConvertToDep(p.mlc_mag_row_addr, p.mlc_mag_col_addr,
                                Math.abs(p.mlc_mag_row_mult), Math.abs(p.mlc_mag_col_mult),
                                p.mlc_mag_set_rows, p.mlc_mag_set_cols,
                                PolSARData.UAVSARFile.mlcHVHV,false);
                    }
                    if (args[4]== "true") {
                        currProgress += progressIncr;
                        updateProgress("Converting VVVV data: ", currProgress );
                        p.ConvertToDep(p.mlc_mag_row_addr, p.mlc_mag_col_addr,
                                Math.abs(p.mlc_mag_row_mult), Math.abs(p.mlc_mag_col_mult),
                                p.mlc_mag_set_rows, p.mlc_mag_set_cols,
                                PolSARData.UAVSARFile.mlcVVVV,false);
                    }
                    if (args[5]== "true") {
                        currProgress += progressIncr;
                        updateProgress("Converting HHHV data: ", currProgress );
                        p.ConvertToDep(p.mlc_mag_row_addr, p.mlc_mag_col_addr,
                                Math.abs(p.mlc_mag_row_mult), Math.abs(p.mlc_mag_col_mult),
                                p.mlc_mag_set_rows, p.mlc_mag_set_cols,
                                PolSARData.UAVSARFile.mlcHHHV,false);
                    }
                    if (args[6]== "true") {
                        currProgress += progressIncr;
                        updateProgress("Converting HHHV data: ", currProgress );
                        p.ConvertToDep(p.mlc_mag_row_addr, p.mlc_mag_col_addr,
                                Math.abs(p.mlc_mag_row_mult), Math.abs(p.mlc_mag_col_mult),
                                p.mlc_mag_set_rows, p.mlc_mag_set_cols,
                                PolSARData.UAVSARFile.mlcHHHV,true);
                    }
                    
                    if (args[7]== "true") {
                        currProgress += progressIncr;
                        updateProgress("Converting HHVV data: ", currProgress );
                        p.ConvertToDep(p.mlc_mag_row_addr, p.mlc_mag_col_addr,
                                Math.abs(p.mlc_mag_row_mult), Math.abs(p.mlc_mag_col_mult),
                                p.mlc_mag_set_rows, p.mlc_mag_set_cols,
                                PolSARData.UAVSARFile.mlcHHVV,false);
                    }
                    if (args[8]== "true") {
                        currProgress += progressIncr;
                        updateProgress("Converting HHVV data: ", currProgress );
                        p.ConvertToDep(p.mlc_mag_row_addr, p.mlc_mag_col_addr,
                                Math.abs(p.mlc_mag_row_mult), Math.abs(p.mlc_mag_col_mult),
                                p.mlc_mag_set_rows, p.mlc_mag_set_cols,
                                PolSARData.UAVSARFile.mlcHHVV,true);
                    }
                    
                    if (args[9]== "true") {
                        currProgress += progressIncr;
                        updateProgress("Converting HVVV data: ", currProgress );
                        p.ConvertToDep(p.mlc_mag_row_addr, p.mlc_mag_col_addr,
                                Math.abs(p.mlc_mag_row_mult), Math.abs(p.mlc_mag_col_mult),
                                p.mlc_mag_set_rows, p.mlc_mag_set_cols,
                                PolSARData.UAVSARFile.mlcHVVV,false);
                    }
                    if (args[10]== "true") {
                        currProgress += progressIncr;
                        updateProgress("Converting HVVV data: ", currProgress );
                        p.ConvertToDep(p.mlc_mag_row_addr, p.mlc_mag_col_addr,
                                Math.abs(p.mlc_mag_row_mult), Math.abs(p.mlc_mag_col_mult),
                                p.mlc_mag_set_rows, p.mlc_mag_set_cols,
                                PolSARData.UAVSARFile.mlcHVVV,true);
                    }
                }

                if (numberOfFiles ==1) {
                    returnData(p.OutputFile);
                }
                else{
                    JOptionPane.showMessageDialog(null, "Conversion is Done", "WhiteBox",1);
                }
                
            } catch (OutOfMemoryError oe) {
                //showFeedback("The Java Virtual Machine (JVM) is out of memory");
            } catch (Exception e) {
                showFeedback(e.getMessage());
            } finally {
                updateProgress("Progress: ", 0);
            }
            // tells the main application that this process is completed.
            amIActive = false;
            myHost.pluginComplete();
        } finally {
            try {
                fw.close();
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(ImportPolSAR.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    // this is only used for debugging the tool
//    public static void main(String[] args) {
//        args = new String[11];
//        //args[0] = "C:\\PDF\\Radar\\winnip_31606_12061_006_120717_L090_CX_01_grd\\winnip_31606_12061_006_120717_L090_CX_01.ann";
//        args[0] = "C:\\PDF\\Radar\\winnip_31606_12061_006_120717_L090_CX_01_mlc\\winnip_31606_12061_006_120717_L090_CX_01.ann";
//        args[1] = "MLC files (.mlc): calibrated multi-looked cross products";
//        args[2] = "true";
//        args[3] = "false";
//        args[4] = "false";
//        args[5] = "true";
//        args[6] = "true";
//        args[7] = "true";
//        args[8] = "true";
//        args[9] = "false";
//        args[10] = "false";
//
//        ImportPolSAR ipolsar = new ImportPolSAR();
//        ipolsar.setArgs(args);
//        ipolsar.run();
//        
//    }
}
