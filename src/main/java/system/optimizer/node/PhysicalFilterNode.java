package system.optimizer.node;


import system.ast.Expr;

public class PhysicalFilterNode extends PhysicalPlanNode {
    private final Expr condition;
    private final PhysicalPlanNode child;

    public PhysicalFilterNode(Expr condition, PhysicalPlanNode child) {
        super("PhysicalFilter");
        this.condition = condition;
        this.child = child;
    }

    public Expr getCondition() {
        return condition;
    }

    public PhysicalPlanNode getChild() {
        return child;
    }

    @Override
    public String prettyPrint(String indent) {
        return indent + "PhysicalFilter(" + condition + ")\n" + child.prettyPrint(indent + "  ");
    }
}