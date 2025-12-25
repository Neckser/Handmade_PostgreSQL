package system.catalog;

import system.buffer.DefaultBufferPoolManager;
import system.manager.HeapPageFileManager;
import system.page.HeapPage;
import system.model.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class OperationManager {
    private final CatalogManager catalogManager;
    private final DefaultBufferPoolManager bufferManager;
    private final HeapPageFileManager pageFileManager;

    public OperationManager(CatalogManager catalogManager, DefaultBufferPoolManager bufferManager,
                            HeapPageFileManager pageFileManager) {
        this.catalogManager = catalogManager;
        this.bufferManager = bufferManager;
        this.pageFileManager = pageFileManager;
    }

    public void insert(String tableName, Object... params) throws IOException {
        CatalogManager.TableWithColumns tableInfo = catalogManager.getTableWithColumns(tableName);
        TableDefinition table = tableInfo.getTable();
        List<ColumnDefinition> columns = tableInfo.getColumns();

        if (params.length != columns.size()) {
            throw new IllegalArgumentException("Parameter count mismatch");
        }

        byte[] rowData = serializeRow(params, columns);

        boolean inserted = tryInsertIntoExistingPages(table, rowData);

        if (!inserted) {
            insertIntoNewPage(table, rowData);
        }

        catalogManager.updateTablePages(table.getOid(), getPageCount(table));
    }

    public List<List<Object>> get(String tableName) throws IOException {
        CatalogManager.TableWithColumns tableInfo = catalogManager.getTableWithColumns(tableName);
        TableDefinition table = tableInfo.getTable();
        List<ColumnDefinition> columns = tableInfo.getColumns();

        List<List<Object>> result = new ArrayList<>();
        int pageCount = getPageCount(table);

        for (int pageId = 0; pageId < pageCount; pageId++) {
            HeapPage page = (HeapPage) pageFileManager.read(pageId, catalogManager.getTableDataFile(table.getOid()));
            if (page != null) {
                int slotCount = page.size();
                for (int slot = 0; slot < slotCount; slot++) {
                    byte[] rowData = page.read(slot);
                    if (rowData != null) {
                        List<Object> row = deserializeRow(rowData, columns);
                        result.add(row);
                    }
                }
            }
        }

        return result;
    }

    private byte[] serializeRow(Object[] params, List<ColumnDefinition> columns) {
        ByteBuffer buffer = ByteBuffer.allocate(calculateRowSize(params, columns)).order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < params.length; i++) {
            ColumnDefinition column = columns.get(i);
            TypeDefinition type = catalogManager.getType(column.getType0id());
            Object value = params[i];

            if (type.getByteLength() == 4) {
                buffer.putInt((Integer) value);
            } else if (type.getByteLength() == -1) {
                String str = (String) value;
                byte[] strBytes = str.getBytes(StandardCharsets.UTF_8);
                buffer.putInt(strBytes.length);
                buffer.put(strBytes);
            } else if (type.getByteLength() == 8) {
                buffer.putLong((Long) value);
            }
        }

        return buffer.array();
    }

    private List<Object> deserializeRow(byte[] rowData, List<ColumnDefinition> columns) {
        ByteBuffer buffer = ByteBuffer.wrap(rowData).order(ByteOrder.LITTLE_ENDIAN);
        List<Object> row = new ArrayList<>();

        for (ColumnDefinition column : columns) {
            TypeDefinition type = catalogManager.getType(column.getType0id());

            if (type.getByteLength() == 4) {
                row.add(buffer.getInt());
            } else if (type.getByteLength() == -1) {
                int length = buffer.getInt();
                byte[] strBytes = new byte[length];
                buffer.get(strBytes);
                row.add(new String(strBytes, StandardCharsets.UTF_8));
            } else if (type.getByteLength() == 8) {
                row.add(buffer.getLong());
            }
        }

        return row;
    }

    private int calculateRowSize(Object[] params, List<ColumnDefinition> columns) {
        int size = 0;

        for (int i = 0; i < params.length; i++) {
            ColumnDefinition column = columns.get(i);
            TypeDefinition type = catalogManager.getType(column.getType0id());

            if (type.getByteLength() > 0) {
                size += type.getByteLength();
            } else {
                String str = (String) params[i];
                size += 4 + str.getBytes(StandardCharsets.UTF_8).length;
            }
        }

        return size;
    }

    private boolean tryInsertIntoExistingPages(TableDefinition table, byte[] rowData) throws IOException {
        int pageCount = getPageCount(table);

        for (int pageId = 0; pageId < pageCount; pageId++) {
            HeapPage page = (HeapPage) pageFileManager.read(pageId, catalogManager.getTableDataFile(table.getOid()));
            if (page != null && page.getFreeSpace() >= (4 + rowData.length)) {
                page.write(rowData);
                pageFileManager.write(page, catalogManager.getTableDataFile(table.getOid()));
                return true;
            }
        }

        return false;
    }

    private void insertIntoNewPage(TableDefinition table, byte[] rowData) throws IOException {
        int pageId = getPageCount(table);
        HeapPage page = new HeapPage(pageId);
        page.write(rowData);
        pageFileManager.write(page, catalogManager.getTableDataFile(table.getOid()));
    }

    private int getPageCount(TableDefinition table) throws IOException {
        Path dataFile = catalogManager.getTableDataFile(table.getOid());
        if (!Files.exists(dataFile)) {
            return 0;
        }
        long size = Files.size(dataFile);
        return (int) (size / HeapPage.PAGE_SIZE);
    }
}