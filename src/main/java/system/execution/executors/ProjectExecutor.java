package system.execution.executors;

import system.ast.TargetEntry;
import system.ast.ColumnRef;
import system.ast.AConst;
import system.catalog.manager.CatalogManager;
import system.catalog.manager.DefaultCatalogManager;
import system.catalog.model.ColumnDefinition;
import system.catalog.model.TableDefinition;

import java.util.ArrayList;
import java.util.List;

public class ProjectExecutor implements Executor {
    private final Executor child;
    private final List<TargetEntry> targetList;
    private boolean isOpen;
    private final TableDefinition table;
    private final CatalogManager catalog;

    public ProjectExecutor(Executor child, List<TargetEntry> targetList,
                           CatalogManager catalog, TableDefinition table) {
        this.child = child;
        this.targetList = targetList;
        this.catalog = catalog;
        this.table = table;
    }

    @Override
    public void open() {
        child.open();
        isOpen = true;
    }

    @Override
    public Object next() {
        if (!isOpen) return null;

        Object rowObj = child.next();
        if (rowObj == null) return null;

        if (!(rowObj instanceof List<?> rawRow)) {
            throw new IllegalStateException("ProjectExecutor expected List<Object> from child, got: " + rowObj.getClass());
        }

        @SuppressWarnings("unchecked")
        List<Object> row = (List<Object>) rawRow;

        if (targetList == null || targetList.isEmpty()) {
            return row;
        }

        for (TargetEntry t : targetList) {
            if (t.expr instanceof ColumnRef col && "*".equals(col.column)) {
                return row;
            }
        }

        List<Object> result = new ArrayList<>(targetList.size());
        for (TargetEntry t : targetList) {
            if (t.expr instanceof ColumnRef col) {
                int idx = getFieldIndex(col.column);
                if (idx < 0) throw new IllegalArgumentException("Unknown column: " + col.column);
                result.add(row.get(idx));
            } else if (t.expr instanceof AConst c) {
                result.add(c.value);
            }
        }

        if (result.size() == 1) return result.get(0);
        return result;
    }


    private int getFieldIndex(String columnName) {
        if (table == null) return -1;

        if (catalog instanceof DefaultCatalogManager cm) {
            var cols = cm.getTableColumns(table);
            for (int i = 0; i < cols.size(); i++) {
                if (cols.get(i).getName().equalsIgnoreCase(columnName)) return i;
            }
            return -1;
        }

        try {
            var m = catalog.getClass().getMethod("getTableColumns", TableDefinition.class);
            @SuppressWarnings("unchecked")
            var cols = (List<ColumnDefinition>) m.invoke(catalog, table);
            for (int i = 0; i < cols.size(); i++) {
                if (cols.get(i).getName().equalsIgnoreCase(columnName)) return i;
            }
            return -1;
        } catch (Exception e) {
            return -1;
        }
    }



    @Override
    public void close() {
        child.close();
        isOpen = false;
    }
}
