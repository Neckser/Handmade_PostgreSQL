package system.parser.nodes;

public class ColumnRef implements AstNode {
    private final String name;

    public ColumnRef(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    @Override
    public String toString() {
        return "ColumnRef{" +
                "name='" + name + '\'' +
                '}';
    }
}