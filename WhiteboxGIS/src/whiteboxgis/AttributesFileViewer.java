/*
 * Copyright (C) 2012 Dr. John Lindsay <jlindsay@uoguelph.ca>
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
package whiteboxgis;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import whitebox.geospatialfiles.ShapeFile;
import whitebox.geospatialfiles.shapefile.ShapeFileRecord;
import whitebox.geospatialfiles.shapefile.ShapeType;
import whitebox.geospatialfiles.shapefile.attributes.DBFField;
import whitebox.geospatialfiles.shapefile.attributes.AttributeTable;
import whitebox.geospatialfiles.shapefile.attributes.DBFField.DBFDataType;
import whitebox.interfaces.WhiteboxPluginHost;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class AttributesFileViewer extends JDialog implements ActionListener {
    
    private String dbfFileName = "";
    private String shapeFileName = "";

    private AttributeTable attributeTable;
    //private JButton edit = new JButton("Edit");
    private JTable dataTable = new JTable();
    private JTable fieldTable = new JTable();
    private JTabbedPane tabs;
    private WhiteboxPluginHost host = null;
    private ShapeFile shapeFile = null;
    
    public AttributesFileViewer(Frame owner, boolean modal, String shapeFileName) {
        super(owner, modal);
        if (owner instanceof WhiteboxPluginHost) {
            host = (WhiteboxPluginHost)owner;
        }
        if (owner != null) {
            Dimension parentSize = owner.getSize(); 
            Point p = owner.getLocation(); 
            setLocation(p.x + parentSize.width / 4, p.y + parentSize.height / 4);
        }
        this.shapeFileName = shapeFileName;

        if (shapeFileName.toLowerCase().contains(".shp")) {
            dbfFileName = shapeFileName.replace(".shp", ".dbf");
        } else if (shapeFileName.toLowerCase().contains(".dbf")) {
            dbfFileName = shapeFileName;
        }
        try {
            shapeFile = new ShapeFile(shapeFileName);
            attributeTable = shapeFile.getAttributeTable();
            createGui();
        } catch (IOException e) {
            if (owner instanceof WhiteboxPluginHost) {
                WhiteboxPluginHost wph = (WhiteboxPluginHost)owner;
                wph.showFeedback("DBF file not read properly. It is possible that there is no database file.");
            } else {
                JLabel warning = new JLabel("DBF file not read properly. It is possible that there is no database file.");
                this.add(warning);
            }
        } 
        
        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                closeWindow();
            }
        });

    }
    
    private void createGui() {
        try {
            if (System.getProperty("os.name").contains("Mac")) {
                this.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
            }
            
            File file = new File(dbfFileName);
            String shortFileName = file.getName();
            shortFileName = shortFileName.replace(".dbf", "");
        
            setTitle("Layer Attribute Table: " + shortFileName);

            // okay and close buttons.
            Box box1 = Box.createHorizontalBox();
            box1.add(Box.createHorizontalStrut(10));
            box1.add(Box.createRigidArea(new Dimension(5, 30)));
            box1.add(Box.createRigidArea(new Dimension(5, 30)));
            
            JButton close = new JButton("Close");
            close.setActionCommand("close");
            close.addActionListener(this);
            close.setToolTipText("Exit without saving changes");
            box1.add(close);
            
            JButton save = new JButton("Save");
            save.setActionCommand("save");
            save.addActionListener(this);
            save.setToolTipText("Save changes to disk");
            box1.add(save);
            
            box1.add(Box.createHorizontalStrut(100));
            box1.add(Box.createHorizontalGlue());
            
            add(box1, BorderLayout.SOUTH);

            Box mainBox = Box.createVerticalBox();
            
            dataTable = getDataTable();
          
            JScrollPane scroll = new JScrollPane(dataTable);
            tabs = new JTabbedPane();

            JPanel panel1 = new JPanel();
            panel1.setLayout(new BoxLayout(panel1, BoxLayout.Y_AXIS));
            panel1.add(scroll);
            tabs.addTab("Attributes Table", panel1);

            // field table
                        
            JPanel panel2 = new JPanel();
            panel2.setLayout(new BoxLayout(panel2, BoxLayout.Y_AXIS));
            
            fieldTable = getFieldTable();

            JScrollPane scroll2 = new JScrollPane(fieldTable);
            panel2.add(scroll2);
            tabs.addTab("Field Summary", panel2);
            
            mainBox.add(tabs);
            this.getContentPane().add(mainBox, BorderLayout.CENTER);
            
            JMenuBar menubar = createMenu();
            this.setJMenuBar(menubar);

            pack();

            // Centre the dialog on the screen.
            // Get the size of the screen
            Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
            int screenHeight = dim.height;
            int screenWidth = dim.width;
            //setSize(screenWidth / 2, screenHeight / 2);
            this.setLocation(screenWidth / 4, screenHeight / 4);
            
        } catch (Exception e) {
            System.out.println(e);
        }
    }
    
    private JTable getDataTable() {
        
        JTable table = new JTable(new AttributeFileTableModel(attributeTable)) {

            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int Index_row, int Index_col) {
                Component comp = super.prepareRenderer(renderer, Index_row, Index_col);
                //even index, selected or not selected
                if (Index_row % 2 == 0) {// && !isCellSelected(Index_row, Index_col)) {
                    comp.setBackground(Color.WHITE);
                    comp.setForeground(Color.BLACK);
                } else {
                    comp.setBackground(new Color(225, 245, 255)); //new Color(210, 230, 255));
                    comp.setForeground(Color.BLACK);
                }
                if (isCellSelected(Index_row, Index_col)) {
                    comp.setForeground(Color.RED);
                }
                return comp;
            }
        };

        table.setAutoCreateRowSorter(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        TableColumn column;

        for (int i = 0; i < table.getColumnCount(); i++) {
            column = table.getColumnModel().getColumn(i);
            if (i == 0) {
                column.setPreferredWidth(10);
            } else if (i == 1) {
                column.setPreferredWidth(40);
            } else {
                column.setPreferredWidth(70);
            }
        }
        
        return table;
    }
    
    private JTable getFieldTable() {

        JTable table = new JTable(new AttributeFieldTableModel(attributeTable)) {

            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int Index_row, int Index_col) {
                Component comp = super.prepareRenderer(renderer, Index_row, Index_col);
                //even index, selected or not selected
                if (Index_row % 2 == 0) {// && !isCellSelected(Index_row, Index_col)) {
                    comp.setBackground(Color.WHITE);
                    comp.setForeground(Color.BLACK);
                } else {
                    comp.setBackground(new Color(225, 245, 255)); //new Color(210, 230, 255));
                    comp.setForeground(Color.BLACK);
                }
                if (isCellSelected(Index_row, Index_col)) {
                    comp.setForeground(Color.RED);
                }
                return comp;
            }
        };

        // Add cell editor for type column
        int typeColIndex = AttributeFieldTableModel.ColumnName.TYPE.ordinal();
        TableColumn typeColumn = table.getColumnModel().getColumn(typeColIndex);
        JComboBox typeComboBox = new JComboBox();
        for (DBFDataType type : DBFDataType.values()) {
            typeComboBox.addItem(type);
        }
        typeColumn.setCellEditor(new DefaultCellEditor(typeComboBox));
        
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);        
        int modifiedColIndex = AttributeFieldTableModel.ColumnName.MODIFIED.ordinal();
        TableColumn modifiedColumn = table.getColumnModel().getColumn(modifiedColIndex);
        modifiedColumn.setPreferredWidth(10);

        return table;
    }

    private JMenuBar createMenu() {
        JMenuBar menubar = new JMenuBar();

        // Add Field menu
        JMenu addFieldMenu = new JMenu("Add Field");
        
        JMenuItem addNewField = new JMenuItem("Add New Field");
        addNewField.setActionCommand("addNewField");
        addNewField.addActionListener(this);
        addFieldMenu.add(addNewField);
        
        JMenuItem deleteField = new JMenuItem("Delete Field...");
        addNewField.setActionCommand("deleteField");
        addNewField.addActionListener(this);
        addFieldMenu.add(deleteField);
        
        if (shapeFile.getShapeType().getBaseType() == ShapeType.POLYGON) {
            JMenuItem addAreaField = new JMenuItem("Add Area Field");
            addAreaField.setActionCommand("addAreaField");
            addAreaField.addActionListener(this);
            addFieldMenu.add(addAreaField);
            
            JMenuItem addPerimeterField = new JMenuItem("Add Perimeter Field");
            addPerimeterField.setActionCommand("addPerimeterField");
            addPerimeterField.addActionListener(this);
            addFieldMenu.add(addPerimeterField);
        }
        
        menubar.add(addFieldMenu);
        
        return menubar;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        String actionCommand = e.getActionCommand();
        switch (actionCommand) {
            case "close":
                closeWindow();
                break;
            case "save":
                saveChanges();
                break;
            case "addFID":
                addFID();
                break;
            case "addNewField":
                addNewField();
                break;
            case "deleteField":
                deleteField();
            case "addAreaField":
                addAreaField();
                break;
            case "addPerimeterField":
                addPerimeterField();
                break;
        }
    }
    
    private void closeWindow() {
        AttributeFileTableModel dataModel = (AttributeFileTableModel)dataTable.getModel();
        AttributeFieldTableModel fieldModel = (AttributeFieldTableModel)fieldTable.getModel();
        if (!fieldModel.isSaved()) {
            tabs.setSelectedIndex(1);
            int continueChoice = JOptionPane.showOptionDialog(this, 
                    "New fields were created and not saved, do you want to continue without saving?", 
                    "Continue?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
            if (continueChoice != JOptionPane.OK_OPTION) {
                return;
            }
        }
        if (!dataModel.isSaved()) {
            tabs.setSelectedIndex(0);
            int continueChoice = JOptionPane.showOptionDialog(this, 
                    "There are unsaved changes to the table data, do you want to continue without saving?", 
                    "Continue?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
            if (continueChoice != JOptionPane.OK_OPTION) {
                return;
            }
        }
        this.dispose();
    }
    
    private void saveChanges() {
        
        if (dataTable.isShowing()) {
            int option = JOptionPane.showOptionDialog(rootPane, "Are you sure you want to save changes to data?", 
                "Save Data Changes?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
            if (option == JOptionPane.OK_OPTION) {
                AttributeFileTableModel model = (AttributeFileTableModel)dataTable.getModel();
                boolean success = model.commitChanges();
                if (!success) {
                    JOptionPane.showMessageDialog(rootPane, "Error saving database file. Some changes have not been saved.", "Error Saving", JOptionPane.ERROR_MESSAGE);
                    // TODO: Report to use which rows weren't saved
                }
            }
        } else if (fieldTable.isShowing()) {
            int option = JOptionPane.showOptionDialog(rootPane, "Are you sure you want to save changes to fields? Warning: This operation can take a long time.", 
                "Save Field Changes?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
            if (option == JOptionPane.OK_OPTION) {
                AttributeFieldTableModel fieldModel = (AttributeFieldTableModel)fieldTable.getModel();
                boolean success = fieldModel.commitChanges();
                if (!success) {
                    JOptionPane.showMessageDialog(rootPane, "Error saving database file. Some changes have not been saved.", "Error Saving", JOptionPane.ERROR_MESSAGE);
                }
                
                AttributeFileTableModel tableModel = (AttributeFileTableModel)dataTable.getModel();
                tableModel.fireTableStructureChanged();
            }
            
        }
        
        
        
    }
    
    private void addFID() {
        
        AttributeFieldTableModel fieldModel = (AttributeFieldTableModel)fieldTable.getModel();
        
        DBFField field = new DBFField();
        field.setName("FID");
        field.setDataType(DBFField.DBFDataType.NUMERIC);
        field.setFieldLength(10);
        field.setDecimalCount(0);
        
        fieldModel.createNewField(field);
        
        fieldTable.setVisible(true);
        
        /*try {
            
            DBFField field = new DBFField();
            field.setName("FID");
            field.setDataType(DBFField.DBFDataType.NUMERIC);
            field.setFieldLength(10);
            field.setDecimalCount(0);
            attributeTable.addField(field);
            
            for (int a = 0; a < attributeTable.getNumberOfRecords(); a++) {
                Object[] recData = attributeTable.getRecord(a);
                recData[recData.length - 1] = new Double(a);
                attributeTable.updateRecord(a, recData);
            }
            
            AttributeFileTableModel tableModel = (AttributeFileTableModel)dataTable.getModel();
            tableModel.fireTableStructureChanged();
            
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }*/
    }
    
    /**
     * Adds a new field to the field table model.
     */
    private void addNewField() {
        AttributeFieldTableModel model = (AttributeFieldTableModel)fieldTable.getModel();
        
        model.createNewField();
    }
    
    /**
     * Hides a field from the field table model and marks the field for deletion
     * on the next save.
     */
    private void deleteField() {
        
    }
    
    private void addAreaField() {
        
        try {
            ShapeType inputType = shapeFile.getShapeType();
            if (inputType.getBaseType() != ShapeType.POLYGON) {
                if (host != null) {
                    host.showFeedback("This function can only be applied to polygon type shapefiles.");
                    return;
                }
            }
            double area;
            int recNum;

            DBFField field = new DBFField();
            field.setName("Area");
            field.setDataType(DBFField.DBFDataType.NUMERIC);
            field.setFieldLength(10);
            field.setDecimalCount(3);
            this.attributeTable.addField(field);
            for (ShapeFileRecord record : shapeFile.records) {
                if (record.getShapeType() != ShapeType.NULLSHAPE) {
                    if (inputType == ShapeType.POLYGON) {
                        whitebox.geospatialfiles.shapefile.Polygon recPolygon =
                                (whitebox.geospatialfiles.shapefile.Polygon) (record.getGeometry());
                        area = recPolygon.getArea();
                    } else if (inputType == ShapeType.POLYGONZ) {
                        whitebox.geospatialfiles.shapefile.PolygonZ recPolygon =
                                (whitebox.geospatialfiles.shapefile.PolygonZ) (record.getGeometry());
                        area = recPolygon.getArea();
                    } else { // POLYGONM
                        whitebox.geospatialfiles.shapefile.PolygonM recPolygon =
                                (whitebox.geospatialfiles.shapefile.PolygonM) (record.getGeometry());
                        area = recPolygon.getArea();
                    }
                    
                    recNum = record.getRecordNumber() - 1;
                    Object[] recData = this.attributeTable.getRecord(recNum);
                    recData[recData.length - 1] = new Double(area);
                    this.attributeTable.updateRecord(recNum, recData);

                }                        
            }
            
            host.showFeedback("Calculation complete!");
            
        } catch (Exception e) {
            if (host != null) {
                host.showFeedback(e.getMessage());
            }
        }
        
    }
    
    
    private void addPerimeterField() {
        
        try {
            ShapeType inputType = shapeFile.getShapeType();
            if (inputType.getBaseType() != ShapeType.POLYGON) {
                if (host != null) {
                    host.showFeedback("This function can only be applied to polygon type shapefiles.");
                    return;
                }
            }
            double perimeter;
            int recNum;
            //double numRecordsDone = 0;
            //int progress = 0;
            //double numRecords = shapeFile.getNumberOfRecords();
            DBFField field = new DBFField();
            field.setName("Perimeter");
            field.setDataType(DBFField.DBFDataType.NUMERIC);
            field.setFieldLength(10);
            field.setDecimalCount(3);
            this.attributeTable.addField(field);
            for (ShapeFileRecord record : shapeFile.records) {
                if (inputType != ShapeType.NULLSHAPE) {
                    if (shapeFile.getShapeType() == ShapeType.POLYGON) {
                        whitebox.geospatialfiles.shapefile.Polygon recPolygon =
                                (whitebox.geospatialfiles.shapefile.Polygon) (record.getGeometry());
                        perimeter = recPolygon.getPerimeter();
                    } else if (inputType == ShapeType.POLYGONZ) {
                        whitebox.geospatialfiles.shapefile.PolygonZ recPolygon =
                                (whitebox.geospatialfiles.shapefile.PolygonZ) (record.getGeometry());
                        perimeter = recPolygon.getPerimeter();
                    } else { // POLYGONM
                        whitebox.geospatialfiles.shapefile.PolygonM recPolygon =
                                (whitebox.geospatialfiles.shapefile.PolygonM) (record.getGeometry());
                        perimeter = recPolygon.getPerimeter();
                    }
                    recNum = record.getRecordNumber() - 1;
                    Object[] recData = this.attributeTable.getRecord(recNum);
                    recData[recData.length - 1] = new Double(perimeter);
                    this.attributeTable.updateRecord(recNum, recData);

                }

            }
            
            host.showFeedback("Calculation complete!");
            
        } catch (Exception e) {
            if (host != null) {
                host.showFeedback(e.getMessage());
            }
        }
        
    }
    
}
