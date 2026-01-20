package system.ast;

public class TargetEntry {

    public Expr expr;
    public String alias;
    public String resultType;

    public TargetEntry(Expr expr, String alias) {
        this.expr = expr;
        this.alias = alias;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TargetEntry(");

        if (expr != null) {
            sb.append("expr=").append(expr);
        } else {
            sb.append("expr=*");
        }

        if (alias != null) {
            sb.append(", alias=").append(alias);
        }

        if (resultType != null) {
            sb.append(", type=").append(resultType);
        }

        sb.append(")");
        return sb.toString();
    }
}

