package system.planner.node;


import system.ast.Expr;
import system.ast.TargetEntry;

import java.util.List;

public class ProjectNode extends LogicalPlanNode {
    private final List<TargetEntry> targetList;
    private final LogicalPlanNode child;

    public ProjectNode(List<TargetEntry> targetList, LogicalPlanNode child) {
        super("Project");
        this.targetList = targetList;
        this.child = child;
        this.outputColumns = targetList.stream()
                .map(te -> te.alias != null ? te.alias : extractColumnName(te.expr))
                .toList();
    }

    public List<TargetEntry> getTargetList() {
        return targetList;
    }

    public LogicalPlanNode getChild() {
        return child;
    }

    private String extractColumnName(Expr expr) {
        return expr.toString();
    }

    @Override
    public String prettyPrint(String indent) {
        return indent + "Project(" + outputColumns + ")\n" + child.prettyPrint(indent + "  ");
    }
}