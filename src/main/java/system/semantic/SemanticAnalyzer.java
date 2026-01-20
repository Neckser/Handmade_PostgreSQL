package system.semantic;

import system.catalog.manager.CatalogManager;
import system.parser.nodes.AstNode;

public interface SemanticAnalyzer {
    QueryTree analyze(AstNode ast, CatalogManager catalog);
}
