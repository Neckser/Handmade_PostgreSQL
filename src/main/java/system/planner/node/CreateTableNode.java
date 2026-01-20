package system.planner.node;


import system.catalog.model.TableDefinition;

import java.util.List;

public class CreateTableNode extends LogicalPlanNode {

    private final TableDefinition tableDefinition;

    public CreateTableNode(TableDefinition tableDefinition) {
        super("CreateTable");
        this.tableDefinition = tableDefinition;
        this.outputColumns = List.of();
    }

    public TableDefinition getTableDefinition() {
        return tableDefinition;
    }

    @Override
    public String prettyPrint(String indent) {
        return indent + "CreateTable(" + tableDefinition.getName() + ")\n";
    }
}