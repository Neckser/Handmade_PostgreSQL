package system.semantic;

import system.catalog.model.TypeDefinition;

public class Filter {
    private final String op;
    private final ResolvedColumn left;
    private final Object rightValue;
    private final TypeDefinition rightType;

    public Filter(String op, ResolvedColumn left, Object rightValue, TypeDefinition rightType) {
        this.op = op;
        this.left = left;
        this.rightValue = rightValue;
        this.rightType = rightType;
    }

    public String getOp() { return op; }
    public ResolvedColumn getLeft() { return left; }
    public Object getRightValue() { return rightValue; }
    public TypeDefinition getRightType() { return rightType; }

    @Override
    public String toString() {
        return "Filter{" +
                "op='" + op + '\'' +
                ", left=" + left.getQualifiedName() +
                ", rightValue=" + rightValue +
                ", rightType=" + rightType.getName() +
                '}';
    }
}

