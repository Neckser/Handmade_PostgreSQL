package system.parser.nodes;

public class ResTarget implements AstNode {
    private final ColumnRef val;

    public ResTarget(ColumnRef val) {
        this.val = val;
    }

    public ColumnRef getVal() { return val; }

    @Override
    public String toString() {
        return "ResTarget{" +
                "val=" + val +
                '}';
    }
}
