package model;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import system.model.ColumnDefinition;

import static org.junit.jupiter.api.Assertions.*;

class ColumnDefinitionTest {

    @Test
    void columnDefinition_constructorAndGetters() {
        ColumnDefinition column = new ColumnDefinition(1, 100, 200, "username", 2);
        assertEquals(1, column.getOid());
        assertEquals(100, column.getTableOid());
        assertEquals(200, column.getType0id());
        assertEquals("username", column.getName());
        assertEquals(2, column.getPosition());
    }

    @Test
    void columnDefinition_serializationRoundTrip() {
        ColumnDefinition original = new ColumnDefinition(456, 123, 789, "email_column", 3);
        byte[] bytes = original.toBytes();
        ColumnDefinition restored = original.fromBytes(bytes);
        assertEquals(original.getOid(), restored.getOid());
        assertEquals(original.getTableOid(), restored.getTableOid());
        assertEquals(original.getType0id(), restored.getType0id());
        assertEquals(original.getName(), restored.getName());
        assertEquals(original.getPosition(), restored.getPosition());
    }

    @Test
    void columnDefinition_serializationWithUnicode() {
        ColumnDefinition original = new ColumnDefinition(1, 2, 3, "столбец", 0);
        byte[] bytes = original.toBytes();
        ColumnDefinition restored = original.fromBytes(bytes);
        assertEquals("столбец", restored.getName());
    }

    @Test
    void columnDefinition_serializationWithEmptyName() {
        ColumnDefinition original = new ColumnDefinition(1, 2, 3, "", 0);
        byte[] bytes = original.toBytes();
        ColumnDefinition restored = original.fromBytes(bytes);
        assertEquals("", restored.getName());
    }

    @Test
    void columnDefinition_serializationExtremeValues() {
        ColumnDefinition original = new ColumnDefinition(
                Integer.MAX_VALUE,
                Integer.MAX_VALUE,
                Integer.MAX_VALUE,
                "long_name",
                Integer.MAX_VALUE
        );

        byte[] bytes = original.toBytes();
        ColumnDefinition restored = original.fromBytes(bytes);
        assertEquals(Integer.MAX_VALUE, restored.getOid());
        assertEquals(Integer.MAX_VALUE, restored.getTableOid());
        assertEquals(Integer.MAX_VALUE, restored.getType0id());
        assertEquals("long_name", restored.getName());
        assertEquals(Integer.MAX_VALUE, restored.getPosition());
    }

    @Test
    void columnDefinition_serializationBoundaryPositions() {
        ColumnDefinition posZero = new ColumnDefinition(1, 2, 3, "first", 0);
        ColumnDefinition posMax = new ColumnDefinition(2, 2, 3, "last", Integer.MAX_VALUE);
        byte[] bytesZero = posZero.toBytes();
        byte[] bytesMax = posMax.toBytes();
        ColumnDefinition restoredZero = posZero.fromBytes(bytesZero);
        ColumnDefinition restoredMax = posMax.fromBytes(bytesMax);
        assertEquals(0, restoredZero.getPosition());
        assertEquals(Integer.MAX_VALUE, restoredMax.getPosition());
    }

    @Test
    void columnDefinition_multipleInstancesIndependent() {
        ColumnDefinition col1 = new ColumnDefinition(1, 10, 100, "col1", 0);
        ColumnDefinition col2 = new ColumnDefinition(2, 10, 100, "col2", 1);
        byte[] bytes1 = col1.toBytes();
        byte[] bytes2 = col2.toBytes();
        ColumnDefinition restored1 = col1.fromBytes(bytes1);
        ColumnDefinition restored2 = col2.fromBytes(bytes2);
        assertEquals("col1", restored1.getName());
        assertEquals("col2", restored2.getName());
        assertEquals(0, restored1.getPosition());
        assertEquals(1, restored2.getPosition());
    }
}