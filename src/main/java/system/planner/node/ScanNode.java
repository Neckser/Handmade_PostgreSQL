package system.planner.node;


import system.catalog.model.TableDefinition;

public class ScanNode extends LogicalPlanNode {
    private final TableDefinition tableDefinition;

    public ScanNode(TableDefinition tableDefinition) {
        super("Scan");
        this.tableDefinition = tableDefinition;
        this.outputColumns = tableDefinition.getColumns().stream()
                .map(col -> tableDefinition.getName() + "." + col.getName())
                .toList();
    }

    public TableDefinition getTableDefinition() {
        return tableDefinition;
    }

    @Override
    public String prettyPrint(String indent) {
        return indent + "Scan(" + tableDefinition.getName() + ")\n";
    }
}