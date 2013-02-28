/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package whiteboxgis;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
        return attributeTable.getFieldCount() + newFields.size();
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
        DBFField row = newFields.get(rowIndex);
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
        
        // Functionality to be re-added when edit funcationality is added 
        // (casting data when existing datatype or length is different) 
        /*
        DBFField row;
        if (newFields.containsKey(rowIndex)) {
            row = newFields.get(rowIndex);
        } else {
            row = attributeTable.getField(rowIndex);
        }
        
        // Date fields can't edit length or precision        
        if (row.getDataType() == DBFDataType.DATE) {
            if (ColumnName.fromColumnIndex(columnIndex) == ColumnName.LENGTH 
                || ColumnName.fromColumnIndex(columnIndex) == ColumnName.PRECISION) {
                return false;
            }
        }
        
        return true;
        
        */
    }
    
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        
        DBFField field = null;
        if (rowIndex >= attributeTable.getFieldCount() || columnIndex >= ColumnName.size) {
            // If it exists in newFields, it will be returned. Null will be returned otherwise.
            field = newFields.get(rowIndex);
            if (field == null) {
                return null;
            } else if (ColumnName.fromColumnIndex(columnIndex) == ColumnName.MODIFIED) {
                return MODIFIED_INDICATOR;
            }
        }
        
        if (field == null) {
            field = attributeTable.getField(rowIndex);       
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
        
        DBFField row;
        if (newFields.containsKey(rowIndex)) {
            row = newFields.get(rowIndex);
        } else {
            row = attributeTable.getField(rowIndex);
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
            newFields.put(getRowCount(), field);
            fireTableRowsInserted(getRowCount(), getRowCount());
        }
    }
    
    /**
     * Saves changes to disk. Adds new fields and makes modifications to existing
     * fields
     */
    public boolean commitChanges() {
        
        Set<Map.Entry<Integer, DBFField>> entries = newFields.entrySet();
        
        for (Iterator<Map.Entry<Integer, DBFField>> iter = entries.iterator(); iter.hasNext();) {
            Map.Entry<Integer, DBFField> entry = iter.next();
            try {
                attributeTable.addField(entry.getValue());
                iter.remove();
            } catch (DBFException e) {
                // Ignore failed insert
            }
        }
        
        if (!newFields.isEmpty()) {
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
        if (newFields.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }
}
