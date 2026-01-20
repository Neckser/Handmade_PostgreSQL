package system.execution;

import system.execution.executors.Executor;
import system.optimizer.node.PhysicalPlanNode;

public interface ExecutorFactory {
    Executor createExecutor(PhysicalPlanNode plan);

}
