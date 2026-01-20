package system.parser;

import system.lexer.Token;
import system.parser.nodes.AstNode;

import java.util.List;

public interface Parser {
    AstNode parse(List<Token> tokens);

}
