package system.optimizer.node;


public abstract class PhysicalPlanNode {

    private String nodeType;

    protected PhysicalPlanNode(String nodeType) {
        this.nodeType = nodeType;
    }
    protected PhysicalPlanNode() {}

    public String getNodeType() {
        return nodeType;
    }

    public String prettyPrint(String indent) {
        return indent + nodeType + "\n";
    }

    @Override
    public String toString() {
        return nodeType;
    }
}