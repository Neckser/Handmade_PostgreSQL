package system.parser;

import org.junit.jupiter.api.Test;
import system.lexer.DefaultLexer;
import system.parser.nodes.*;

import static org.junit.jupiter.api.Assertions.*;

class DefaultParserTest {

    @Test
    void testSelectWithWhere() {
        DefaultLexer lexer = new DefaultLexer();
        DefaultParser parser = new DefaultParser();

        var tokens = lexer.tokenize("SELECT name, age FROM users WHERE age > 18;");
        AstNode node = parser.parse(tokens);

        assertTrue(node instanceof SelectStmt);
        SelectStmt select = (SelectStmt) node;

        assertEquals(2, select.getTargetList().size());
        assertEquals(1, select.getFromClause().size());
        assertNotNull(select.getWhereClause())  ;
    }

    @Test
    void testSelectWithoutWhere() {
        DefaultLexer lexer = new DefaultLexer();
        DefaultParser parser = new DefaultParser();

        var tokens = lexer.tokenize("SELECT id FROM accounts;");
        SelectStmt stmt = (SelectStmt) parser.parse(tokens);

        assertEquals("id", stmt.getTargetList().get(0).getVal().getName());
        assertNull(stmt.getWhereClause());
    }

    @Test
    void testCreateTable() {
        DefaultLexer lexer = new DefaultLexer();
        DefaultParser parser = new DefaultParser();

        var tokens = lexer.tokenize("CREATE TABLE users (id INT, name TEXT, age INT);");
        AstNode node = parser.parse(tokens);

        assertTrue(node instanceof CreateStmt);
        CreateStmt create = (CreateStmt) node;

        assertEquals("users", create.getTableName());

        assertEquals(3, create.getColumns().size());
        assertEquals("id", create.getColumns().get(0).getName());
        assertEquals("INT", create.getColumns().get(0).getType());
        assertEquals("name", create.getColumns().get(1).getName());
        assertEquals("TEXT", create.getColumns().get(1).getType());
    }

    @Test
    void testUpdateWithWhere() {
        DefaultLexer lexer = new DefaultLexer();
        DefaultParser parser = new DefaultParser();

        var tokens = lexer.tokenize("UPDATE users SET age = 30 WHERE id = 1;");
        AstNode node = parser.parse(tokens);

        assertTrue(node instanceof UpdateStmt);
        UpdateStmt update = (UpdateStmt) node;

        assertEquals("users", update.getTableName());
        assertEquals("age", update.getColumn());

        assertTrue(update.getNewValue() instanceof AConst);
        AConst value = (AConst) update.getNewValue();
        assertEquals(30, value.getValue());

        assertNotNull(update.getWhereClause());
        assertTrue(update.getWhereClause() instanceof AExpr);
        AExpr where = (AExpr) update.getWhereClause();

        assertTrue(where.getLeft() instanceof ColumnRef);
        assertEquals("id", ((ColumnRef) where.getLeft()).getName());
        assertEquals("=", where.getOp());
        assertTrue(where.getRight() instanceof AConst);
        assertEquals(1, ((AConst) where.getRight()).getValue());
    }


}
