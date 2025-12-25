package system.semantic;

import system.model.TableDefinition;
import system.model.ColumnDefinition;
import system.nodes.AstNode;
import java.util.List;
import java.util.ArrayList;

public class QueryTree {
    private final List<ColumnDefinition> targetList;
    private final List<TableDefinition> fromTables;
    private final AstNode whereClause;

    public QueryTree(List<ColumnDefinition> targetList, List<TableDefinition> fromTables, AstNode whereClause) {
        this.targetList = targetList;
        this.fromTables = fromTables;
        this.whereClause = whereClause;
    }

    public List<ColumnDefinition> getTargetList() { return targetList; }
    public List<TableDefinition> getFromTables() { return fromTables; }
    public AstNode getWhereClause() { return whereClause; }

    @Override
    public String toString() {
        return "QueryTree\n" +
                "├── targetList: " + formatColumns(targetList) + "\n" +
                "├── fromTables: " + fromTables + "\n" +
                "└── whereClause: " + whereClause;
    }

    private String formatColumns(List<ColumnDefinition> columns) {
        List<String> formatted = new ArrayList<>();
        for (ColumnDefinition col : columns) {
            formatted.add("Column(\"" + col.getName() + ": " + col.getType0id() + "\")");
        }
        return formatted.toString();
    }
}