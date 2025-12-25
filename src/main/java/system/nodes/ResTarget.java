package system.nodes;

public class ResTarget implements AstNode {
    private final AstNode value;

    public ResTarget(AstNode value) {
        this.value = value;
    }
    public AstNode getValue() {
        return value;
    }
}