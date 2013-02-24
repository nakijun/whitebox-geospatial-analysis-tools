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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.swing.table.AbstractTableModel;
import whitebox.geospatialfiles.shapefile.attributes.AttributeTable;
import whitebox.geospatialfiles.shapefile.attributes.DBFException;
import whitebox.geospatialfiles.shapefile.attributes.DBFField;
import whitebox.interfaces.WhiteboxPluginHost;

/**
 * Model for displaying an AttributeTable in AttributeFilesViewer.
 * @author Kevin Green
 */
public class AttributeFileTableModel extends AbstractTableModel {
    
    private AttributeTable table;
    
    // A lookup table for changed rows. This allows changes to exist only in memory
    private HashMap<Integer, Object[]> changedRows = new HashMap<>();
    
    public AttributeFileTableModel(AttributeTable table) {
        this.table = table;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public int getRowCount() {
        return table.getNumberOfRecords();
    }

    @Override
    public int getColumnCount() {
        // Add 1 for the ID column
        return table.getFieldCount() + 1;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        
        if (columnIndex == 0) {
            return Integer.class;
        }
        
        int fieldIndex = columnIndex - 1;
        
        Class<?> klass = Object.class;
        DBFField[] fields = table.getAllFields();
        if (fieldIndex < fields.length) {
            klass = fields[fieldIndex].getDataType().getEquivalentClass();
        }
        
        return klass;
    }
    
    @Override
    public String getColumnName(int column) {
        if (column == 0) {
            return "ID";
        }
        int fieldIndex = column - 1;
        String[] names = table.getAttributeTableFieldNames();
        if (names != null && names.length > fieldIndex) {
            return names[fieldIndex];
        }
        
        // Return empty string if column name doesn't exist
        return "";
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        
        if (columnIndex == 0) {
            return rowIndex;
        }
        int fieldIndex = columnIndex - 1;
        try {
            
            // First check if the row is in the changed rows
            Object[] row = changedRows.get(rowIndex);
            
            // Not changed, get it from disk
            if (row == null) {
                row = table.getRecord(rowIndex);
            }

            if (row != null && row.length > fieldIndex) {
                // First column shown is the ID
                return row[fieldIndex];
            }
        } catch (DBFException e) {
            System.out.println(e.getMessage());
        }
        
        return null;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        // Check if the type of aValue fits to table at rowIndex, columnIndex
        
        // columnIndex 0 is always ID
        if (columnIndex == 0) {
            return;
        }
        try {
            int fieldIndex = columnIndex - 1;
            Object[] row = table.getRecord(rowIndex);
            
            if (row != null && row.length > fieldIndex) {
                row[fieldIndex] = aValue;
                changedRows.put(rowIndex, row);
            }
        } catch (DBFException e) {
            System.out.println(e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("Entered value not compatible with field type");
        }
    }
    
    /**
     * Method to signal from the view to the model that changes to row values should
     * be saved to disk.
     * @return True if all changes were saved and false if some changes couldn't
     * be saved.
     */
    public boolean commitChanges() {
        
        Set<Map.Entry<Integer, Object[]>> entries = changedRows.entrySet();
        
        for (Iterator<Map.Entry<Integer, Object[]>> iter = entries.iterator(); iter.hasNext();) {
            Map.Entry<Integer, Object[]> entry = iter.next();
            try {
                table.updateRecord(entry.getKey(), entry.getValue());
                iter.remove();
            } catch (DBFException e) {
                System.out.println(e.getMessage());
            }
            
        }
        
        if (!changedRows.isEmpty()) {
            // Some changes weren't saved
            return false;
        }
        
        return true;
        
        
    }
    
}
