package system.optimizer;

import system.optimizer.node.PhysicalPlanNode;
import system.planner.node.LogicalPlanNode;

public interface Optimizer {
    PhysicalPlanNode optimize(LogicalPlanNode logicalPlan);
}
