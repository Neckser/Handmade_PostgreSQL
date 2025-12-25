package semantic;

import org.junit.jupiter.api.Test;
import system.lexer.MyLexer;
import system.parser.MyParser;
import system.lexer.Token;
import system.nodes.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MySemanticAnalyzerTest {

    @Test
    void testBasicSQLPipeline() {
        MyLexer lexer = new MyLexer();
        MyParser parser = new MyParser();
        String sql1 = "SELECT id FROM users";
        testPipeline(lexer, parser, sql1);

        String sql2 = "SELECT name FROM users WHERE age > 21";
        testPipeline(lexer, parser, sql2);

        String sql3 = "SELECT id, name, age FROM users";
        testPipeline(lexer, parser, sql3);

        String sql4 = "SELECT name FROM users WHERE name = 'Alice'";
        testPipeline(lexer, parser, sql4);
    }

    private void testPipeline(MyLexer lexer, MyParser parser, String sql) {

        try {
            List<Token> tokens = lexer.tokenize(sql);
            assertTrue(tokens.size() > 0, "Should have tokens");

            AstNode ast = parser.parse(tokens);

            assertInstanceOf(SelectStmt.class, ast, "Should be SelectStmt");

            SelectStmt select = (SelectStmt) ast;

        } catch (Exception e) {
            fail("Pipeline failed for: " + sql + "\nError: " + e.getMessage());
        }
    }
}