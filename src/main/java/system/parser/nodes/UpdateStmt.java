package system.parser.nodes;

public class UpdateStmt implements AstNode {
    private final String tableName;
    private final String column;
    private final AstNode newValue;
    private final AstNode whereClause;

    public UpdateStmt(String tableName, String column, AstNode newValue, AstNode whereClause) {
        this.tableName = tableName;
        this.column = column;
        this.newValue = newValue;
        this.whereClause = whereClause;
    }

    public String getTableName() { return tableName; }
    public String getColumn() { return column; }
    public AstNode getNewValue() { return newValue; }
    public AstNode getWhereClause() { return whereClause; }
}
