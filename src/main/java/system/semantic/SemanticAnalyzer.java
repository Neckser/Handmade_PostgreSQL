package system.semantic;

import system.catalog.CatalogManager;
import system.nodes.AstNode;
import system.semantic.QueryTree;

public interface SemanticAnalyzer {
    QueryTree analyze(AstNode ast, CatalogManager catalog);
}
