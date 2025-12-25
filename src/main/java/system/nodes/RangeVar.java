package system.nodes;

public class RangeVar implements AstNode {
    private final String name;

    public RangeVar(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}