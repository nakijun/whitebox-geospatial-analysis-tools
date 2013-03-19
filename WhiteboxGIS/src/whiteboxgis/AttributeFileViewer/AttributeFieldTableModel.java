/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package whiteboxgis.AttributeFileViewer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.swing.table.AbstractTableModel;
import whitebox.geospatialfiles.shapefile.attributes.AttributeTable;
import whitebox.geospatialfiles.shapefile.attributes.DBFException;
import whitebox.geospatialfiles.shapefile.attributes.DBFField;
import whitebox.geospatialfiles.shapefile.attributes.DBFField.DBFDataType;

/**
 * AttributeFieldTableModel. A model for displaying and editing DBFField data for
 * an attribute file.
 * @author Kevin Green
 */
public class AttributeFieldTableModel extends AbstractTableModel {
    
    private AttributeTable attributeTable;
    
    private static final String MODIFIED_INDICATOR = "*";
    private static final String NOT_MODIFIED_INDICATOR = "";
    
    // To keep track of changes to existing fields
    private HashMap<Integer, DBFField> newFields = new HashMap<>();
    // To keep track of deleted fields
    private HashMap<Integer, DBFField> deletedFields = new HashMap<>();
    
    protected enum ColumnName {
        MODIFIED("", String.class),
        NAME("Name", String.class), 
        TYPE("Type", DBFDataType.class), 
        LENGTH("Length", Integer.class), 
        PRECISION("Precision", Integer.class);
        
        public static final int size = ColumnName.values().length;
        
        private static final ColumnName[] values = ColumnName.values();
        
        private final String displayName;
        private final Class<?> columnClass;
        
        ColumnName(String displayName, Class<?> columnClass) {
            this.displayName = displayName;
            this.columnClass = columnClass;
        }
        
        public static ColumnName fromColumnIndex(int columnIndex) {
            if (columnIndex < ColumnName.values().length) {
                return values[columnIndex];
            }
            
            return null;
        }
        
        public Class<?> getColumnClass() {
            return this.columnClass;
        }

        @Override
        public String toString() {
            return this.displayName;
        }
        
    }
    
    
    public AttributeFieldTableModel(AttributeTable attributeTable) {
        this.attributeTable = attributeTable;
    }

    @Override
    public int getRowCount() {
        return attributeTable.getFieldCount() + newFields.size() - deletedFields.size();
    }

    @Override
    public int getColumnCount() {
        return ColumnName.values().length;
    }
    
    @Override
    public String getColumnName(int column) {
        
        ColumnName columnName = ColumnName.fromColumnIndex(column);
        if (columnName != null) {
            return columnName.toString();
        }
        return null;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        
        // Modified column isn't editable
        if (columnIndex == 0) {
            return false;
        }
        
        // Only allow new fields to be edited until edit functionality is added
        DBFField row = newFields.get(getActualRow(rowIndex));
        if (row != null) {
            if (row.getDataType() == DBFDataType.DATE) {
                if (ColumnName.fromColumnIndex(columnIndex) != ColumnName.LENGTH 
                    && ColumnName.fromColumnIndex(columnIndex) != ColumnName.PRECISION) {
                    return true;
                }
            } else {
                return true;
            }
        }
        
        return false;
        
    }
    
    /**
     * Returns the actual index of the underlying attribute table fields that
     * represented the model's index. Deleted fields are only hidden in the model
     * but are still represented in the attribute table.
     * @param rowIndex
     * @return 
     */
    private int getActualRow(int rowIndex) {
        
        List<Integer> keyList = new ArrayList(deletedFields.keySet());
        Collections.sort(keyList);

        int actualIndex = rowIndex;
        for (int deletedRow : keyList) {
            if (deletedRow <= actualIndex) {
                actualIndex++;
            }
        }
        
        return actualIndex;
    }
    
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        
        int actualIndex = getActualRow(rowIndex);
        
        DBFField field = null;
        if (actualIndex >= attributeTable.getFieldCount() || columnIndex >= ColumnName.size) {
            // If it exists in newFields, it will be returned. Null will be returned otherwise.
            field = newFields.get(actualIndex);
            if (field == null) {
                return null;
            }
            switch (ColumnName.fromColumnIndex(columnIndex)) {
                case MODIFIED:
                    return MODIFIED_INDICATOR;
                
            }
        }
               
        if (field == null) {
            field = attributeTable.getField(actualIndex);       
            if (field == null) {
                return null;
            }
        }

        switch (ColumnName.fromColumnIndex(columnIndex)) {
            case MODIFIED:
                return NOT_MODIFIED_INDICATOR;
            case NAME:
                return field.getName();
            case TYPE:
                return field.getDataType();
            case LENGTH:
                return field.getFieldLength();
            case PRECISION:
                return field.getDecimalCount();
        }
        
        return null;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        
        ColumnName column = ColumnName.fromColumnIndex(columnIndex);
        
        if (column != null) {
            return column.getColumnClass();
        } else {
            return super.getColumnClass(columnIndex);
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        
        int actualRowIndex = getActualRow(rowIndex);
        
        DBFField row;
        if (newFields.containsKey(actualRowIndex)) {
            row = newFields.get(actualRowIndex);
        } else {
            row = attributeTable.getField(actualRowIndex);
        }
        
        ColumnName column = ColumnName.fromColumnIndex(columnIndex); 
        
        if (column != null) {
            switch (column) {
                case NAME:
                    row.setName((String)aValue);
                    break;
                case TYPE:
                    row.setDataType((DBFDataType)aValue);
                    break;
                case LENGTH:
                    row.setFieldLength((Integer)aValue);
                    break;
                case PRECISION:
                    row.setDecimalCount((Integer)aValue);
                    break;
            }
        }
        
        this.fireTableRowsUpdated(rowIndex, rowIndex);

    }
    
    /**
     * Adds a new generic field to the model. The field only exists in the model
     * and is not added to the DBF file until saved with @see commitChanges()
     */
    public void createNewField() {
        createNewField(new DBFField());
    }
    
    /**
     * Adds new field to the model. The field only exists in the model and is
     * not added to the DBF field until saved with @see commitChanges()
     * @param field 
     */
    public void createNewField(DBFField field) {
        if (field != null) {
            int index = getActualRow(getRowCount());
            newFields.put(index, field);
            fireTableRowsInserted(index, index);
        }
    }
    
    /**
     * Hides the field (row of model) specified by fieldIndex and marks the
     * field for deletion on next time changes are saved
     * @param fieldIndex 
     */
    public void deleteField(int fieldIndex) {
        if (fieldIndex < 0 || fieldIndex >= getRowCount()) {
            return;
        }
        
        int actualRow = getActualRow(fieldIndex);
        
        if (newFields.containsKey(actualRow)) {
            // Delete a field that doesn't exist in the data yet
            newFields.remove(actualRow);
        } else {
            // Hide the field and mark for deletion
            DBFField deletedField = attributeTable.getField(actualRow);
            deletedFields.put(actualRow, deletedField);
        }
        
        fireTableRowsDeleted(fieldIndex, fieldIndex);
        
    }
    
    /**
     * Saves changes to disk. Adds new fields and makes modifications to existing
     * fields
     */
    public boolean saveChanges() {
                
        // Add fields in lowest to heighest index to keep correct order in file
        List<Map.Entry<Integer, DBFField>> newEntries = new ArrayList(newFields.entrySet());
        
        Collections.sort(newEntries, new Comparator<Map.Entry<Integer, DBFField>>() {
            @Override
            public int compare(Entry<Integer, DBFField> o1, Entry<Integer, DBFField> o2) {
                // Sort in descending order
                return Integer.compare(o1.getKey(), o2.getKey());
            }
        });
        
        for (Map.Entry<Integer, DBFField> entry : newEntries) {
            try {
                attributeTable.addField(entry.getValue());
                newFields.remove(entry.getKey());
            } catch (DBFException e) {
                System.out.println(e);
            }
        }
        
        // Delete from greatest index to lowest to prevent changing indexes
        List<Map.Entry<Integer, DBFField>> deletedEntries = new ArrayList(deletedFields.entrySet());
        
        Collections.sort(deletedEntries, new Comparator<Map.Entry<Integer, DBFField>>() {

            @Override
            public int compare(Entry<Integer, DBFField> o1, Entry<Integer, DBFField> o2) {
                // Sort in descending order
                return Integer.compare(o2.getKey(), o1.getKey());
            }
        });
            
        for (Map.Entry<Integer, DBFField> entry : deletedEntries) {
            try {
                System.out.println("Deleting field from AttributeTable: " + entry.getKey() + " " + entry.getValue());
                attributeTable.deleteField(entry.getKey());
                deletedFields.remove(entry.getKey());
            } catch (DBFException e) {
                System.out.println(e);
            }
        }
        
        if (!newFields.isEmpty() || !deletedFields.isEmpty()) {
            // Some changes weren't saved
            return false;
        }
        
        return true;        
    }
    
    /**
     * Returns false if there are unsaved changes
     * @return True if all changes are saved
     */
    public boolean isSaved() {
        if (newFields.isEmpty() && deletedFields.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }
}
