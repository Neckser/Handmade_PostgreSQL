package system.cli.impl;

import system.SqlProcessor;
import system.ast.QueryTree;
import system.catalog.manager.CatalogManager;
import system.catalog.manager.DefaultCatalogManager;
import system.catalog.model.TableDefinition;
import system.catalog.operation.DefaultOperationManager;
import system.catalog.operation.OperationManager;
import system.cli.api.Engine;
import system.execution.ExecutorFactory;
import system.execution.ExecutorFactoryImpl;
import system.execution.QueryExecutionEngine;
import system.execution.QueryExecutionEngineImpl;
import system.execution.executors.Executor;
import system.lexer.DefaultLexer;
import system.lexer.Lexer;
import system.lexer.Token;
import system.memory.buffer.BufferPoolManager;
import system.memory.buffer.DefaultBufferPoolManager;
import system.memory.manager.HeapPageFileManager;
import system.memory.manager.PageFileManager;
import system.memory.replacer.ClockReplacer;
import system.optimizer.Optimizer;
import system.optimizer.OptimizerImpl;
import system.optimizer.node.PhysicalPlanNode;
import system.parser.DefaultParser;
import system.parser.Parser;
import system.parser.nodes.AstNode;
import system.planner.Planner;
import system.planner.PlannerImpl;
import system.planner.node.LogicalPlanNode;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class EngineImpl implements Engine {

    // shared global state
    private final CatalogManager catalog = new DefaultCatalogManager();
    private final Lexer lexer = new DefaultLexer();
    private final Parser parser = new DefaultParser();
    private final SqlProcessor sqlProcessor = new SqlProcessor(lexer, parser, catalog);

    private final Planner planner = new PlannerImpl(catalog);
    private final Optimizer optimizer = new OptimizerImpl();

    private final PageFileManager pfm = new HeapPageFileManager();
    private final OperationManager opManager = new DefaultOperationManager(catalog);
    private final QueryExecutionEngine execEngine = new QueryExecutionEngineImpl();

    @Override
    public String executeSql(String sql) {
        try {
            List<Token> tokens = lexer.tokenize(sql);
            log("TOKENS", tokens);

            AstNode ast = parser.parse(tokens);
            log("AST", ast);

            QueryTree queryTree = sqlProcessor.process(sql);
            log("QUERY_TREE", queryTree);

            // 4) Planner
            LogicalPlanNode logical = planner.plan(queryTree);
            log("LOGICAL_PLAN", logical);

            PhysicalPlanNode physical = optimizer.optimize(logical);
            log("PHYSICAL_PLAN", physical);

            // ✅ ВАЖНО: выбрать правильный файл данных для этой операции
            Path tableFile = resolveTableFile(queryTree);

            // ✅ создаём BufferPool под конкретный файл таблицы
            BufferPoolManager bufferPool = new DefaultBufferPoolManager(
                    16,
                    pfm,
                    new ClockReplacer(),
                    new ClockReplacer(),
                    tableFile
            );

            ExecutorFactory executorFactory = new ExecutorFactoryImpl(catalog, opManager, bufferPool,pfm);

            // 6) ExecutorFactory -> executor
            Executor executor = executorFactory.createExecutor(physical);
            log("EXECUTOR", executor.getClass().getSimpleName());

            // 7) execute
            List<Object> rows = execEngine.execute(executor);

            // flush, чтобы персистилось
            bufferPool.flushAllPages();

            if (rows.isEmpty()) return "OK";
            return rows.stream().map(String::valueOf).collect(Collectors.joining("\n"));

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    /**
     * Определяем, какой data-файл использовать:
     * - для CREATE: если таблицы ещё нет в каталоге — предполагаем следующий oid.dat невозможно,
     *   поэтому берём tableDefinition после фактического create в executor (но у нас executor уже внутри)
     *   => решение: для CREATE используем "temp" и rely on createDataFile в CatalogManager.
     *
     * Но проще: для CREATE/INSERT/SELECT берём таблицу из rangeTable[0] и ищем в каталоге,
     * а если её нет — используем tableName + ".dat" не получится. Поэтому:
     * - CREATE: используем Path.of("create_tmp.dat") (буфер пул почти не нужен)
     * - остальные: берём из каталога fileNode.
     */
    private Path resolveTableFile(QueryTree qt) {
        String tableName = (qt.rangeTable != null && !qt.rangeTable.isEmpty())
                ? qt.rangeTable.get(0).tableName
                : null;

        if (tableName == null) {
            return Path.of("1.dat").toAbsolutePath();
        }

        // CREATE: таблицы ещё может не быть, поэтому буфер пул по файлу не критичен
        if (qt.commandType != null && qt.commandType.name().equals("CREATE")) {
            return Path.of("create_tmp.dat").toAbsolutePath();
        }

        TableDefinition table = catalog.getTable(tableName);
        if (table == null) {
            // если таблицы нет — пусть будет хоть что-то, но дальше planner обычно должен упасть
            return Path.of("1.dat").toAbsolutePath();
        }

        return Path.of(table.getFileNode()).toAbsolutePath();
    }

    private void log(String stage, Object obj) {
        System.out.println("=== " + stage + " ===");
        System.out.println(obj);
    }
}
