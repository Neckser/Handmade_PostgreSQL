package system.semantic;

import system.catalog.model.ColumnDefinition;
import system.catalog.model.TableDefinition;
import system.catalog.model.TypeDefinition;

public class ResolvedColumn {
    private final TableDefinition table;
    private final ColumnDefinition column;
    private final TypeDefinition type;

    public ResolvedColumn(TableDefinition table, ColumnDefinition column, TypeDefinition type) {
        this.table = table;
        this.column = column;
        this.type = type;
    }

    public String getQualifiedName() {
        return table.getName() + "." + column.getName();
    }

    public TypeDefinition getType() { return type; }
    public ColumnDefinition getColumn() { return column; }
    public TableDefinition getTable() { return table; }
}
