package system.catalog;

import system.buffer.DefaultBufferPoolManager;
import system.manager.HeapPageFileManager;
import system.model.TableDefinition;
import system.model.ColumnDefinition;
import system.model.TypeDefinition;
import system.page.HeapPage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class CatalogManager {
    private static final String TABLE_DEFINITIONS_FILE = "table_definitions.dat";
    private static final String COLUMN_DEFINITIONS_FILE = "column_definitions.dat";
    private static final String TYPE_DEFINITIONS_FILE = "types_definitions.dat";
    private static final int PAGE_SIZE = 8192;

    private final DefaultBufferPoolManager bufferManager;
    private final HeapPageFileManager pageFileManager;
    private final Path baseDir;

    private final Map<Integer, TableDefinition> tablesById = new HashMap<>();
    private final Map<String, TableDefinition> tablesByName = new HashMap<>();
    private final Map<Integer, List<ColumnDefinition>> columnsByTableId = new HashMap<>();
    private final Map<Integer, TypeDefinition> typesById = new HashMap<>();
    private final Map<String, TypeDefinition> typesByName = new HashMap<>();

    private int nextTableOid = 1;
    private int nextColumnOid = 1;
    private int nextTypeOid = 1;

    private final Path tablesFile;
    private final Path columnsFile;
    private final Path typesFile;

    public CatalogManager(DefaultBufferPoolManager bufferManager, HeapPageFileManager pageFileManager) throws IOException {
        this(bufferManager, pageFileManager, Paths.get("."));
    }

    public CatalogManager(DefaultBufferPoolManager bufferManager, HeapPageFileManager pageFileManager, Path baseDir) throws IOException {
        this.bufferManager = Objects.requireNonNull(bufferManager, "bufferManager");
        this.pageFileManager = Objects.requireNonNull(pageFileManager, "pageFileManager");
        this.baseDir = baseDir.toAbsolutePath().normalize();

        this.tablesFile = baseDir.resolve(TABLE_DEFINITIONS_FILE);
        this.columnsFile = baseDir.resolve(COLUMN_DEFINITIONS_FILE);
        this.typesFile = baseDir.resolve(TYPE_DEFINITIONS_FILE);

        initCatalogFiles();
        loadCatalog();
        ensureBuiltinTypes();
    }

    public synchronized void createTable(TableDefinition tableDefinition, List<ColumnDefinition> columns) throws IOException {
        Objects.requireNonNull(tableDefinition, "tableDefinition");
        Objects.requireNonNull(columns, "columns");

        if (columns.isEmpty()) {
            throw new IllegalArgumentException("Table must have at least one column");
        }

        String tableName = tableDefinition.getName().toLowerCase();
        if (tablesByName.containsKey(tableName)) {
            throw new IllegalArgumentException("Table already exists: " + tableDefinition.getName());
        }

        int tableOid = tableDefinition.getOid();
        if (tableOid <= 0) {
            tableOid = nextTableOid++;
            tableDefinition = new TableDefinition(
                    tableOid,
                    tableDefinition.getName(),
                    tableDefinition.getType(),
                    tableDefinition.getFileNode(),
                    tableDefinition.getPagesCount()
            );
        }

        String fileNode = tableDefinition.getFileNode();
        if (fileNode == null || fileNode.isEmpty()) {
            fileNode = tableOid + ".dat";
            tableDefinition = new TableDefinition(
                    tableOid,
                    tableDefinition.getName(),
                    tableDefinition.getType(),
                    fileNode,
                    tableDefinition.getPagesCount()
            );
        }

        for (ColumnDefinition column : columns) {
            int typeOid = column.getType0id();
            if (!typesById.containsKey(typeOid)) {
                throw new IllegalArgumentException("Unknown type OID: " + typeOid);
            }
        }

        saveTableDefinition(tableDefinition);

        for (int i = 0; i < columns.size(); i++) {
            ColumnDefinition column = columns.get(i);
            ColumnDefinition columnWithLinks = new ColumnDefinition(
                    nextColumnOid++,
                    tableOid,
                    column.getType0id(),
                    column.getName(),
                    i
            );
            saveColumnDefinition(columnWithLinks);
        }

        Path dataFile = baseDir.resolve(fileNode);
        if (!Files.exists(dataFile)) {
            Files.createFile(dataFile);
        }
    }

    public synchronized TableDefinition getTable(String name) {
        Objects.requireNonNull(name, "name");
        TableDefinition table = tablesByName.get(name.toLowerCase());
        if (table == null) {
            throw new IllegalArgumentException("Table not found: " + name);
        }
        return table;
    }

    public synchronized TableWithColumns getTableWithColumns(String name) {
        TableDefinition table = getTable(name);
        List<ColumnDefinition> columns = columnsByTableId.getOrDefault(table.getOid(), Collections.emptyList());
        columns.sort(Comparator.comparingInt(ColumnDefinition::getPosition));
        return new TableWithColumns(table, columns);
    }

    public synchronized List<String> listTables() {
        List<String> tableNames = new ArrayList<>(tablesByName.keySet());
        Collections.sort(tableNames);
        return tableNames;
    }

    public synchronized List<ColumnDefinition> getTableColumns(int tableOid) {
        List<ColumnDefinition> columns = columnsByTableId.get(tableOid);
        if (columns == null) {
            return Collections.emptyList();
        }
        columns.sort(Comparator.comparingInt(ColumnDefinition::getPosition));
        return new ArrayList<>(columns);
    }

    public synchronized TypeDefinition getType(int oid) {
        TypeDefinition type = typesById.get(oid);
        if (type == null) {
            throw new IllegalArgumentException("Type not found: " + oid);
        }
        return type;
    }

    public synchronized TypeDefinition getTypeByName(String name) {
        TypeDefinition type = typesByName.get(name.toLowerCase());
        if (type == null) {
            throw new IllegalArgumentException("Type not found: " + name);
        }
        return type;
    }

    public synchronized void addType(TypeDefinition type) throws IOException {
        Objects.requireNonNull(type, "type");

        if (typesByName.containsKey(type.getName().toLowerCase())) {
            throw new IllegalArgumentException("Type already exists: " + type.getName());
        }

        int typeOid = type.getOid();
        if (typeOid <= 0) {
            typeOid = nextTypeOid++;
            type = new TypeDefinition(typeOid, type.getName(), type.getByteLength());
        }

        saveTypeDefinition(type);
    }

    public synchronized void updateTablePages(int tableOid, int newPagesCount) throws IOException {
        TableDefinition table = tablesById.get(tableOid);
        if (table == null) {
            throw new IllegalArgumentException("Table not found: " + tableOid);
        }

        TableDefinition updated = new TableDefinition(
                table.getOid(),
                table.getName(),
                table.getType(),
                table.getFileNode(),
                newPagesCount
        );

        tablesById.put(tableOid, updated);
        tablesByName.put(table.getName().toLowerCase(), updated);

        rewriteTablesFile();
    }

    private void rewriteTablesFile() throws IOException {
        Files.write(tablesFile, new byte[0]);

        for (TableDefinition table : tablesById.values()) {
            byte[] data = table.toBytes();
            writeRecordToFile(tablesFile, data);
        }
    }

    public synchronized Path getTableDataFile(int tableOid) {
        TableDefinition table = tablesById.get(tableOid);
        if (table == null) {
            throw new IllegalArgumentException("Table not found: " + tableOid);
        }
        return baseDir.resolve(table.getFileNode());
    }

    private void initCatalogFiles() throws IOException {
        ensureFile(tablesFile);
        ensureFile(columnsFile);
        ensureFile(typesFile);
    }

    private void ensureFile(Path file) throws IOException {
        if (!Files.exists(file)) {
            Files.createFile(file);
        }
    }

    private int getPageCount(Path file) throws IOException {
        if (!Files.exists(file)) {
            return 0;
        }
        long size = Files.size(file);
        return (int) (size / PAGE_SIZE);
    }

    private HeapPage readPage(Path file, int pageId) throws IOException {
        try {
            return (HeapPage) pageFileManager.read(pageId, file);
        } catch (IllegalArgumentException e) {
            return null;
        } catch (Exception e) {
            throw new IOException("Failed to read page " + pageId + " from " + file, e);
        }
    }

    private void writePage(Path file, int pageId, HeapPage page) throws IOException {
        try {
            pageFileManager.write(page, file);
        } catch (Exception e) {
            throw new IOException("Failed to write page " + pageId + " to " + file, e);
        }
    }

    private void writeRecordToFile(Path file, byte[] recordData) throws IOException {
        int pageCount = getPageCount(file);
        HeapPage targetPage = null;
        int targetPageId = -1;

        for (int pageId = 0; pageId < pageCount; pageId++) {
            try {
                HeapPage page = readPage(file, pageId);
                if (page != null) {
                    int freeSpace = page.getFreeSpace();
                    if (freeSpace >= (4 + recordData.length)) {
                        targetPage = page;
                        targetPageId = pageId;
                        break;
                    }
                }
            } catch (Exception e) {
                continue;
            }
        }

        if (targetPage == null) {
            targetPageId = pageCount;
            targetPage = new HeapPage(targetPageId);
        }

        targetPage.write(recordData);
        writePage(file, targetPageId, targetPage);
    }

    private void loadCatalog() throws IOException {
        loadTypes();
        loadTables();
        loadColumns();

        nextTableOid = tablesById.keySet().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
        nextTypeOid = typesById.keySet().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;

        int maxColumnOid = columnsByTableId.values().stream()
                .flatMap(List::stream)
                .mapToInt(ColumnDefinition::getOid)
                .max()
                .orElse(0);
        nextColumnOid = maxColumnOid + 1;
    }

    private void loadTypes() throws IOException {
        if (!Files.exists(typesFile) || Files.size(typesFile) == 0) {
            return;
        }

        int pageCount = getPageCount(typesFile);
        for (int pageId = 0; pageId < pageCount; pageId++) {
            HeapPage page = readPage(typesFile, pageId);
            if (page == null) continue;

            int slotCount = page.size();
            for (int slot = 0; slot < slotCount; slot++) {
                byte[] recordData = page.read(slot);
                if (recordData != null && recordData.length > 0) {
                    TypeDefinition type = TypeDefinition.fromBytes(recordData);
                    typesById.put(type.getOid(), type);
                    typesByName.put(type.getName().toLowerCase(), type);
                }
            }
        }
    }

    private void loadTables() throws IOException {
        if (!Files.exists(tablesFile) || Files.size(tablesFile) == 0) {
            return;
        }

        int pageCount = getPageCount(tablesFile);
        for (int pageId = 0; pageId < pageCount; pageId++) {
            HeapPage page = readPage(tablesFile, pageId);
            if (page == null) continue;

            int slotCount = page.size();
            for (int slot = 0; slot < slotCount; slot++) {
                byte[] recordData = page.read(slot);
                if (recordData != null && recordData.length > 0) {
                    TableDefinition table = TableDefinition.fromBytes(recordData);
                    tablesById.put(table.getOid(), table);
                    tablesByName.put(table.getName().toLowerCase(), table);
                }
            }
        }
    }

    private void loadColumns() throws IOException {
        if (!Files.exists(columnsFile) || Files.size(columnsFile) == 0) {
            return;
        }

        int pageCount = getPageCount(columnsFile);
        for (int pageId = 0; pageId < pageCount; pageId++) {
            HeapPage page = readPage(columnsFile, pageId);
            if (page == null) continue;

            int slotCount = page.size();
            for (int slot = 0; slot < slotCount; slot++) {
                byte[] recordData = page.read(slot);
                if (recordData != null && recordData.length > 0) {
                    ColumnDefinition column = ColumnDefinition.fromBytes(recordData);
                    columnsByTableId
                            .computeIfAbsent(column.getTableOid(), k -> new ArrayList<>())
                            .add(column);
                }
            }
        }
    }

    private void saveTableDefinition(TableDefinition table) throws IOException {
        byte[] data = table.toBytes();
        writeRecordToFile(tablesFile, data);

        tablesById.put(table.getOid(), table);
        tablesByName.put(table.getName().toLowerCase(), table);

        if (table.getOid() >= nextTableOid) {
            nextTableOid = table.getOid() + 1;
        }
    }

    private void saveColumnDefinition(ColumnDefinition column) throws IOException {
        byte[] data = column.toBytes();
        writeRecordToFile(columnsFile, data);

        columnsByTableId
                .computeIfAbsent(column.getTableOid(), k -> new ArrayList<>())
                .add(column);

        if (column.getOid() >= nextColumnOid) {
            nextColumnOid = column.getOid() + 1;
        }
    }

    private void saveTypeDefinition(TypeDefinition type) throws IOException {
        byte[] data = type.toBytes();
        writeRecordToFile(typesFile, data);

        typesById.put(type.getOid(), type);
        typesByName.put(type.getName().toLowerCase(), type);

        if (type.getOid() >= nextTypeOid) {
            nextTypeOid = type.getOid() + 1;
        }
    }

    private void ensureBuiltinTypes() throws IOException {
        ensureType("INT", 4);
        ensureType("VARCHAR_256", -1);
    }

    private void ensureType(String name, int byteLength) throws IOException {
        if (typesByName.containsKey(name.toLowerCase())) {
            return;
        }

        TypeDefinition type = new TypeDefinition(nextTypeOid++, name, byteLength);
        saveTypeDefinition(type);
    }

    public synchronized void clear() throws IOException {
        tablesById.clear();
        tablesByName.clear();
        columnsByTableId.clear();
        typesById.clear();
        typesByName.clear();

        nextTableOid = 1;
        nextColumnOid = 1;
        nextTypeOid = 1;

        Files.write(tablesFile, new byte[0]);
        Files.write(columnsFile, new byte[0]);
        Files.write(typesFile, new byte[0]);

        ensureBuiltinTypes();
    }

    public synchronized String getStats() {
        return String.format(
                "Catalog Stats: Tables=%d, Types=%d, TotalColumns=%d",
                tablesById.size(),
                typesById.size(),
                columnsByTableId.values().stream().mapToInt(List::size).sum()
        );
    }

    public static class TableWithColumns {
        private final TableDefinition table;
        private final List<ColumnDefinition> columns;

        public TableWithColumns(TableDefinition table, List<ColumnDefinition> columns) {
            this.table = table;
            this.columns = Collections.unmodifiableList(new ArrayList<>(columns));
        }

        public TableDefinition getTable() { return table; }
        public List<ColumnDefinition> getColumns() { return columns; }

        @Override
        public String toString() {
            return "TableWithColumns{table=" + table + ", columns=" + columns + "}";
        }
    }
}