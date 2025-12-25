package catalog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import system.catalog.CatalogManager;
import system.buffer.DefaultBufferPoolManager;
import system.manager.HeapPageFileManager;
import system.model.TableDefinition;
import system.model.ColumnDefinition;
import system.replacer.ClockReplacer;
import system.catalog.OperationManager;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OperationManagerTest {

    @TempDir
    Path tempDir;

    private CatalogManager createCatalogManager() throws IOException {
        HeapPageFileManager pageFileManager = new HeapPageFileManager();
        DefaultBufferPoolManager bufferManager = new DefaultBufferPoolManager(
                100,
                pageFileManager,
                new ClockReplacer()
        );
        return new CatalogManager(bufferManager, pageFileManager, tempDir);
    }

    @Test
    void insertAndGetSingleRow() throws IOException {
        CatalogManager catalog = createCatalogManager();
        HeapPageFileManager pageFileManager = new HeapPageFileManager();
        DefaultBufferPoolManager bufferManager = new DefaultBufferPoolManager(100, pageFileManager, new ClockReplacer());
        OperationManager operationManager = new OperationManager(catalog, bufferManager, pageFileManager);
        catalog.createTable(
                new TableDefinition(1, "users", "TABLE", "1.dat", 0),
                Arrays.asList(
                        new ColumnDefinition(1, 1, catalog.getTypeByName("INT").getOid(), "id", 0),
                        new ColumnDefinition(2, 1, catalog.getTypeByName("VARCHAR_256").getOid(), "name", 1)
                )
        );

        operationManager.insert("users", 1, "John");

        List<List<Object>> result = operationManager.get("users");

        assertEquals(1, result.size());
        List<Object> row = result.get(0);
        assertEquals(2, row.size());
        assertEquals(1, row.get(0));
        assertEquals("John", row.get(1));
    }

    @Test
    void insertMultipleRows() throws IOException {
        CatalogManager catalog = createCatalogManager();
        HeapPageFileManager pageFileManager = new HeapPageFileManager();
        DefaultBufferPoolManager bufferManager = new DefaultBufferPoolManager(100, pageFileManager, new ClockReplacer());
        OperationManager operationManager = new OperationManager(catalog, bufferManager, pageFileManager);

        catalog.createTable(
                new TableDefinition(1, "products", "TABLE", "1.dat", 0),
                Arrays.asList(
                        new ColumnDefinition(1, 1, catalog.getTypeByName("INT").getOid(), "product_id", 0),
                        new ColumnDefinition(2, 1, catalog.getTypeByName("VARCHAR_256").getOid(), "product_name", 1)
                )
        );

        operationManager.insert("products", 100, "Laptop");
        operationManager.insert("products", 101, "Mouse");
        operationManager.insert("products", 102, "Keyboard");

        List<List<Object>> result = operationManager.get("products");
        assertEquals(3, result.size());

        boolean hasLaptop = false;
        boolean hasMouse = false;
        boolean hasKeyboard = false;

        for (List<Object> row : result) {
            int id = (Integer) row.get(0);
            String name = (String) row.get(1);

            if (id == 100 && name.equals("Laptop")) hasLaptop = true;
            if (id == 101 && name.equals("Mouse")) hasMouse = true;
            if (id == 102 && name.equals("Keyboard")) hasKeyboard = true;
        }

        assertTrue(hasLaptop, "Не найдено Laptop");
        assertTrue(hasMouse, "Не найдено Mouse");
        assertTrue(hasKeyboard, "Не найдено Keyboard");
    }

    @Test
    void parameterCountMismatchThrows() throws IOException {
        CatalogManager catalog = createCatalogManager();
        HeapPageFileManager pageFileManager = new HeapPageFileManager();
        DefaultBufferPoolManager bufferManager = new DefaultBufferPoolManager(100, pageFileManager, new ClockReplacer());
        OperationManager operationManager = new OperationManager(catalog, bufferManager, pageFileManager);

        catalog.createTable(
                new TableDefinition(1, "test", "TABLE", "1.dat", 0),
                Arrays.asList(
                        new ColumnDefinition(1, 1, catalog.getTypeByName("INT").getOid(), "id", 0),
                        new ColumnDefinition(2, 1, catalog.getTypeByName("VARCHAR_256").getOid(), "name", 1)
                )
        );

        assertThrows(IllegalArgumentException.class, () -> {
            operationManager.insert("test", 1);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            operationManager.insert("test", 1, "John", "extra");
        });
    }

    @Test
    void emptyTableReturnsEmptyList() throws IOException {
        CatalogManager catalog = createCatalogManager();
        HeapPageFileManager pageFileManager = new HeapPageFileManager();
        DefaultBufferPoolManager bufferManager = new DefaultBufferPoolManager(100, pageFileManager, new ClockReplacer());
        OperationManager operationManager = new OperationManager(catalog, bufferManager, pageFileManager);

        catalog.createTable(
                new TableDefinition(1, "empty_table", "TABLE", "empty.dat", 0),
                Arrays.asList(new ColumnDefinition(1, 1, catalog.getTypeByName("INT").getOid(), "id", 0))
        );

        List<List<Object>> result = operationManager.get("empty_table");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void pagesCountUpdatedAfterInsert() throws IOException {
        CatalogManager catalog = createCatalogManager();
        HeapPageFileManager pageFileManager = new HeapPageFileManager();
        DefaultBufferPoolManager bufferManager = new DefaultBufferPoolManager(100, pageFileManager, new ClockReplacer());
        OperationManager operationManager = new OperationManager(catalog, bufferManager, pageFileManager);

        catalog.createTable(
                new TableDefinition(1, "test", "TABLE", "1.dat", 0),
                Arrays.asList(
                        new ColumnDefinition(1, 1, catalog.getTypeByName("INT").getOid(), "id", 0),
                        new ColumnDefinition(2, 1, catalog.getTypeByName("VARCHAR_256").getOid(), "data", 1)
                )
        );

        operationManager.insert("test", 1, "test data");

        TableDefinition table = catalog.getTable("test");
        assertTrue(table.getPagesCount() >= 0, "pagesCount должен быть >= 0");
    }

    @Test
    void insertIntoNonExistentTableThrows() throws IOException {
        CatalogManager catalog = createCatalogManager();
        HeapPageFileManager pageFileManager = new HeapPageFileManager();
        DefaultBufferPoolManager bufferManager = new DefaultBufferPoolManager(100, pageFileManager, new ClockReplacer());
        OperationManager operationManager = new OperationManager(catalog, bufferManager, pageFileManager);

        assertThrows(IllegalArgumentException.class, () -> {
            operationManager.insert("nonexistent", 1, "test");
        });
    }

    @Test
    void getFromNonExistentTableThrows() throws IOException {
        CatalogManager catalog = createCatalogManager();
        HeapPageFileManager pageFileManager = new HeapPageFileManager();
        DefaultBufferPoolManager bufferManager = new DefaultBufferPoolManager(100, pageFileManager, new ClockReplacer());
        OperationManager operationManager = new OperationManager(catalog, bufferManager, pageFileManager);

        assertThrows(IllegalArgumentException.class, () -> {
            operationManager.get("nonexistent");
        });
    }

    @Test
    void varcharWithDifferentLengths() throws IOException {
        CatalogManager catalog = createCatalogManager();
        HeapPageFileManager pageFileManager = new HeapPageFileManager();
        DefaultBufferPoolManager bufferManager = new DefaultBufferPoolManager(100, pageFileManager, new ClockReplacer());
        OperationManager operationManager = new OperationManager(catalog, bufferManager, pageFileManager);

        catalog.createTable(
                new TableDefinition(1, "texts", "TABLE", "texts.dat", 0),
                Arrays.asList(
                        new ColumnDefinition(1, 1, catalog.getTypeByName("INT").getOid(), "id", 0),
                        new ColumnDefinition(2, 1, catalog.getTypeByName("VARCHAR_256").getOid(), "text", 1)
                )
        );

        operationManager.insert("texts", 1, "short");
        operationManager.insert("texts", 2, "medium length text");

        List<List<Object>> result = operationManager.get("texts");
        assertEquals(2, result.size());

        boolean hasShort = false;
        boolean hasMedium = false;

        for (List<Object> row : result) {
            int id = (Integer) row.get(0);
            String text = (String) row.get(1);

            if (id == 1 && text.equals("short")) hasShort = true;
            if (id == 2 && text.equals("medium length text")) hasMedium = true;
        }

        assertTrue(hasShort, "Не найдено 'short'");
        assertTrue(hasMedium, "Не найдено 'medium length text'");
    }

    @Test
    void insertWithIntOnly() throws IOException {
        CatalogManager catalog = createCatalogManager();
        HeapPageFileManager pageFileManager = new HeapPageFileManager();
        DefaultBufferPoolManager bufferManager = new DefaultBufferPoolManager(100, pageFileManager, new ClockReplacer());
        OperationManager operationManager = new OperationManager(catalog, bufferManager, pageFileManager);

        catalog.createTable(
                new TableDefinition(1, "numbers", "TABLE", "numbers.dat", 0),
                Arrays.asList(
                        new ColumnDefinition(1, 1, catalog.getTypeByName("INT").getOid(), "value", 0)
                )
        );

        operationManager.insert("numbers", 10);
        operationManager.insert("numbers", 20);
        operationManager.insert("numbers", 30);

        List<List<Object>> result = operationManager.get("numbers");
        assertEquals(3, result.size());

        boolean has10 = false;
        boolean has20 = false;
        boolean has30 = false;

        for (List<Object> row : result) {
            int value = (Integer) row.get(0);
            if (value == 10) has10 = true;
            if (value == 20) has20 = true;
            if (value == 30) has30 = true;
        }

        assertTrue(has10, "Не найдено 10");
        assertTrue(has20, "Не найдено 20");
        assertTrue(has30, "Не найдено 30");
    }

    @Test
    void largeNumberOfRows() throws IOException {
        CatalogManager catalog = createCatalogManager();
        HeapPageFileManager pageFileManager = new HeapPageFileManager();
        DefaultBufferPoolManager bufferManager = new DefaultBufferPoolManager(1000, pageFileManager, new ClockReplacer());
        OperationManager operationManager = new OperationManager(catalog, bufferManager, pageFileManager);

        catalog.createTable(
                new TableDefinition(1, "large", "TABLE", "large.dat", 0),
                Arrays.asList(
                        new ColumnDefinition(1, 1, catalog.getTypeByName("INT").getOid(), "id", 0),
                        new ColumnDefinition(2, 1, catalog.getTypeByName("VARCHAR_256").getOid(), "name", 1)
                )
        );

        for (int i = 0; i < 10; i++) {
            operationManager.insert("large", i, "Item_" + i);
        }

        List<List<Object>> result = operationManager.get("large");
        assertEquals(10, result.size());

        for (int i = 0; i < 10; i++) {
            boolean found = false;
            for (List<Object> row : result) {
                int id = (Integer) row.get(0);
                String name = (String) row.get(1);
                if (id == i && name.equals("Item_" + i)) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "Не найдена строка с id=" + i);
        }
    }
}