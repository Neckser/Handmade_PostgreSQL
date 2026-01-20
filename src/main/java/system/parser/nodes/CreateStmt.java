package system.parser.nodes;

import java.util.List;

public class CreateStmt implements AstNode {
    private final String tableName;
    private final List<ColumnDef> columns;

    public CreateStmt(String tableName, List<ColumnDef> columns) {
        this.tableName = tableName;
        this.columns = columns;
    }

    public String getTableName() {
        return tableName;
    }

    public List<ColumnDef> getColumns() {
        return columns;
    }

    @Override
    public String toString() {
        return "CreateStmt(table=" + tableName + ", columns=" + columns + ")";
    }
}
