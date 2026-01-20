package system.planner;

import system.ast.QueryTree;
import system.planner.node.LogicalPlanNode;

public interface Planner {
    LogicalPlanNode plan(QueryTree queryTree);
}
