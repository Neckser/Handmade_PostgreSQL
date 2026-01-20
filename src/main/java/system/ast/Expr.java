package system.ast;

public abstract class Expr extends AstNode {
    protected String alias;

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getAlias() {
        return alias;
    }
}