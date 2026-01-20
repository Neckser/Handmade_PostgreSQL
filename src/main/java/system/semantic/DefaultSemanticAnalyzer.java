package system.semantic;

import system.catalog.manager.CatalogManager;
import system.catalog.model.ColumnDefinition;
import system.catalog.model.TableDefinition;
import system.catalog.model.TypeDefinition;
import system.parser.nodes.*;

import java.util.*;

public class DefaultSemanticAnalyzer implements SemanticAnalyzer {

    @Override
    public QueryTree analyze(AstNode ast, CatalogManager catalog) {
        if (ast instanceof SelectStmt ss) {
            return analyzeSelect(ss, catalog);
        }
        if (ast instanceof CreateStmt cs) {
            return analyzeCreate(cs, catalog);
        }
        if (ast instanceof UpdateStmt us) {
            return analyzeUpdate(us, catalog);
        }
        throw new IllegalArgumentException("Unsupported statement: " + ast.getClass().getSimpleName());
    }

    private QueryTree analyzeSelect(SelectStmt stmt, CatalogManager catalog) {
        List<String> fromTables = new ArrayList<>();
        for (RangeVar rv : stmt.getFromClause()) {
            fromTables.add(rv.getRelName());
        }

        List<ResolvedColumn> targetColumns = new ArrayList<>();
        for (ResTarget target : stmt.getTargetList()) {
            ColumnRef colRef = target.getVal();
            ResolvedColumn resolved = resolveColumn(colRef, fromTables, catalog);
            targetColumns.add(resolved);
        }

        Filter filter = null;
        if (stmt.getWhereClause() != null) {
            if (!(stmt.getWhereClause() instanceof AExpr expr))
                throw new RuntimeException("Expected expression in WHERE");

            if (!(expr.getLeft() instanceof ColumnRef leftRef))
                throw new RuntimeException("Left part WHERE must be a column");

            ResolvedColumn leftCol = resolveColumn(leftRef, fromTables, catalog);

            Object rightVal;
            TypeDefinition rightType;

            if (expr.getRight() instanceof ColumnRef rightRef) {
                ResolvedColumn rightCol = resolveColumn(rightRef, fromTables, catalog);
                rightVal = rightCol.getQualifiedName();
                rightType = rightCol.getType();
            } else if (expr.getRight() instanceof AConst c) {
                rightVal = c.getValue();
                rightType = catalog.getType(c.getType());
                if (rightType == null)
                    throw new RuntimeException("Unknown type: " + c.getType());
            } else {
                throw new RuntimeException("Unsupported node in WHERE");
            }

            if (!areCompatible(leftCol.getType(), rightType)) {
                throw new RuntimeException("Incompatible types: " +
                        leftCol.getType().getName() + " vs " + rightType.getName());
            }

            filter = new Filter(expr.getOp(), leftCol, rightVal, rightType);
        }

        return new QueryTree(targetColumns, fromTables, filter);
    }

    private QueryTree analyzeCreate(CreateStmt stmt, CatalogManager catalog) {
        String tableName = stmt.getTableName();

        List<ColumnDefinition> cols = new ArrayList<>();
        int i = 0;
        for (ColumnDef cd : stmt.getColumns()) {
            TypeDefinition type = catalog.getType(cd.getType());
            if (type == null) {
                throw new RuntimeException("Unknown type: " + cd.getType());
            }
            cols.add(new ColumnDefinition(type.getOid(), cd.getName(), i++));
        }

        catalog.createTable(tableName, cols);

        return new QueryTree("CREATE", tableName, cols);
    }

    private QueryTree analyzeUpdate(UpdateStmt stmt, CatalogManager catalog) {
        String tableName = stmt.getTableName();
        TableDefinition table = catalog.getTable(tableName);
        if (table == null) {
            throw new RuntimeException("Table not found: " + tableName);
        }

        String columnName = stmt.getColumn();
        ColumnDefinition targetCol = catalog.getColumn(table, columnName);
        if (targetCol == null) {
            throw new RuntimeException("Column not found: " + columnName);
        }

        if (!(stmt.getNewValue() instanceof AConst cons)) {
            throw new RuntimeException("Only constant values are supported in UPDATE SET");
        }

        Filter filter = null;
        if (stmt.getWhereClause() != null) {
            if (!(stmt.getWhereClause() instanceof AExpr expr))
                throw new RuntimeException("Expected expression in WHERE");
            if (!(expr.getLeft() instanceof ColumnRef leftRef))
                throw new RuntimeException("Left part WHERE must be a column");

            ResolvedColumn leftCol = resolveColumn(leftRef, List.of(tableName), catalog);

            Object rightVal;
            TypeDefinition rightType;

            if (expr.getRight() instanceof ColumnRef rightRef) {
                ResolvedColumn rightCol = resolveColumn(rightRef, List.of(tableName), catalog);
                rightVal = rightCol.getQualifiedName();
                rightType = rightCol.getType();
            } else if (expr.getRight() instanceof AConst c) {
                rightVal = c.getValue();
                rightType = catalog.getType(c.getType());
                if (rightType == null)
                    throw new RuntimeException("Unknown type: " + c.getType());
            } else {
                throw new RuntimeException("Unsupported node in WHERE");
            }

            if (!areCompatible(leftCol.getType(), rightType)) {
                throw new RuntimeException("Incompatible types: " +
                        leftCol.getType().getName() + " vs " + rightType.getName());
            }

            filter = new Filter(expr.getOp(), leftCol, rightVal, rightType);
        }

        ResolvedColumn resolvedTarget = new ResolvedColumn(table, targetCol, catalog.getType(cons.getType()));
        Map<ResolvedColumn, Object> setValues = new HashMap<>();
        setValues.put(resolvedTarget, cons.getValue());

        return new QueryTree("UPDATE", tableName, setValues, filter);
    }

    private ResolvedColumn resolveColumn(ColumnRef colRef, List<String> fromTables, CatalogManager catalog) {
        String name = colRef.getName();
        String tableName = null;
        String colName = name;

        int dot = name.indexOf('.');
        if (dot >= 0) {
            tableName = name.substring(0, dot);
            colName = name.substring(dot + 1);
        } else if (fromTables.size() == 1) {
            tableName = fromTables.get(0);
        }

        if (tableName == null) {
            throw new RuntimeException("Ambiguous column reference: " + name);
        }

        TableDefinition table = catalog.getTable(tableName);
        ColumnDefinition col = catalog.getColumn(table, colName);
        if (col == null) {
            throw new RuntimeException("Column not found: " + name);
        }
        TypeDefinition type = null;

        try {
            int typeOid = col.getTypeOid();
            type = catalog.getType(typeOid);
        } catch (Exception ignored) {}

        if (type == null) {
            type = catalog.getType("INT");
        }

        if (type == null) {
            throw new RuntimeException("Cannot resolve type for column: " + name);
        }

        return new ResolvedColumn(table, col, type);

    }

    private boolean areCompatible(TypeDefinition left, TypeDefinition right) {
        if (left == null || right == null) return false;
        return left.getName().equalsIgnoreCase(right.getName());
    }
}
