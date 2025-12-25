package model;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import system.model.TableDefinition;

import static org.junit.jupiter.api.Assertions.*;

class TableDefinitionTest {

    @Test
    void tableDefinition_constructorAndGetters() {
        TableDefinition table = new TableDefinition(1, "users", "TABLE", "users.dat", 5);
        assertEquals(1, table.getOid());
        assertEquals("users", table.getName());
        assertEquals("TABLE", table.getType());
        assertEquals("users.dat", table.getFileNode());
        assertEquals(5, table.getPagesCount());
    }

    @Test
    void tableDefinition_setPagesCount() {
        TableDefinition table = new TableDefinition(1, "users", "TABLE", "users.dat", 5);
        table.setPagesCount(10);
        assertEquals(10, table.getPagesCount());
        table.setPagesCount(0);
        assertEquals(0, table.getPagesCount());
    }

    @Test
    void tableDefinition_serializationRoundTrip() {
        TableDefinition original = new TableDefinition(123, "test_table", "HEAP", "123.dat", 42);
        byte[] bytes = original.toBytes();
        TableDefinition restored = TableDefinition.fromBytes(bytes);
        assertEquals(original.getOid(), restored.getOid());
        assertEquals(original.getName(), restored.getName());
        assertEquals(original.getType(), restored.getType());
        assertEquals(original.getFileNode(), restored.getFileNode());
        assertEquals(original.getPagesCount(), restored.getPagesCount());
    }

    @Test
    void tableDefinition_serializationWithUnicode() {
        TableDefinition original = new TableDefinition(1, "таблица", "ТИП", "файл.dat", 10);
        byte[] bytes = original.toBytes();
        TableDefinition restored = TableDefinition.fromBytes(bytes);
        assertEquals("таблица", restored.getName());
        assertEquals("ТИП", restored.getType());
        assertEquals("файл.dat", restored.getFileNode());
    }

    @Test
    void tableDefinition_serializationWithEmptyStrings() {
        TableDefinition original = new TableDefinition(1, "", "", "", 0);
        byte[] bytes = original.toBytes();
        TableDefinition restored = TableDefinition.fromBytes(bytes);
        assertEquals("", restored.getName());
        assertEquals("", restored.getType());
        assertEquals("", restored.getFileNode());
    }

    @Test
    void tableDefinition_serializationExtremeValues() {
        TableDefinition original = new TableDefinition(
                Integer.MAX_VALUE,
                "long_name",
                "special_type",
                "file_norm.dat",
                Integer.MAX_VALUE
        );

        byte[] bytes = original.toBytes();
        TableDefinition restored = TableDefinition.fromBytes(bytes);
        assertEquals(Integer.MAX_VALUE, restored.getOid());
        assertEquals("long_name", restored.getName());
        assertEquals("special_type", restored.getType());
        assertEquals("file_norm.dat", restored.getFileNode());
        assertEquals(Integer.MAX_VALUE, restored.getPagesCount());
    }

    @Test
    void tableDefinition_serializationConsistentLength() {
        TableDefinition table1 = new TableDefinition(1, "a", "b", "c", 1);
        TableDefinition table2 = new TableDefinition(999, "long_name", "long_type", "long_file.dat", 999);
        byte[] bytes1 = table1.toBytes();
        byte[] bytes2 = table2.toBytes();
        assertNotNull(bytes1);
        assertNotNull(bytes2);
        assertTrue(bytes1.length > 0);
        assertTrue(bytes2.length > 0);
        assertDoesNotThrow(() -> TableDefinition.fromBytes(bytes1));
        assertDoesNotThrow(() -> TableDefinition.fromBytes(bytes2));
    }
}