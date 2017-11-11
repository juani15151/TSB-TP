/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package clases;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author juani
 */
public class TSBHashtableTest {
    
    private TSBHashtable<String, Integer> table;
    
    public TSBHashtableTest() {
    }
    
    @Before
    public void setUp() {
        table = new TSBHashtable<>(3, 0.2f);
        table.put("Argentina", 1);
        table.put("Brasil", 2);
        table.put("Chile", 3);
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of size method, of class TSBHashtable.
     */
    @Test
    public void testSize() {
        assertEquals(table.size(), 3);
    }

    /**
     * Test of isEmpty method, of class TSBHashtable.
     */
    @Test
    public void testIsEmpty() {
        assertFalse(table.isEmpty());
    }

    /**
     * Test of containsKey method, of class TSBHashtable.
     */
    @Test
    public void testContainsKey() {
        assertTrue(table.containsKey("Argentina"));
        assertFalse(table.containsKey("Random"));
    }

    /**
     * Test of containsValue method, of class TSBHashtable.
     */
    @Test
    public void testContainsValue() {
        assertTrue(table.containsValue(1));
        assertFalse(table.containsValue(-1));
    }

    /**
     * Test of get method, of class TSBHashtable.
     */
    @Test
    public void testGet() {
        assertEquals((int)table.get("Argentina"), 1);
    }

    /**
     * Test of put method, of class TSBHashtable.
     */
    @Test
    public void testPut() {
        table.put("Argentina", 25);
        assertEquals((int)table.get("Argentina"), 25);
        table.put("Colombia", 25);
        assertEquals((int)table.get("Colombia"), 25);
    }

    /**
     * Test of remove method, of class TSBHashtable.
     */
    @Test
    public void testRemove() {
        assertEquals((int)table.remove("Argentina"), 1);
        assertFalse(table.contains("Argentina"));
        assertNull(table.get("Argentina"));
    }

    /**
     * Test of putAll method, of class TSBHashtable.
     */
    @Test
    public void testPutAll() {
        // TODO: Hacer prueba.
    }

    /**
     * Test of clear method, of class TSBHashtable.
     */
    @Test
    public void testClear() {
        table.clear();
        assertTrue(table.isEmpty());
    }

    /**
     * Test of keySet method, of class TSBHashtable.
     */
    @Test
    public void testKeySet() {
        // TODO: Hacer prueba.
    }

    /**
     * Test of values method, of class TSBHashtable.
     */
    @Test
    public void testValues() {
        // TODO: Hacer prueba.
    }

    /**
     * Test of entrySet method, of class TSBHashtable.
     */
    @Test
    public void testEntrySet() {
        // TODO: Hacer prueba.
    }

    /**
     * Test of clone method, of class TSBHashtable.
     */
    @Test
    public void testClone() throws Exception {
        // TODO: Hacer prueba.
    }

    /**
     * Test of equals method, of class TSBHashtable.
     */
    @Test
    public void testEquals() {
        // TODO: Hacer prueba.
    }

    /**
     * Test of hashCode method, of class TSBHashtable.
     */
    @Test
    public void testHashCode() {
        // TODO: Hacer prueba.
    }

    /**
     * Test of toString method, of class TSBHashtable.
     */
    @Test
    public void testToString() {
        // TODO: Hacer prueba.
    }

    /**
     * Test of contains method, of class TSBHashtable.
     */
    @Test
    public void testContains() {
        // TODO: Hacer prueba.
    }
    
}
