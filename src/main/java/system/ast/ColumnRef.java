package system.ast;

public class ColumnRef extends Expr {
    public String table;
    public String column;
    public int tableIndex = -1;
    public int columnIndex = -1;

    public ColumnRef(String table, String column) {
        this.table = table;
        this.column = column;
    }

    public ColumnRef(String column) {
        this(null, column);
    }

    public ColumnRef(String table, String column, int tableIndex, int columnIndex) {
        this.table = table;
        this.column = column;
        this.tableIndex = tableIndex;
        this.columnIndex = columnIndex;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (table != null && !table.trim().isEmpty()) {
            sb.append(table).append(".");
        }
        sb.append(column);
        if (alias != null && !alias.trim().isEmpty()) {
            sb.append(" AS ").append(alias);
        }
        return sb.toString();
    }
}
