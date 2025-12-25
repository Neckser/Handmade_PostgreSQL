package system.semantic;

import system.catalog.CatalogManager;
import system.model.TableDefinition;
import system.model.ColumnDefinition;
import system.model.TypeDefinition;
import system.nodes.*;
import java.util.*;

public class MySemanticAnalyzer implements SemanticAnalyzer {

    @Override
    public QueryTree analyze(AstNode ast, CatalogManager catalog) {
        if (!(ast instanceof SelectStmt)) {
            throw new RuntimeException();
        }
        SelectStmt selectStmt = (SelectStmt) ast;
        List<TableDefinition> tables = validateTables(selectStmt.getFromClause(), catalog);
        List<ColumnDefinition> targetList = resolveTargetList(selectStmt.getTargetList(), tables, catalog);
        if (selectStmt.getWhereClause() != null) {
            validateWhereClause(selectStmt.getWhereClause(), tables, catalog);
        }
        return new QueryTree(targetList, tables, selectStmt.getWhereClause());
    }

    private List<TableDefinition> validateTables(List<RangeVar> fromTables, CatalogManager catalog) {
        List<TableDefinition> tableDefs = new ArrayList<>();
        for (RangeVar rangeVar : fromTables) {
            TableDefinition table = catalog.getTable(rangeVar.getName());
            if (table == null) {
                throw new RuntimeException();
            }
            tableDefs.add(table);
        }
        if (tableDefs.isEmpty()) {
            throw new RuntimeException();
        }
        return tableDefs;
    }

    private List<ColumnDefinition> resolveTargetList(List<ResTarget> targets, List<TableDefinition> tables, CatalogManager catalog) {
        List<ColumnDefinition> resolvedColumns = new ArrayList<>();
        for (ResTarget target : targets) {
            if (target.getValue() instanceof ColumnRef) {
                ColumnDefinition column = resolveColumn((ColumnRef) target.getValue(), tables, catalog);
                resolvedColumns.add(column);
            } else {
                throw new RuntimeException();
            }
        }

        return resolvedColumns;
    }

    private void validateWhereClause(AstNode whereClause, List<TableDefinition> tables, CatalogManager catalog) {
        if (whereClause instanceof AExpr) {
            validateAExpr((AExpr) whereClause, tables, catalog);
        } else {
            throw new RuntimeException();
        }
    }

    private void validateAExpr(AExpr aexpr, List<TableDefinition> tables, CatalogManager catalog) {
        if (!(aexpr.getLeft() instanceof ColumnRef)) {
            throw new RuntimeException();
        }
        ColumnDefinition leftCol = resolveColumn((ColumnRef) aexpr.getLeft(), tables, catalog);
        String leftType = getColumnType(leftCol, catalog);
        String rightType = getExpressionType(aexpr.getRight(), tables, catalog);
        if (!leftType.equals(rightType)) {
            throw new RuntimeException("Type mismatch: " + leftType +
                    " " + aexpr.getOperator() + " " + rightType);
        }
        String operator = aexpr.getOperator();
        if (operator.equals(">") || operator.equals("<") || operator.equals(">=") || operator.equals("<=")) {
            if (!leftType.equals("INT") && !leftType.equals("FLOAT")) {
                throw new RuntimeException("Operator " + operator + " not supported for type: " + leftType);
            }
        }
    }

    private ColumnDefinition resolveColumn(ColumnRef columnRef, List<TableDefinition> tables, CatalogManager catalog) {
        String columnName = columnRef.getName();
        List<ColumnDefinition> foundColumns = new ArrayList<>();
        for (TableDefinition table : tables) {
            List<ColumnDefinition> tableColumns = catalog.getTableColumns(table.getOid());
            for (ColumnDefinition column : tableColumns) {
                if (column.getName().equalsIgnoreCase(columnName)) {
                    foundColumns.add(column);
                }
            }
        }
        if (foundColumns.isEmpty()) {
            throw new RuntimeException("Column not found in any table: " + columnName);
        }
        if (foundColumns.size() > 1) {
            throw new RuntimeException("Ambiguous column: " + columnName);
        }

        return foundColumns.get(0);
    }
    private String getColumnType(ColumnDefinition column, CatalogManager catalog) {
        TypeDefinition typeDef = catalog.getType(column.getType0id());
        if (typeDef == null) {
            throw new RuntimeException("Type not found for column: " + column.getName());
        }
        return typeDef.getName();
    }

    private String getExpressionType(AstNode node, List<TableDefinition> tables, CatalogManager catalog) {
        if (node instanceof ColumnRef) {
            ColumnDefinition column = resolveColumn((ColumnRef) node, tables, catalog);
            return getColumnType(column, catalog);
        } else if (node instanceof AConst) {
            Object value = ((AConst) node).getValue();
            if (value instanceof Integer) {
                return "INT";
            }
            if (value instanceof String) {
                return "STRING";
            }
            if (value instanceof Boolean) {
                return "BOOLEAN";
            }
            if (value instanceof Float || value instanceof Double) return "FLOAT";
            throw new RuntimeException("Unsupported type: " + value.getClass().getSimpleName());
        }
        throw new RuntimeException("Cannot determine type for: " + node);
    }
}