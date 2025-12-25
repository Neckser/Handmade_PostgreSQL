package system.nodes;

public class AConst implements AstNode {
    private final Object value;

    public AConst(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }
}