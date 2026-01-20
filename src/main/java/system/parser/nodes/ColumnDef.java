package system.parser.nodes;

public class ColumnDef implements AstNode {
    private final String name;
    private final String type;

    public ColumnDef(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() { return name; }
    public String getType() { return type; }

    @Override
    public String toString() {
        return "ColumnDef(name=" + name + ", type=" + type + ")";
    }
}
