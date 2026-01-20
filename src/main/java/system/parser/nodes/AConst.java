package system.parser.nodes;

public class AConst implements AstNode {
    private final Object value;
    private final String type;

    public AConst(Object value, String type) {
        this.value = value;
        this.type = type;
    }

    public Object getValue() { return value; }
    public String getType() { return type; }
    @Override
    public String toString() {
        return "......";
    }
}