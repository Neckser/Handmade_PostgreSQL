package system.nodes;

public class ColumnRef implements AstNode {
    private final String name;

    public ColumnRef(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}