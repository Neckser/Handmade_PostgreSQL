package system.execution.executors;


import system.catalog.manager.CatalogManager;
import system.catalog.model.TableDefinition;


public class CreateTableExecutor implements Executor {

    private final CatalogManager catalogManager;
    private final TableDefinition tableDefinition;

    public CreateTableExecutor(CatalogManager catalogManager, TableDefinition tableDefinition) {
        this.catalogManager = catalogManager;
        this.tableDefinition = tableDefinition;
    }

    @Override
    public void open() { }

    @Override
    public Object next() {
        catalogManager.createTable(
                tableDefinition.getName(),
                tableDefinition.getColumns()
        );
        return null;
    }

    @Override
    public void close() { }
}