/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package whiteboxgis;

import java.awt.Toolkit;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import whitebox.interfaces.WhiteboxPlugin;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.parallel.Parallel;

/**
 *
 * @author Bill Gardner <wgardner@socs.uoguelph.ca>
 */
public class TimingProfiler extends javax.swing.JFrame {
    
    WhiteboxPluginHost host;    // initialized by constructor
    
    // This describes the tool currently being timed
    WhiteboxPlugin plugin;
    String[] pluginArgs;
    long pluginStart = 0;   // nanosecs
    
    List<Integer> runAllList = new LinkedList<>();
    
    // Times and corresponding text fields
    long[] times;
    ArrayList<JTextField> fields;
    int visibleFields = 16;
    
    /**
     * Creates TimingProfiler window
     * 
     * @param host Parent frame
     */
    public TimingProfiler(WhiteboxPluginHost host) {
        // want to use icon WhiteboxGIS/resources/Images/timer_clock.png
        
        this.host = host;
        
        initComponents();
        jPanel1.setVisible(true);
        jPanel2.setVisible(true);
        jPanel3.setVisible(true);
        jPanel4.setVisible(true);
        this.setVisible(true);
        
        // collect the text fields into an array so it's easy to update times
        fields = new ArrayList<>();
        fields.add(jTextField1);
        fields.add(jTextField2);
        fields.add(jTextField3);
        fields.add(jTextField4);
        fields.add(jTextField5);
        fields.add(jTextField6);
        fields.add(jTextField7);
        fields.add(jTextField8);
        fields.add(jTextField9);
        fields.add(jTextField10);
        fields.add(jTextField11);
        fields.add(jTextField12);
        fields.add(jTextField13);
        fields.add(jTextField14);
        fields.add(jTextField15);
        fields.add(jTextField16);
        times = new long[fields.size()];
        
        // This is the platform-specific max. no. of available processors that
        // we need to account for.
        int nprocs = Runtime.getRuntime().availableProcessors();
        
        int n = 0;
        for (Enumeration<AbstractButton> bg = selectProcsGrp.getElements(); bg.hasMoreElements(); ) {
            n++;
            JRadioButton b = (JRadioButton) bg.nextElement();
            if (n==nprocs) {
                selectProcsGrp.setSelected(b.getModel(), true);
            } else if (n>nprocs) {
                b.setVisible(false);
                fields.get(n-1).setVisible(false);
                visibleFields--;
            }
        }
        
        this.pack();
    }
    
    private void setSelectedProcessors(int nprocs) {
        // Go through the radio buttons, set as the default the one equal to
        // nprocs, and hide the remaining ones > nprocs, and their text fields
        // Problems:
        //      Hardcoded to 16; suppose nprocs > 16??
        int n = 0;
        
        for (Enumeration<AbstractButton> bg = selectProcsGrp.getElements(); bg.hasMoreElements(); ) {
            n++;
            JRadioButton b = (JRadioButton) bg.nextElement();
            if (n==nprocs) {
                selectProcsGrp.setSelected(b.getModel(), true);
                break;
            }
        }
    }
    
    /**
     * Gets the selected number of processors
     * @return the number of processors selected to be used.
     */
    private int getSelectedProcessors() {
                
        // find which radio button is currently clicked
        int n = 1;
        try {
            for (Enumeration<AbstractButton> btns = selectProcsGrp.getElements(); btns.hasMoreElements(); ) {
                JRadioButton radioBtn = (JRadioButton)btns.nextElement();
                if (radioBtn.isSelected()) {
                    n = Integer.parseInt(radioBtn.getActionCommand().trim());
                    break;
                }
            }
        } catch (NumberFormatException e) {
            System.out.println("Error getting action command for processor button.");
        }
        
        return n;
    }
    
    /**
     * Start a record for timing a particular plugin with an array of arguments.
     * Calling stopTiming() will cause the Timing Profiler to display the results,
     * and it can rerun the plugin with the same arguments, typically with a
     * different no. of processors.
     * 
     * @param plugin Plugin whose name was given to WhiteboxGUI.runPlugin()
     * @param args Array of arguments given to WhiteboxGUI.runPlugin()
     */
    public void startTiming( WhiteboxPlugin plugin, String[] args ) {
        // update no. of processors so plugin can obtain it
        int threads = getSelectedProcessors();
        Parallel.setPluginProcessors(threads);
        
        // remember the name and args for stopTiming() and rerunTool button
        this.plugin = plugin;
        if (plugin != null) {
            rerunToolButton.setEnabled(true);
            runAllButton.setEnabled(true);
        } else {
            rerunToolButton.setEnabled(false);
            runAllButton.setEnabled(false);
        }
        this.pluginArgs = args;
        
        // force garbage collection so it doesn't occur during timing run
        //System.gc();
        // NOTE: this was tried, but it's just a "suggestion" and didn't give
        // consistent results
        
        // very last thing, capture current time
        this.pluginStart = System.nanoTime();
    }
    
    /**
     * Report the run time of the plugin for which startTiming() was called.
     * 
     * @param nanosecs Wallclock execution time of plugin in nanoseconds.
     */
    public void stopTiming() {
        
        if (pluginStart == 0) {
            // We never started the timing for this plugin, leave now
            return;
        }
        
        // We store the nanosec. time, but display seconds.  Truncate to 100ths
        // seconds, and let Java formatter round to one decimal place for display.
        long execTime = System.nanoTime() - pluginStart;
        float execSecs = (float) ((float)(execTime/10000000)/100.0);
        
        // store results for current no. of processors
        int nprocs = Parallel.getPluginProcessors();
        times[nprocs-1] = execTime;
               
        // update to GUI should be done by Swing thread
        final String s1 = String.format("%.1f", execSecs);
        final String s2 = String.format("Tool name: %s%n" +
                    "Arguments[%d]: %s%n" +
                    "No. processors: %d%n" +
                    "Execution time (sec): %.1f%n%n",
                    (plugin!=null) ? plugin.getName() : "(unknown)",
                    pluginArgs.length,
                    Arrays.toString(pluginArgs),
                    nprocs, 
                    execSecs);
        
        // Figure out whether we need to pause before the next run,
        // because sleeping should be done on this thread to avoid
        // freezing the GUI.  There's only a next run if the size of the
        // runAllList > 1.
        final int ps = (runAllList.size() > 1) ? runAllPause.getValue() : 0;
        
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                String time = s1, report = s2;
                int pauseSecs = ps;
                @Override
                public void run() {
                    // display run time for no. processors
                    fields.get(Parallel.getPluginProcessors()-1).setText(time);

                    // display report in scrollable text area, "Pausing" messge
                    // if applicable, then scroll to (new) bottom
                    log.append(report);
                    
                    if ( pauseSecs > 0 ) {
                        log.append("Pausing " + pauseSecs + " seconds..." +
                                System.lineSeparator() );
                    }
                    log.setCaretPosition(log.getDocument().getLength());
                }
            });
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        // Now we can execute the actual pause, before returning control to
        // the finishing plugin.
        if ( ps > 0 ) {
            try {
                Thread.sleep(1000 * ps);    //sec -> msec
            }
            catch (Exception e) {}
        }        

        // Empty list means run finished and not under scope of "Run All"
        if (runAllList.isEmpty()) {
            if ( beepSelect.isSelected() )     // want end-of-run beep
                Toolkit.getDefaultToolkit().beep();
            return;
        }
        
        // The next plugin invocation should be done by the Swing thread, like
        // when "Rerun Tool" is clicked.  Unlike "Rerun Tool", we can't call
        // runPlugin() directly because stopTiming() is being called from the
        // terminating (but still running) plugin's call to pluginComplete().
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
               runNext();                
            };
        });
    }
    
    /**
     * Runs the next configuration {plugin, args, no. processors} from
     * runAllList.  Arrive here by clicking "Run All" button (which populates
     * the list), or a thread spawned from stopTiming().  At the end of all
     * passes, the list may contain only a sentinel used for logging the
     * times.
     */
    private void runNext() {

        // Are we at the end of a pass? (signified by <0 entry)
        int nextProcs = runAllList.remove(0);
        if ( nextProcs < 0 ) {
            copyTimesToLog();
            if ( runAllList.size() == 0 ) { // this was the last pass
                if ( beepSelect.isSelected() )     // wants end-of-runs beep
                    Toolkit.getDefaultToolkit().beep();
                stopButton.setEnabled(false);
                return;
                // Control goes back to user now
            }
            nextProcs = runAllList.remove(0);   // dequeue next entry
        }

        // Are we at the start of a new pass? (signified by 1 entry)
        if ( nextProcs == 1 ) {
            log.append("Passes remaining: " +
                    (1+runAllList.size())/(1+Runtime.getRuntime().availableProcessors()) +
                    "; " );
        }
        
        // Configure and launch the next run
        setSelectedProcessors(nextProcs);
        log.append("Starting next run..." + System.lineSeparator());
        log.setCaretPosition(log.getDocument().getLength());
        host.runPlugin(plugin.getName(), pluginArgs);
    }
    
     /**
     * Copies the per-processors execution times to the log as a CSV table. Called
     * by GUI "Copy Times to Log" button, and runNext() at end of pass.
     */
    private void copyTimesToLog() {
        log.append("/////" + System.lineSeparator());
        for (int i = 1; i <= visibleFields; i++) {
            log.append(i + "," + fields.get(i-1).getText() + System.lineSeparator());
        }
        log.append("\\\\\\\\\\" + System.lineSeparator() + System.lineSeparator());
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        selectProcsGrp = new javax.swing.ButtonGroup();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        procBtn1 = new javax.swing.JRadioButton();
        jTextField1 = new javax.swing.JTextField();
        procBtn2 = new javax.swing.JRadioButton();
        jTextField2 = new javax.swing.JTextField();
        procBtn3 = new javax.swing.JRadioButton();
        jTextField3 = new javax.swing.JTextField();
        procBtn4 = new javax.swing.JRadioButton();
        jTextField4 = new javax.swing.JTextField();
        procBtn5 = new javax.swing.JRadioButton();
        jTextField5 = new javax.swing.JTextField();
        procBtn6 = new javax.swing.JRadioButton();
        jTextField6 = new javax.swing.JTextField();
        procBtn7 = new javax.swing.JRadioButton();
        jTextField7 = new javax.swing.JTextField();
        procBtn8 = new javax.swing.JRadioButton();
        jTextField8 = new javax.swing.JTextField();
        procBtn9 = new javax.swing.JRadioButton();
        jTextField9 = new javax.swing.JTextField();
        procBtn10 = new javax.swing.JRadioButton();
        jTextField10 = new javax.swing.JTextField();
        procBtn11 = new javax.swing.JRadioButton();
        jTextField11 = new javax.swing.JTextField();
        procBtn12 = new javax.swing.JRadioButton();
        jTextField12 = new javax.swing.JTextField();
        procBtn13 = new javax.swing.JRadioButton();
        jTextField13 = new javax.swing.JTextField();
        procBtn14 = new javax.swing.JRadioButton();
        jTextField14 = new javax.swing.JTextField();
        procBtn15 = new javax.swing.JRadioButton();
        jTextField15 = new javax.swing.JTextField();
        procBtn16 = new javax.swing.JRadioButton();
        jTextField16 = new javax.swing.JTextField();
        jPanel2 = new javax.swing.JPanel();
        rerunToolButton = new javax.swing.JButton();
        copyToLogButton = new javax.swing.JButton();
        clearTimesButton = new javax.swing.JButton();
        saveLogToFileButton = new javax.swing.JButton();
        clearLogButton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        log = new javax.swing.JTextArea();
        jPanel3 = new javax.swing.JPanel();
        runAllButton = new javax.swing.JButton();
        runAllPause = new javax.swing.JSlider();
        jLabel4 = new javax.swing.JLabel();
        runAllPasses = new javax.swing.JSpinner();
        jLabel3 = new javax.swing.JLabel();
        stopButton = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        beepSelect = new javax.swing.JCheckBox();
        closeButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Timing Profiler");
        setMinimumSize(new java.awt.Dimension(800, 500));
        setPreferredSize(new java.awt.Dimension(800, 419));
        getContentPane().setLayout(new java.awt.GridBagLayout());

        jPanel1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        jPanel1.setMinimumSize(new java.awt.Dimension(300, 300));
        jPanel1.setName(""); // NOI18N
        jPanel1.setPreferredSize(new java.awt.Dimension(300, 100));
        jPanel1.setLayout(new java.awt.GridBagLayout());

        jLabel1.setText("Procs");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel1.add(jLabel1, gridBagConstraints);

        jLabel2.setText("Time (s)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel1.add(jLabel2, gridBagConstraints);

        selectProcsGrp.add(procBtn1);
        procBtn1.setText("1");
        procBtn1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                numProcessorsButtonClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel1.add(procBtn1, gridBagConstraints);

        jTextField1.setEditable(false);
        jTextField1.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextField1.setText("0.0");
        jTextField1.setPreferredSize(new java.awt.Dimension(80, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.ipadx = 74;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel1.add(jTextField1, gridBagConstraints);

        selectProcsGrp.add(procBtn2);
        procBtn2.setText("2");
        procBtn2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                numProcessorsButtonClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel1.add(procBtn2, gridBagConstraints);

        jTextField2.setEditable(false);
        jTextField2.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextField2.setText("0.0");
        jTextField2.setPreferredSize(new java.awt.Dimension(80, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.ipadx = 74;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel1.add(jTextField2, gridBagConstraints);

        selectProcsGrp.add(procBtn3);
        procBtn3.setText("3");
        procBtn3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                numProcessorsButtonClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel1.add(procBtn3, gridBagConstraints);

        jTextField3.setEditable(false);
        jTextField3.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextField3.setText("0.0");
        jTextField3.setPreferredSize(new java.awt.Dimension(80, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.ipadx = 74;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel1.add(jTextField3, gridBagConstraints);

        selectProcsGrp.add(procBtn4);
        procBtn4.setText("4");
        procBtn4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                numProcessorsButtonClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel1.add(procBtn4, gridBagConstraints);

        jTextField4.setEditable(false);
        jTextField4.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextField4.setText("0.0");
        jTextField4.setPreferredSize(new java.awt.Dimension(80, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.ipadx = 74;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel1.add(jTextField4, gridBagConstraints);

        selectProcsGrp.add(procBtn5);
        procBtn5.setText("5");
        procBtn5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                numProcessorsButtonClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel1.add(procBtn5, gridBagConstraints);

        jTextField5.setEditable(false);
        jTextField5.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextField5.setText("0.0");
        jTextField5.setPreferredSize(new java.awt.Dimension(80, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.ipadx = 74;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel1.add(jTextField5, gridBagConstraints);

        selectProcsGrp.add(procBtn6);
        procBtn6.setText("6");
        procBtn6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                numProcessorsButtonClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel1.add(procBtn6, gridBagConstraints);

        jTextField6.setEditable(false);
        jTextField6.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextField6.setText("0.0");
        jTextField6.setPreferredSize(new java.awt.Dimension(80, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.ipadx = 74;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel1.add(jTextField6, gridBagConstraints);

        selectProcsGrp.add(procBtn7);
        procBtn7.setText("7");
        procBtn7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                numProcessorsButtonClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel1.add(procBtn7, gridBagConstraints);

        jTextField7.setEditable(false);
        jTextField7.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextField7.setText("0.0");
        jTextField7.setPreferredSize(new java.awt.Dimension(80, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.ipadx = 74;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel1.add(jTextField7, gridBagConstraints);

        selectProcsGrp.add(procBtn8);
        procBtn8.setText("8");
        procBtn8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                numProcessorsButtonClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel1.add(procBtn8, gridBagConstraints);

        jTextField8.setEditable(false);
        jTextField8.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextField8.setText("0.0");
        jTextField8.setPreferredSize(new java.awt.Dimension(80, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.ipadx = 74;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel1.add(jTextField8, gridBagConstraints);

        selectProcsGrp.add(procBtn9);
        procBtn9.setText("9  ");
        procBtn9.setActionCommand("9");
        procBtn9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                numProcessorsButtonClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel1.add(procBtn9, gridBagConstraints);

        jTextField9.setEditable(false);
        jTextField9.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextField9.setText("0.0");
        jTextField9.setPreferredSize(new java.awt.Dimension(80, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.ipadx = 74;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel1.add(jTextField9, gridBagConstraints);

        selectProcsGrp.add(procBtn10);
        procBtn10.setText("10");
        procBtn10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                numProcessorsButtonClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel1.add(procBtn10, gridBagConstraints);

        jTextField10.setEditable(false);
        jTextField10.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextField10.setText("0.0");
        jTextField10.setPreferredSize(new java.awt.Dimension(80, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.ipadx = 74;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel1.add(jTextField10, gridBagConstraints);

        selectProcsGrp.add(procBtn11);
        procBtn11.setText("11");
        procBtn11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                numProcessorsButtonClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel1.add(procBtn11, gridBagConstraints);

        jTextField11.setEditable(false);
        jTextField11.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextField11.setText("0.0");
        jTextField11.setPreferredSize(new java.awt.Dimension(80, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.ipadx = 74;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel1.add(jTextField11, gridBagConstraints);

        selectProcsGrp.add(procBtn12);
        procBtn12.setText("12");
        procBtn12.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                numProcessorsButtonClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel1.add(procBtn12, gridBagConstraints);

        jTextField12.setEditable(false);
        jTextField12.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextField12.setText("0.0");
        jTextField12.setPreferredSize(new java.awt.Dimension(80, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.ipadx = 74;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel1.add(jTextField12, gridBagConstraints);

        selectProcsGrp.add(procBtn13);
        procBtn13.setText("13");
        procBtn13.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                numProcessorsButtonClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel1.add(procBtn13, gridBagConstraints);

        jTextField13.setEditable(false);
        jTextField13.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextField13.setText("0.0");
        jTextField13.setPreferredSize(new java.awt.Dimension(80, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 7;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.ipadx = 74;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel1.add(jTextField13, gridBagConstraints);

        selectProcsGrp.add(procBtn14);
        procBtn14.setText("14");
        procBtn14.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                numProcessorsButtonClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel1.add(procBtn14, gridBagConstraints);

        jTextField14.setEditable(false);
        jTextField14.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextField14.setText("0.0");
        jTextField14.setPreferredSize(new java.awt.Dimension(80, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 7;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.ipadx = 74;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel1.add(jTextField14, gridBagConstraints);

        selectProcsGrp.add(procBtn15);
        procBtn15.setText("15");
        procBtn15.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                numProcessorsButtonClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel1.add(procBtn15, gridBagConstraints);

        jTextField15.setEditable(false);
        jTextField15.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextField15.setText("0.0");
        jTextField15.setPreferredSize(new java.awt.Dimension(80, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 7;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.ipadx = 74;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel1.add(jTextField15, gridBagConstraints);

        selectProcsGrp.add(procBtn16);
        procBtn16.setText("16");
        procBtn16.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                numProcessorsButtonClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel1.add(procBtn16, gridBagConstraints);

        jTextField16.setEditable(false);
        jTextField16.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextField16.setText("0.0");
        jTextField16.setPreferredSize(new java.awt.Dimension(80, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 7;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.ipadx = 74;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        jPanel1.add(jTextField16, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 20;
        gridBagConstraints.ipady = 20;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        getContentPane().add(jPanel1, gridBagConstraints);

        rerunToolButton.setText("Rerun Tool");
        rerunToolButton.setToolTipText("Repeat last run with same arguments using selected processors.");
        rerunToolButton.setActionCommand("");
        rerunToolButton.setEnabled(false);
        rerunToolButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rerunToolButtonActionPerformed(evt);
            }
        });
        jPanel2.add(rerunToolButton);

        copyToLogButton.setText("Copy Times to Log");
        copyToLogButton.setToolTipText("Copy all times into log below.");
        copyToLogButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyToLogButtonActionPerformed(evt);
            }
        });
        jPanel2.add(copyToLogButton);

        clearTimesButton.setText("Clear Times");
        clearTimesButton.setToolTipText("Clear all times to zero.");
        clearTimesButton.setPreferredSize(new java.awt.Dimension(121, 23));
        clearTimesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearTimesButtonActionPerformed(evt);
            }
        });
        jPanel2.add(clearTimesButton);

        saveLogToFileButton.setText("Save Log to File");
        saveLogToFileButton.setToolTipText("Save log contents as a text file.");
        saveLogToFileButton.setPreferredSize(new java.awt.Dimension(123, 23));
        saveLogToFileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveLogToFileButtonActionPerformed(evt);
            }
        });
        jPanel2.add(saveLogToFileButton);

        clearLogButton.setText("Clear Log");
        clearLogButton.setToolTipText("Clear log contents below.");
        clearLogButton.setPreferredSize(new java.awt.Dimension(123, 23));
        clearLogButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearLogButtonActionPerformed(evt);
            }
        });
        jPanel2.add(clearLogButton);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        getContentPane().add(jPanel2, gridBagConstraints);

        jScrollPane1.setPreferredSize(new java.awt.Dimension(400, 200));

        log.setColumns(20);
        log.setRows(5);
        log.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));
        jScrollPane1.setViewportView(log);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.9;
        getContentPane().add(jScrollPane1, gridBagConstraints);

        runAllButton.setText("Run All");
        runAllButton.setToolTipText("Rerun tool with same arguments, using all configurations of processors, from 1 to max.");
        runAllButton.setEnabled(false);
        runAllButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                runAllButtonActionPerformed(evt);
            }
        });
        jPanel3.add(runAllButton);

        runAllPause.setMajorTickSpacing(60);
        runAllPause.setMaximum(300);
        runAllPause.setMinorTickSpacing(10);
        runAllPause.setPaintLabels(true);
        runAllPause.setPaintTicks(true);
        runAllPause.setSnapToTicks(true);
        runAllPause.setToolTipText("Seconds to pause between \"Run All\" passes");
        runAllPause.setValue(0);
        jPanel3.add(runAllPause);

        jLabel4.setText("pause (s)");
        jPanel3.add(jLabel4);

        runAllPasses.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(1), Integer.valueOf(1), null, Integer.valueOf(1)));
        runAllPasses.setToolTipText("No. of passes repeating \"Run All\".");
        runAllPasses.setPreferredSize(new java.awt.Dimension(40, 18));
        jPanel3.add(runAllPasses);

        jLabel3.setText("passes");
        jPanel3.add(jLabel3);

        stopButton.setText("Stop");
        stopButton.setToolTipText("Stop \"Run All\" after current run.");
        stopButton.setEnabled(false);
        stopButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopButtonActionPerformed(evt);
            }
        });
        jPanel3.add(stopButton);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        getContentPane().add(jPanel3, gridBagConstraints);

        beepSelect.setSelected(true);
        beepSelect.setText("Beep");
        beepSelect.setToolTipText("Tries to beep at end of final run.");
        beepSelect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                beepSelectActionPerformed(evt);
            }
        });
        jPanel4.add(beepSelect);

        closeButton.setText("Close");
        closeButton.setToolTipText("Stop further runs and close profiler. Current run will finish.");
        closeButton.setPreferredSize(new java.awt.Dimension(85, 23));
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeButtonActionPerformed(evt);
            }
        });
        jPanel4.add(closeButton);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        getContentPane().add(jPanel4, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void rerunToolButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rerunToolButtonActionPerformed
        if (plugin != null) {
            host.runPlugin(plugin.getName(), pluginArgs);
        }
    }//GEN-LAST:event_rerunToolButtonActionPerformed

    private void closeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeButtonActionPerformed
        runAllList.clear();
        this.dispose();
    }//GEN-LAST:event_closeButtonActionPerformed

    private void clearTimesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearTimesButtonActionPerformed
        int n=0;
        for (JTextField f : fields) {
            f.setText("0.0");
            times[n++] = 0;
        }
    }//GEN-LAST:event_clearTimesButtonActionPerformed

    private void clearLogButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearLogButtonActionPerformed
        log.setText("");
    }//GEN-LAST:event_clearLogButtonActionPerformed

    private void copyToLogButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyToLogButtonActionPerformed
        copyTimesToLog();
    }//GEN-LAST:event_copyToLogButtonActionPerformed

    private void saveLogToFileButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveLogToFileButtonActionPerformed
        JFileChooser chooser = new JFileChooser();
        int value = chooser.showSaveDialog(this);
        
        if (value == JFileChooser.APPROVE_OPTION) {     
            // Open the selected file and write log to it
            File selection = chooser.getSelectedFile();           
            try {
                if (!selection.exists()) {
                    selection.createNewFile();
                }
                
                String logText = log.getText();
                // try with resource, it will close on good or bad result
                // BufferedWriter needs a size > 0, adding 1 to prevent exception
                try (BufferedWriter bf = new BufferedWriter(new FileWriter(selection), logText.getBytes().length + 1)) {
                    bf.append(logText);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Unable to save file", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Unable to create new file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_saveLogToFileButtonActionPerformed

    private void runAllButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_runAllButtonActionPerformed
        
        if (plugin != null) {
            
             // Add all processor configurations times no. of passes to run queue.
            int availableProcs = Runtime.getRuntime().availableProcessors();
            int numPasses = (Integer)runAllPasses.getValue();
            runAllList.clear();
            for (int j = 1; j <= numPasses; j++) {
                for (int i = 1; i <= availableProcs; i++) {
                    runAllList.add(i);
                }
                runAllList.add(-1);     // signals end of pass
            }
            
            stopButton.setEnabled(true);
            runNext();
        }
    }//GEN-LAST:event_runAllButtonActionPerformed

    private void numProcessorsButtonClicked(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_numProcessorsButtonClicked
        Parallel.setPluginProcessors(this.getSelectedProcessors());
    }//GEN-LAST:event_numProcessorsButtonClicked

    private void stopButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopButtonActionPerformed
        runAllList.clear();
    }//GEN-LAST:event_stopButtonActionPerformed

    private void beepSelectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_beepSelectActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_beepSelectActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox beepSelect;
    private javax.swing.JButton clearLogButton;
    private javax.swing.JButton clearTimesButton;
    private javax.swing.JButton closeButton;
    private javax.swing.JButton copyToLogButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField10;
    private javax.swing.JTextField jTextField11;
    private javax.swing.JTextField jTextField12;
    private javax.swing.JTextField jTextField13;
    private javax.swing.JTextField jTextField14;
    private javax.swing.JTextField jTextField15;
    private javax.swing.JTextField jTextField16;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField4;
    private javax.swing.JTextField jTextField5;
    private javax.swing.JTextField jTextField6;
    private javax.swing.JTextField jTextField7;
    private javax.swing.JTextField jTextField8;
    private javax.swing.JTextField jTextField9;
    private javax.swing.JTextArea log;
    private javax.swing.JRadioButton procBtn1;
    private javax.swing.JRadioButton procBtn10;
    private javax.swing.JRadioButton procBtn11;
    private javax.swing.JRadioButton procBtn12;
    private javax.swing.JRadioButton procBtn13;
    private javax.swing.JRadioButton procBtn14;
    private javax.swing.JRadioButton procBtn15;
    private javax.swing.JRadioButton procBtn16;
    private javax.swing.JRadioButton procBtn2;
    private javax.swing.JRadioButton procBtn3;
    private javax.swing.JRadioButton procBtn4;
    private javax.swing.JRadioButton procBtn5;
    private javax.swing.JRadioButton procBtn6;
    private javax.swing.JRadioButton procBtn7;
    private javax.swing.JRadioButton procBtn8;
    private javax.swing.JRadioButton procBtn9;
    private javax.swing.JButton rerunToolButton;
    private javax.swing.JButton runAllButton;
    private javax.swing.JSpinner runAllPasses;
    private javax.swing.JSlider runAllPause;
    private javax.swing.JButton saveLogToFileButton;
    private javax.swing.ButtonGroup selectProcsGrp;
    private javax.swing.JButton stopButton;
    // End of variables declaration//GEN-END:variables
}
