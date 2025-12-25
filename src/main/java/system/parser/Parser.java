package system.parser;

import system.lexer.Token;
import system.nodes.AstNode;

import java.util.List;

public interface Parser {
    AstNode parse(List<Token> tokens);

}
