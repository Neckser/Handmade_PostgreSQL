package system.planner.node;

import system.catalog.model.TableDefinition;
import system.ast.Expr;

import java.util.List;

public class InsertNode extends LogicalPlanNode {

    private final TableDefinition tableDefinition;
    private final List<Expr> values;

    public InsertNode(TableDefinition tableDefinition, List<Expr> values) {
        super("Insert");
        this.tableDefinition = tableDefinition;
        this.values = values;
        this.outputColumns = List.of();
    }

    public TableDefinition getTableDefinition() {
        return tableDefinition;
    }

    public List<Expr> getValues() {
        return values;
    }

    @Override
    public String prettyPrint(String indent) {
        return indent + "Insert(" + tableDefinition.getName() + ", values=" + values + ")\n";
    }
}