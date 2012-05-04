package whiteboxgis;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import whitebox.interfaces.DialogComponent;

/**
 *
 * @author johnlindsay
 */
public class DialogComboBox extends JPanel implements ActionListener, DialogComponent {
   
    private int numArgs = 5;
    private String name;
    private String description;
    private String value;
    private String label;
    private JLabel lbl = new JLabel();
    private JComboBox comboBox = new JComboBox();
    
    private void createUI() {
        try {
            this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            Border border = BorderFactory.createEmptyBorder(5, 5, 5, 5);
            this.setBorder(border);
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
            
            comboBox.addActionListener(this);
            lbl = new JLabel(label);
            panel.add(lbl);
            panel.add(Box.createHorizontalStrut(5));
            panel.add(comboBox);
            panel.setToolTipText(description);
            this.setToolTipText(description);
            comboBox.setToolTipText(description);
            lbl.setToolTipText(description);
            this.add(panel);
            this.add(Box.createHorizontalGlue());
            
            this.setMaximumSize(new Dimension(2500, 40));
            this.setPreferredSize(new Dimension(350, 40));
        
        } catch (Exception e) {
            System.out.println(e.getCause());
        }
    }
    
    public String getValue() {
        return value.trim();
    }
    
    public String getComponentName() {
        return name;
    }
    
    public boolean getOptionalStatus() {
        return false;
    }
    
    public boolean setArgs(String[] args) {
        try {
            // first make sure that there are the right number of args
            if (args.length != numArgs) {
                return false;
            }
            name = args[0];
            description = args[1];
            label = args[2];
            String[] listItems = args[3].split(",");
            for (int i = 0; i < listItems.length; i++) {
                listItems[i] = listItems[i].trim();
            }
            comboBox = new JComboBox(listItems);
            comboBox.setSelectedIndex(Integer.parseInt(args[4]));
            value = (String)comboBox.getSelectedItem();
            createUI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public String[] getArgsDescriptors() {
        String[] argsDescriptors = new String[numArgs];
        argsDescriptors[0] = "String name";
        argsDescriptors[1] = "String description";
        argsDescriptors[2] = "String label";
        argsDescriptors[3] = "String[] listItems";
        argsDescriptors[4] = "Int defaultItem (zero-based)";
        return argsDescriptors;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        JComboBox cb = (JComboBox)e.getSource();
        value = (String)cb.getSelectedItem();
    }
}
