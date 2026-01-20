package system.catalog.manager;

import system.catalog.model.TableDefinition;
import system.catalog.model.ColumnDefinition;
import system.catalog.model.TypeDefinition;

import java.util.List;

public interface CatalogManager {

    TableDefinition createTable(String name, List<ColumnDefinition> columns);

    TableDefinition getTable(String tableName);

    ColumnDefinition getColumn(TableDefinition table, String columnName);

    List<TableDefinition> listTables();

    TypeDefinition getType(String resultType);

    TypeDefinition getType(int resultType);
}
