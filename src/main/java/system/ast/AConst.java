package system.ast;

public class AConst extends Expr {
    public Object value;

    public AConst(Object val) {
        this.value = val;
    }

    @Override
    public String toString() {
        return "......";
    }
}