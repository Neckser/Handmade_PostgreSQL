package catalog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import system.buffer.DefaultBufferPoolManager;
import system.manager.HeapPageFileManager;
import system.model.TableDefinition;
import system.model.ColumnDefinition;
import system.model.TypeDefinition;
import system.replacer.ClockReplacer;
import system.catalog.CatalogManager;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CatalogManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void createAndGetTable() throws IOException {
        HeapPageFileManager pageFileManager = new HeapPageFileManager();
        DefaultBufferPoolManager bufferManager = new DefaultBufferPoolManager(10, pageFileManager, new ClockReplacer());
        CatalogManager catalog = new CatalogManager(bufferManager, pageFileManager, tempDir);
        TypeDefinition intType = catalog.getTypeByName("INT");
        TypeDefinition varcharType = catalog.getTypeByName("VARCHAR_256");
        TableDefinition tableDef = new TableDefinition(1, "users", "TABLE", "1.dat", 0);
        List<ColumnDefinition> columns = Arrays.asList(
                new ColumnDefinition(1, 1, intType.getOid(), "id", 0),
                new ColumnDefinition(2, 1, varcharType.getOid(), "name", 1),
                new ColumnDefinition(3, 1, varcharType.getOid(), "email", 2)
        );

        catalog.createTable(tableDef, columns);

        TableDefinition retrieved = catalog.getTable("users");
        assertEquals("users", retrieved.getName());
        assertEquals(0, retrieved.getPagesCount());

        List<ColumnDefinition> retrievedColumns = catalog.getTableColumns(1);
        assertEquals(3, retrievedColumns.size());
        assertEquals("id", retrievedColumns.get(0).getName());
        assertEquals("name", retrievedColumns.get(1).getName());
    }

    @Test
    void listTables() throws IOException {
        HeapPageFileManager pageFileManager = new HeapPageFileManager();
        DefaultBufferPoolManager bufferManager = new DefaultBufferPoolManager(10, pageFileManager, new ClockReplacer());
        CatalogManager catalog = new CatalogManager(bufferManager, pageFileManager, tempDir);

        TypeDefinition intType = catalog.getTypeByName("INT");
        TypeDefinition varcharType = catalog.getTypeByName("VARCHAR_256");

        catalog.createTable(
                new TableDefinition(1, "users", "TABLE", "1.dat", 0),
                Arrays.asList(new ColumnDefinition(1, 1, intType.getOid(), "id", 0))
        );

        catalog.createTable(
                new TableDefinition(2, "products", "TABLE", "2.dat", 0),
                Arrays.asList(new ColumnDefinition(2, 2, intType.getOid(), "product_id", 0))
        );

        List<String> tables = catalog.listTables();
        assertEquals(2, tables.size());
        assertTrue(tables.contains("users"));
        assertTrue(tables.contains("products"));
    }

    @Test
    void tableWithColumns() throws IOException {
        HeapPageFileManager pageFileManager = new HeapPageFileManager();
        DefaultBufferPoolManager bufferManager = new DefaultBufferPoolManager(10, pageFileManager, new ClockReplacer());
        CatalogManager catalog = new CatalogManager(bufferManager, pageFileManager, tempDir);

        TypeDefinition intType = catalog.getTypeByName("INT");
        TypeDefinition varcharType = catalog.getTypeByName("VARCHAR_256");

        catalog.createTable(
                new TableDefinition(1, "test", "TABLE", "1.dat", 0),
                Arrays.asList(
                        new ColumnDefinition(1, 1, intType.getOid(), "col1", 0),
                        new ColumnDefinition(2, 1, varcharType.getOid(), "col2", 1)
                )
        );

        CatalogManager.TableWithColumns tableWithCols = catalog.getTableWithColumns("test");
        assertNotNull(tableWithCols);
        assertEquals("test", tableWithCols.getTable().getName());
        assertEquals(2, tableWithCols.getColumns().size());
    }

    @Test
    void duplicateTableThrowsException() throws IOException {
        HeapPageFileManager pageFileManager = new HeapPageFileManager();
        DefaultBufferPoolManager bufferManager = new DefaultBufferPoolManager(10, pageFileManager, new ClockReplacer());
        CatalogManager catalog = new CatalogManager(bufferManager, pageFileManager, tempDir);

        TypeDefinition intType = catalog.getTypeByName("INT");

        catalog.createTable(
                new TableDefinition(1, "test", "TABLE", "1.dat", 0),
                Arrays.asList(new ColumnDefinition(1, 1, intType.getOid(), "id", 0))
        );

        assertThrows(IllegalArgumentException.class, () -> {
            catalog.createTable(
                    new TableDefinition(2, "test", "TABLE", "2.dat", 0),
                    Arrays.asList(new ColumnDefinition(2, 2, intType.getOid(), "id", 0))
            );
        });
    }

    @Test
    void getNonExistentTableThrows() throws IOException {
        HeapPageFileManager pageFileManager = new HeapPageFileManager();
        DefaultBufferPoolManager bufferManager = new DefaultBufferPoolManager(10, pageFileManager, new ClockReplacer());
        CatalogManager catalog = new CatalogManager(bufferManager, pageFileManager, tempDir);

        assertThrows(IllegalArgumentException.class, () -> {
            catalog.getTable("nonexistent");
        });
    }

    @Test
    void dataFileCreated() throws IOException {
        HeapPageFileManager pageFileManager = new HeapPageFileManager();
        DefaultBufferPoolManager bufferManager = new DefaultBufferPoolManager(10, pageFileManager, new ClockReplacer());
        CatalogManager catalog = new CatalogManager(bufferManager, pageFileManager, tempDir);

        TypeDefinition intType = catalog.getTypeByName("INT");

        catalog.createTable(
                new TableDefinition(1, "test", "TABLE", "test.dat", 0),
                Arrays.asList(new ColumnDefinition(1, 1, intType.getOid(), "id", 0))
        );

        Path dataFile = catalog.getTableDataFile(1);
        assertTrue(dataFile.toFile().exists());
    }

    @Test
    void updateTablePages() throws IOException {
        HeapPageFileManager pageFileManager = new HeapPageFileManager();
        DefaultBufferPoolManager bufferManager = new DefaultBufferPoolManager(10, pageFileManager, new ClockReplacer());
        CatalogManager catalog = new CatalogManager(bufferManager, pageFileManager, tempDir);

        TypeDefinition intType = catalog.getTypeByName("INT");

        catalog.createTable(
                new TableDefinition(1, "test", "TABLE", "1.dat", 0),
                Arrays.asList(new ColumnDefinition(1, 1, intType.getOid(), "id", 0))
        );

        catalog.updateTablePages(1, 5);

        TableDefinition updated = catalog.getTable("test");
        assertEquals(5, updated.getPagesCount());
    }

    @Test
    void testGetTypeByName() throws IOException {
        HeapPageFileManager pageFileManager = new HeapPageFileManager();
        DefaultBufferPoolManager bufferManager = new DefaultBufferPoolManager(10, pageFileManager, new ClockReplacer());
        CatalogManager catalog = new CatalogManager(bufferManager, pageFileManager, tempDir);

        TypeDefinition intType = catalog.getTypeByName("INT");
        TypeDefinition varcharType = catalog.getTypeByName("VARCHAR_256");

        assertNotNull(intType);
        assertNotNull(varcharType);
        assertEquals(4, intType.getByteLength());
        assertEquals(-1, varcharType.getByteLength());
    }

    @Test
    void testClearMethod() throws IOException {
        HeapPageFileManager pageFileManager = new HeapPageFileManager();
        DefaultBufferPoolManager bufferManager = new DefaultBufferPoolManager(10, pageFileManager, new ClockReplacer());
        CatalogManager catalog = new CatalogManager(bufferManager, pageFileManager, tempDir);

        TypeDefinition intType = catalog.getTypeByName("INT");

        catalog.createTable(
                new TableDefinition(1, "test", "TABLE", "1.dat", 0),
                Arrays.asList(new ColumnDefinition(1, 1, intType.getOid(), "id", 0))
        );

        assertEquals(1, catalog.listTables().size());
        catalog.clear();
        assertEquals(0, catalog.listTables().size());
        assertNotNull(catalog.getTypeByName("INT"));
        assertNotNull(catalog.getTypeByName("VARCHAR_256"));
    }
}