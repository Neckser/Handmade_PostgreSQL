package lexer;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

import system.lexer.MyLexer;
import system.lexer.Token;

class LexerTest {

    private final MyLexer lexer = new MyLexer();

    @Test
    void testSimpleSelect() {
        List<Token> tokens = lexer.tokenize("SELECT name FROM users;");

        assertEquals(5, tokens.size());
        assertEquals("SELECT", tokens.get(0).getType());
        assertEquals("IDENT", tokens.get(1).getType());
        assertEquals("name", tokens.get(1).getValue());
        assertEquals("FROM", tokens.get(2).getType());
        assertEquals("IDENT", tokens.get(3).getType());
        assertEquals("users", tokens.get(3).getValue());
        assertEquals("SEMICOLON", tokens.get(4).getType());
    }

    @Test
    void testSelectWithWhere() {
        List<Token> tokens = lexer.tokenize("SELECT id FROM products WHERE price > 100");

        assertEquals(8, tokens.size());
        assertEquals("SELECT", tokens.get(0).getType());
        assertEquals("FROM", tokens.get(2).getType());
        assertEquals("WHERE", tokens.get(4).getType());
        assertEquals("GT", tokens.get(6).getType()); // >
        assertEquals("NUMBER", tokens.get(7).getType());
        assertEquals("100", tokens.get(7).getValue());
    }

    @Test
    void testMultipleColumns() {
        List<Token> tokens = lexer.tokenize("SELECT col1, col2, col3 FROM my_table");
        assertEquals(8, tokens.size());
        assertEquals("SELECT", tokens.get(0).getType());
        assertEquals("IDENT", tokens.get(1).getType());
        assertEquals("col1", tokens.get(1).getValue());
        assertEquals("COMMA", tokens.get(2).getType());
        assertEquals("IDENT", tokens.get(3).getType());
        assertEquals("col2", tokens.get(3).getValue());
        assertEquals("COMMA", tokens.get(4).getType());
        assertEquals("IDENT", tokens.get(5).getType());
        assertEquals("col3", tokens.get(5).getValue());
        assertEquals("FROM", tokens.get(6).getType());
        assertEquals("IDENT", tokens.get(7).getType());
        assertEquals("my_table", tokens.get(7).getValue());
    }

    @Test
    void testStringLiteral() {
        List<Token> tokens = lexer.tokenize("SELECT * FROM users WHERE name = 'John'");
        assertEquals(8, tokens.size());
        assertEquals("STRING", tokens.get(7).getType());
        assertEquals("John", tokens.get(7).getValue());
    }

    @Test
    void testDecimalNumber() {
        List<Token> tokens = lexer.tokenize("SELECT price FROM products WHERE price > 19.99");

        assertEquals(8, tokens.size());
        assertEquals("NUMBER", tokens.get(7).getType());
        assertEquals("19.99", tokens.get(7).getValue());
    }

    @Test
    void testAllOperators() {
        List<Token> tokens = lexer.tokenize("WHERE a = 1 AND b > 2 OR c < 3 AND d >= 4 OR e <= 5 AND f != 6");
        boolean hasEQ = false, hasGT = false, hasLT = false, hasGTE = false, hasLTE = false, hasNEQ = false;
        for (Token t : tokens) {
            switch (t.getType()) {
                case "EQ": hasEQ = true; break;
                case "GT": hasGT = true; break;
                case "LT": hasLT = true; break;
                case "GTE": hasGTE = true; break;
                case "LTE": hasLTE = true; break;
                case "NEQ": hasNEQ = true; break;
            }
        }

        assertTrue(hasEQ, "Missing =");
        assertTrue(hasGT, "Missing >");
        assertTrue(hasLT, "Missing <");
        assertTrue(hasGTE, "Missing >=");
        assertTrue(hasLTE, "Missing <=");
        assertTrue(hasNEQ, "Missing !=");
    }

    @Test
    void testCaseInsensitiveKeywords() {
        List<Token> tokens1 = lexer.tokenize("SELECT name FROM users");
        List<Token> tokens2 = lexer.tokenize("select name from users");
        List<Token> tokens3 = lexer.tokenize("SeLeCt NaMe FrOm UsErS");

        assertEquals(tokens1.size(), tokens2.size());
        assertEquals(tokens1.size(), tokens3.size());

        for (int i = 0; i < tokens1.size(); i++) {
            assertEquals(tokens1.get(i).getType(), tokens2.get(i).getType());
            assertEquals(tokens1.get(i).getType(), tokens3.get(i).getType());
        }
    }
}