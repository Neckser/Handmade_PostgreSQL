package system.parser.nodes;

import java.util.List;

public class SelectStmt implements AstNode {
    private final List<ResTarget> targetList;
    private final List<RangeVar> fromClause;
    private final AstNode whereClause;

    public SelectStmt(List<ResTarget> targetList, List<RangeVar> fromClause, AstNode whereClause) {
        this.targetList = targetList;
        this.fromClause = fromClause;
        this.whereClause = whereClause;
    }
    @Override
    public String toString() {
        return "SelectStmt{" +
                "targets=" + targetList +
                ", from=" + fromClause +
                (whereClause != null ? ", where=" + whereClause : "") +
                '}';
    }


    public List<ResTarget> getTargetList() { return targetList; }
    public List<RangeVar> getFromClause() { return fromClause; }
    public AstNode getWhereClause() { return whereClause; }
}