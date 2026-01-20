package system.ast;

import java.util.ArrayList;
import java.util.List;

public class QueryTree {
    public List<RangeTblEntry> rangeTable;
    public List<TargetEntry> targetList;
    public Expr whereClause;
    public QueryType commandType;


    public QueryTree() {
        this.rangeTable = new ArrayList<>();
        this.targetList = new ArrayList<>();
    }

    @Override
    public String toString() {
        return "QueryTree{" +
                "commandType=" + commandType +
                ", rangeTable=" + rangeTable +
                ", targetList=" + targetList +
                (whereClause != null ? ", where=" + whereClause : "") +
                '}';
    }


}


