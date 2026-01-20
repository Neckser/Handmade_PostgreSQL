package system.parser.nodes;

public class AExpr implements AstNode {
    private final String op;
    private final AstNode left;
    private final AstNode right;

    public AExpr(String op, AstNode left, AstNode right) {
        this.op = op;
        this.left = left;
        this.right = right;
    }

    public String getOp() { return op; }
    public AstNode getLeft() { return left; }
    public AstNode getRight() { return right; }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (left instanceof system.ast.AExpr) {
            sb.append("(").append(left).append(")");
        } else {
            sb.append(left);
        }

        sb.append(" ").append(op).append(" ");

        if (right instanceof system.ast.AExpr) {
            sb.append("(").append(right).append(")");
        } else {
            sb.append(right);
        }
        return sb.toString();
    }
}
