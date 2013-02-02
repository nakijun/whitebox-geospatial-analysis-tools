/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package whitebox.geospatialfiles.shapefile.attributes;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URL;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Kevin Green
 */
public class AttributeTableTest {
    
    public AttributeTableTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of getFileName method, of class AttributeTable.
     */
    @Test
    public void testGetFileName() {
        System.out.println("getFileName");
        AttributeTable instance = null;
        String expResult = "";
        String result = instance.getFileName();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of isInitialized method, of class AttributeTable.
     */
    @Test
    public void testIsInitialized() {
        System.out.println("isInitialized");
        AttributeTable instance = null;
        boolean expResult = false;
        boolean result = instance.isInitialized();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getCurrentRecord method, of class AttributeTable.
     */
    @Test
    public void testGetCurrentRecord() {
        System.out.println("getCurrentRecord");
        AttributeTable instance = null;
        int expResult = 0;
        int result = instance.getCurrentRecord();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setCurrentRecord method, of class AttributeTable.
     */
    @Test
    public void testSetCurrentRecord() {
        System.out.println("setCurrentRecord");
        int currentRecord = 0;
        AttributeTable instance = null;
        instance.setCurrentRecord(currentRecord);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getCharactersetName method, of class AttributeTable.
     */
    @Test
    public void testGetCharactersetName() {
        System.out.println("getCharactersetName");
        AttributeTable instance = null;
        String expResult = "";
        String result = instance.getCharactersetName();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setCharactersetName method, of class AttributeTable.
     */
    @Test
    public void testSetCharactersetName() {
        System.out.println("setCharactersetName");
        String characterSetName = "";
        AttributeTable instance = null;
        instance.setCharactersetName(characterSetName);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getNumberOfRecords method, of class AttributeTable.
     */
    @Test
    public void testGetNumberOfRecords() {
        System.out.println("getNumberOfRecords");
        AttributeTable instance = null;
        int expResult = 0;
        int result = instance.getNumberOfRecords();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getField method, of class AttributeTable.
     */
    @Test
    public void testGetField() throws Exception {
        System.out.println("getField");
        int index = 0;
        AttributeTable instance = null;
        DBFField expResult = null;
        DBFField result = instance.getField(index);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getAllFields method, of class AttributeTable.
     */
    @Test
    public void testGetAllFields() throws Exception {
        System.out.println("getAllFields");
        AttributeTable instance = null;
        DBFField[] expResult = null;
        DBFField[] result = instance.getAllFields();
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getFieldCount method, of class AttributeTable.
     */
    @Test
    public void testGetFieldCount() throws Exception {
        System.out.println("getFieldCount");
        AttributeTable instance = null;
        int expResult = 0;
        int result = instance.getFieldCount();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getAttributeTableFieldNames method, of class AttributeTable.
     */
    @Test
    public void testGetAttributeTableFieldNames() {
        System.out.println("getAttributeTableFieldNames");
        AttributeTable instance = null;
        String[] expResult = null;
        String[] result = instance.getAttributeTableFieldNames();
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setFields method, of class AttributeTable.
     */
    @Test
    public void testSetFields() throws Exception {
        System.out.println("setFields");
        DBFField[] fields = null;
        AttributeTable instance = null;
        instance.setFields(fields);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of addField method, of class AttributeTable.
     */
    @Test
    public void testAddField_DBFField() throws Exception {
        System.out.println("addField");
        DBFField field = null;
        AttributeTable instance = null;
        instance.addField(field);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of addField method, of class AttributeTable.
     */
    @Test
    public void testAddField_DBFField_int() throws Exception {
        
        try {
            System.out.println("addField");
            DBFField field1 = new DBFField();
            field1.setName("test1");
            field1.setDataType((byte)'N');
            int insertAfter = 0;

            AttributeTable instance = new AttributeTable("test.dbf", new DBFField[] { field1 }, true);

            DBFField field2 = new DBFField();
            field2.setName("test2");
            field2.setDataType((byte)'N');
            instance.addField(field2, insertAfter);

            assertArrayEquals(new DBFField[] { field2, field1 }, instance.getAllFields());
        } finally {
            //new File("test.dbf").delete();
            //new File("test_copy.dbf").delete();
        }
    }

    /**
     * Test of deleteField method, of class AttributeTable.
     */
    @Test
    public void testDeleteField_int() {
        System.out.println("deleteField");
        int fieldNum = 0;
        AttributeTable instance = null;
        instance.deleteField(fieldNum);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of deleteField method, of class AttributeTable.
     */
    @Test
    public void testDeleteField_String() {
        System.out.println("deleteField");
        String fieldName = "";
        AttributeTable instance = null;
        instance.deleteField(fieldName);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getRecord method, of class AttributeTable.
     */
    @Test
    public void testGetRecord() throws Exception {
        System.out.println("getRecord");
        int n = 0;
        AttributeTable instance = null;
        Object[] expResult = null;
        Object[] result = instance.getRecord(n);
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getRecords method, of class AttributeTable.
     */
    @Test
    public void testGetRecords() throws Exception {
        System.out.println("getRecords");
        int startingRecord = 0;
        int endingRecord = 0;
        AttributeTable instance = null;
        Object[] expResult = null;
        Object[] result = instance.getRecords(startingRecord, endingRecord);
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of nextRecord method, of class AttributeTable.
     */
    @Test
    public void testNextRecord() throws Exception {
        System.out.println("nextRecord");
        AttributeTable instance = null;
        Object[] expResult = null;
        Object[] result = instance.nextRecord();
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of addRecord method, of class AttributeTable.
     */
    @Test
    public void testAddRecord() throws Exception {
        System.out.println("addRecord");
        Object[] values = null;
        AttributeTable instance = null;
        instance.addRecord(values);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of updateRecord method, of class AttributeTable.
     */
    @Test
    public void testUpdateRecord() throws Exception {
        System.out.println("updateRecord");
        int recordNumber = 0;
        Object[] rowData = null;
        AttributeTable instance = null;
        instance.updateRecord(recordNumber, rowData);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of deleteRecord method, of class AttributeTable.
     */
    @Test
    public void testDeleteRecord() throws Exception {
        System.out.println("deleteRecord");
        int recordNumber = 0;
        AttributeTable instance = null;
        instance.deleteRecord(recordNumber);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of write method, of class AttributeTable.
     */
    @Test
    public void testWrite() throws Exception {
        System.out.println("write");
        AttributeTable instance = null;
        instance.write();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of readHeader method, of class AttributeTable.
     */
    @Test
    public void testReadHeader() throws Exception {
        System.out.println("readHeader");
        AttributeTable instance = null;
        instance.readHeader();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of writeHeader method, of class AttributeTable.
     */
    @Test
    public void testWriteHeader() throws Exception {
        System.out.println("writeHeader");
        RandomAccessFile raf = null;
        AttributeTable instance = null;
        instance.writeHeader(raf);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
}
