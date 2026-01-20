package system.execution;

import system.catalog.manager.CatalogManager;
import system.catalog.model.TableDefinition;
import system.catalog.operation.OperationManager;
import system.execution.executors.*;
import system.memory.buffer.BufferPoolManager;
import system.memory.manager.PageFileManager;
import system.optimizer.node.*;

public class ExecutorFactoryImpl implements ExecutorFactory {

    private final CatalogManager catalogManager;
    private final OperationManager operationManager;
    private final BufferPoolManager bufferPool;
    private final PageFileManager pgManager;



    public ExecutorFactoryImpl(CatalogManager catalogManager,
                               OperationManager operationManager,
                               BufferPoolManager bufferPool,
                               PageFileManager pgManager) {
        this.catalogManager = catalogManager;
        this.operationManager = operationManager;
        this.bufferPool = bufferPool;
        this.pgManager = pgManager;
    }


    @Override
    public Executor createExecutor(PhysicalPlanNode plan) {
        if (plan instanceof PhysicalCreateNode create) {
            return new CreateTableExecutor(catalogManager, create.getTableDefinition());

        } else if (plan instanceof PhysicalInsertNode insert) {
            return new InsertExecutor(
                    pgManager,
                    bufferPool,
                    insert.getTableDefinition(),
                    insert.getValues()
            );


        } else if (plan instanceof PhysicalSeqScanNode scan) {
            return new SeqScanExecutor(bufferPool, scan.getTableDefinition());

        } else if (plan instanceof PhysicalFilterNode filter) {
            Executor child = createExecutor(filter.getChild());
            TableDefinition table = findTable(filter.getChild());
            return new FilterExecutor(child, filter.getCondition(), catalogManager, table);
        }
        else if (plan instanceof PhysicalProjectNode project) {
            Executor child = createExecutor(project.getChild());
            TableDefinition table = findTable(project.getChild());
            return new ProjectExecutor(child, project.getTargetList(), catalogManager, table);
        }


        throw new UnsupportedOperationException(
                "Unsupported physical plan node: " + plan.getClass().getSimpleName()
        );
    }
    private TableDefinition findTable(PhysicalPlanNode node) {
        if (node instanceof PhysicalSeqScanNode s) return s.getTableDefinition();
        if (node instanceof PhysicalFilterNode f) return findTable(f.getChild());
        if (node instanceof PhysicalProjectNode p) return findTable(p.getChild());
        return null;
    }

}