package system.parser.nodes;

public class RangeVar implements AstNode {
    public String relName;
    public String alias;

    public RangeVar(String relName) {
        this(relName, null);
    }

    public RangeVar(String relName, String alias) {
        this.relName = relName;
        this.alias = alias;
    }
    public String getRelName() { return relName; }
    public String getAlias() { return alias; }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();


        if (alias != null && alias.trim().isEmpty()) sb.append(" AS ").append(alias);

        return sb.toString();
    }
}