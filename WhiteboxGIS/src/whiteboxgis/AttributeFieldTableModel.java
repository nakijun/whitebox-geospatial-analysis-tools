/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package whiteboxgis;

import java.util.List;
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
    
    private enum ColumnName {
        NAME("Name"), TYPE("Type"), LENGTH("Length"), PRECISION("Precision");
        
        public static final int size = ColumnName.values().length;
        
        private static final ColumnName[] values = ColumnName.values();
        
        private final String displayName;
        
        ColumnName(String displayName) {
            this.displayName = displayName;
        }
        
        public static ColumnName fromColumnIndex(int columnIndex) {
            if (columnIndex < ColumnName.values().length) {
                return values[columnIndex];
            }
            
            return null;
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
        return attributeTable.getFieldCount();
    }

    @Override
    public int getColumnCount() {
        return ColumnName.values().length; // Currently Name, Type, Length and Precision
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
        DBFField row = attributeTable.getField(rowIndex);
        
        // Date fields can't edit length or precision        
        if (row.getDataType() == DBFDataType.DATE) {
            if (ColumnName.fromColumnIndex(columnIndex) == ColumnName.LENGTH 
                || ColumnName.fromColumnIndex(columnIndex) == ColumnName.PRECISION) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        
        if (rowIndex >= attributeTable.getFieldCount() || columnIndex >= ColumnName.size) {
            return null;
        }
        
        DBFField field = attributeTable.getField(rowIndex);
        
        switch (ColumnName.fromColumnIndex(columnIndex)) {
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
            switch (column) {
                case NAME:
                    return String.class;
                case TYPE:
                    return DBFDataType.class;
                case LENGTH:
                case PRECISION:
                    return Integer.class;
            }
        }
        
        return super.getColumnClass(columnIndex);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        DBFField row = attributeTable.getField(rowIndex);
        
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

    }
}
