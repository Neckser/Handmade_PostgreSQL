package system.lexer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DefaultLexerTest {

    @Test
    void testSimpleSelect() {
        DefaultLexer lexer = new DefaultLexer();
        List<Token> tokens = lexer.tokenize("SELECT name, age FROM users WHERE age > 18;");

        assertFalse(tokens.isEmpty());
        assertEquals("SELECT", tokens.get(0).getType());
        assertTrue(tokens.stream().anyMatch(t -> t.getType().equals("WHERE")));
        assertEquals("SEMICOLON", tokens.get(tokens.size()-1).getType());
    }

    @Test
    void testComparisonTokens() {
        DefaultLexer lexer = new DefaultLexer();
        List<Token> tokens = lexer.tokenize("a>=10");
        assertEquals("GE", tokens.get(1).getType());
    }

    @Test
    void testInvalidCharacterThrows() {
        DefaultLexer lexer = new DefaultLexer();
        assertThrows(RuntimeException.class, () -> lexer.tokenize("SELECT # FROM users"));
    }
}
