package system.execution.executors;

import system.ast.AConst;
import system.ast.AExpr;
import system.ast.ColumnRef;
import system.ast.Expr;
import system.catalog.manager.CatalogManager;
import system.catalog.model.ColumnDefinition;
import system.catalog.model.TableDefinition;

import java.util.List;

public class FilterExecutor implements Executor {
    private final Executor child;
    private final Expr condition;

    private final CatalogManager catalog;
    private final TableDefinition table;

    private boolean isOpen;

    public FilterExecutor(Executor child, Expr condition, CatalogManager catalog, TableDefinition table) {
        this.child = child;
        this.condition = condition;
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

        Object rowObj;
        while ((rowObj = child.next()) != null) {
            if (passesFilter(rowObj)) return rowObj;
        }
        return null;
    }

    private boolean passesFilter(Object rowObj) {
        if (!(condition instanceof AExpr aexpr)) return false;
        if (!(aexpr.getLeft() instanceof ColumnRef leftCol)) return false;
        if (!(aexpr.getRight() instanceof AConst rightConst)) return false;

        if (!(rowObj instanceof List<?> raw)) return false;
        @SuppressWarnings("unchecked")
        List<Object> row = (List<Object>) raw;

        int idx = resolveColumnIndex(leftCol.column);
        if (idx < 0 || idx >= row.size()) return false;

        Object leftValue = row.get(idx);
        Object rightValue = rightConst.value;

        int cmp = compare(leftValue, rightValue);

        return switch (aexpr.getOp()) {
            case "="  -> cmp == 0;
            case "!=" -> cmp != 0;
            case ">"  -> cmp > 0;
            case "<"  -> cmp < 0;
            case ">=" -> cmp >= 0;
            case "<=" -> cmp <= 0;
            default -> false;
        };
    }

    private int resolveColumnIndex(String colName) {
        List<ColumnDefinition> cols = getTableColumns(table);
        for (int i = 0; i < cols.size(); i++) {
            if (cols.get(i).getName().equalsIgnoreCase(colName)) {
                return i;
            }
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    private List<ColumnDefinition> getTableColumns(TableDefinition table) {
        try {
            var m = catalog.getClass().getMethod("getTableColumns", TableDefinition.class);
            return (List<ColumnDefinition>) m.invoke(catalog, table);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot read table columns from catalog for table=" + table.getName(), e);
        }
    }

    private int compare(Object a, Object b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;

        if (a instanceof Number na && b instanceof Number nb) {
            return Long.compare(na.longValue(), nb.longValue());
        }
        if (a instanceof String sa && b instanceof String sb) {
            return sa.compareTo(sb);
        }
        if (a instanceof Boolean ba && b instanceof Boolean bb) {
            return Boolean.compare(ba, bb);
        }

        return a.toString().compareTo(b.toString());
    }

    @Override
    public void close() {
        child.close();
        isOpen = false;
    }
}
