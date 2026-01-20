package system.optimizer.node;


import system.ast.TargetEntry;

import java.util.List;

public class PhysicalProjectNode extends PhysicalPlanNode {
    private final List<TargetEntry> targetList;
    private final PhysicalPlanNode child;

    public PhysicalProjectNode(List<TargetEntry> targetList, PhysicalPlanNode child) {
        super("PhysicalProject");
        this.targetList = targetList;
        this.child = child;
    }

    public List<TargetEntry> getTargetList() {
        return targetList;
    }

    public PhysicalPlanNode getChild() {
        return child;
    }

    @Override
    public String prettyPrint(String indent) {
        List<String> columnNames = targetList.stream()
                .map(te -> te.alias != null ? te.alias : te.expr.toString())
                .toList();
        return indent + "PhysicalProject(" + columnNames + ")\n" + child.prettyPrint(indent + "  ");
    }
}