package system;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import system.ast.*;
import system.catalog.manager.*;
import system.catalog.model.TypeDefinition;
import system.catalog.operation.OperationManager;
import system.execution.ExecutorFactory;
import system.execution.ExecutorFactoryImpl;
import system.execution.QueryExecutionEngine;
import system.execution.QueryExecutionEngineImpl;
import system.execution.executors.Executor;
import system.memory.buffer.BufferPoolManager;
import system.memory.manager.HeapPageFileManager;
import system.memory.manager.PageFileManager;
import system.memory.model.BufferSlot;
import system.memory.page.Page;
import system.optimizer.Optimizer;
import system.optimizer.OptimizerImpl;
import system.planner.Planner;
import system.planner.PlannerImpl;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class QueryExecutionTest {
    private CatalogManager catalogManager;
    private Planner planner;
    private Optimizer optimizer;
    private ExecutorFactory executorFactory;
    private QueryExecutionEngine executionEngine;

    @BeforeEach
    void setUp() {
        deleteIfExists("table_definitions.dat");
        deleteIfExists("column_definitions.dat");
        deleteIfExists("types_definitions.dat");

        deleteNumericDatFiles();

        catalogManager = new DefaultCatalogManager();

        BufferPoolManager bufferPool = new BufferPoolManager() {
            public BufferSlot getPage(int pageId) { return null; }
            public void updatePage(int pageId, Page page) { }
            public void pinPage(int pageId) { }
            public void flushPage(int pageId) { }
            public void flushAllPages() { }
            public List<BufferSlot> getDirtyPages() { return List.of(); }
        };

        OperationManager operationManager = new OperationManager() {
            public void insert(String tableName, List<Object> values) {}
            public List<Object> select(String tableName, List<String> columnNames) {
                return List.of();
            }
        };
        PageFileManager pfm = new HeapPageFileManager();
        planner = new PlannerImpl(catalogManager);
        optimizer = new OptimizerImpl();
        executorFactory = new ExecutorFactoryImpl(catalogManager, operationManager, bufferPool,pfm);
        executionEngine = new QueryExecutionEngineImpl();
    }

    @Test
    void testPlanner_CreateTable_WithValidTypes() {
        QueryTree query = createSimpleCreateQueryWithValidTypes("test_table");
        var logicalPlan = planner.plan(query);
        assertNotNull(logicalPlan);
    }

    @Test
    void testOptimizer_CreateTable_WithValidTypes() {
        QueryTree query = createSimpleCreateQueryWithValidTypes("test_table");
        var logicalPlan = planner.plan(query);
        var physicalPlan = optimizer.optimize(logicalPlan);
        assertNotNull(physicalPlan);
    }

    @Test
    void testFullPipeline_CreateTable_WithValidTypes() {
        QueryTree query = createSimpleCreateQueryWithValidTypes("test_table");

        var logicalPlan = planner.plan(query);
        var physicalPlan = optimizer.optimize(logicalPlan);
        Executor executor = executorFactory.createExecutor(physicalPlan);
        List<Object> results = executionEngine.execute(executor);

        assertNotNull(results);
    }

    @Test
    void testPlanner_Select_NonExistentTable() {
        QueryTree selectQuery = createSimpleSelectQuery("nonexistent_table");
        assertThrows(Exception.class, () -> planner.plan(selectQuery));
    }

    @Test
    void testMultipleCreateTables_WithValidTypes() {
        executeCreateTable("table1");
        executeCreateTable("table2");
        assertTrue(true);
    }

    private QueryTree createSimpleCreateQueryWithValidTypes(String tableName) {
        QueryTree query = new QueryTree();
        query.commandType = QueryType.CREATE;
        query.rangeTable.add(new RangeTblEntry(tableName));

        TargetEntry idCol = new TargetEntry(null, "id");
        idCol.resultType = "integer";
        query.targetList.add(idCol);

        TargetEntry nameCol = new TargetEntry(null, "name");
        nameCol.resultType = "varchar";
        query.targetList.add(nameCol);

        return query;
    }

    private QueryTree createSimpleInsertQuery(String tableName, int id, String name) {
        QueryTree query = new QueryTree();
        query.commandType = QueryType.INSERT;
        query.rangeTable.add(new RangeTblEntry(tableName));

        query.targetList.add(new TargetEntry(new AConst(id), null));
        query.targetList.add(new TargetEntry(new AConst(name), null));

        return query;
    }

    private QueryTree createSimpleSelectQuery(String tableName) {
        QueryTree query = new QueryTree();
        query.commandType = QueryType.SELECT;
        query.rangeTable.add(new RangeTblEntry(tableName));
        return query;
    }

    private void executeCreateTable(String tableName) {
        QueryTree createQuery = createSimpleCreateQueryWithValidTypes(tableName);
        var logicalPlan = planner.plan(createQuery);
        var physicalPlan = optimizer.optimize(logicalPlan);
        Executor executor = executorFactory.createExecutor(physicalPlan);
        executionEngine.execute(executor);
    }

    @Test
    void testCatalogManager_DefaultTypesExist() {
        TypeDefinition integerType = ((DefaultCatalogManager) catalogManager).getType("integer");
        assertNotNull(integerType, "Integer type should exist");

        TypeDefinition varcharType = ((DefaultCatalogManager) catalogManager).getType("varchar");
        assertNotNull(varcharType, "Varchar type should exist");

        TypeDefinition booleanType = ((DefaultCatalogManager) catalogManager).getType("boolean");
        assertNotNull(booleanType, "Boolean type should exist");
    }

    private void deleteIfExists(String filename) {
        File f = new File(filename);
        if (f.exists() && !f.delete()) {
            throw new RuntimeException("Cannot delete file: " + filename);
        }
    }

    private void deleteNumericDatFiles() {
        Pattern p = Pattern.compile("^\\d+\\.dat$");
        File dir = new File(".");
        File[] files = dir.listFiles((d, name) -> p.matcher(name).matches());
        if (files == null) return;
        for (File f : files) {
            f.delete();
        }
    }
}
