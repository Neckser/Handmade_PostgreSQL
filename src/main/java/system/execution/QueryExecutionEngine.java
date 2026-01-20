package system.execution;

import system.execution.executors.Executor;

import java.util.List;

public interface QueryExecutionEngine {
    List<Object> execute(Executor executor);
}
