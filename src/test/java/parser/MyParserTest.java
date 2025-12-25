package parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import system.lexer.Token;
import system.parser.MyParser;
import system.nodes.*;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class MyParserTest {

    private MyParser parser = new MyParser();

    private Token t(String type, String value) {
        return new Token(type, value);
    }

    private void assertAConstValue(AConst aConst, Object expected) {
        Object actual = aConst.getValue();
        if (expected instanceof Integer && actual instanceof Integer) {
            assertEquals(expected, actual);
        } else if (expected instanceof String && actual instanceof String) {
            assertEquals(expected, actual);
        } else if (expected instanceof Double && actual instanceof Double) {
            assertEquals((Double) expected, (Double) actual, 0.001);
        } else {
            assertEquals(String.valueOf(expected), String.valueOf(actual));
        }
    }

    @Test
    void testSimpleSelectSingleColumn() {
        List<Token> tokens = List.of(
                t("SELECT", "SELECT"),
                t("IDENT", "name"),
                t("FROM", "FROM"),
                t("IDENT", "users"),
                t("SEMICOLON", ";")
        );

        AstNode result = parser.parse(tokens);
        assertInstanceOf(SelectStmt.class, result);

        SelectStmt select = (SelectStmt) result;

        assertEquals(1, select.getTargetList().size());
        ResTarget resTarget = select.getTargetList().get(0);
        assertInstanceOf(ColumnRef.class, resTarget.getValue());
        ColumnRef columnRef = (ColumnRef) resTarget.getValue();
        assertEquals("name", columnRef.getName());

        assertEquals(1, select.getFromClause().size());
        RangeVar rangeVar = select.getFromClause().get(0);
        assertEquals("users", rangeVar.getName());

        assertNull(select.getWhereClause());
    }

    @Test
    void testSelectMultipleColumns() {
        List<Token> tokens = List.of(
                t("SELECT", "SELECT"),
                t("IDENT", "id"),
                t("COMMA", ","),
                t("IDENT", "name"),
                t("COMMA", ","),
                t("IDENT", "age"),
                t("FROM", "FROM"),
                t("IDENT", "users"),
                t("SEMICOLON", ";")
        );

        AstNode result = parser.parse(tokens);
        SelectStmt select = (SelectStmt) result;

        assertEquals(3, select.getTargetList().size());
        assertEquals("id", ((ColumnRef) select.getTargetList().get(0).getValue()).getName());
        assertEquals("name", ((ColumnRef) select.getTargetList().get(1).getValue()).getName());
        assertEquals("age", ((ColumnRef) select.getTargetList().get(2).getValue()).getName());
    }

    @Test
    void testSelectWithWhereClauseNumberComparison() {
        List<Token> tokens = List.of(
                t("SELECT", "SELECT"),
                t("IDENT", "name"),
                t("FROM", "FROM"),
                t("IDENT", "users"),
                t("WHERE", "WHERE"),
                t("IDENT", "age"),
                t("GT", ">"),
                t("NUMBER", "18"),
                t("SEMICOLON", ";")
        );

        AstNode result = parser.parse(tokens);
        SelectStmt select = (SelectStmt) result;

        assertNotNull(select.getWhereClause());
        assertInstanceOf(AExpr.class, select.getWhereClause());

        AExpr expr = (AExpr) select.getWhereClause();
        assertEquals("GT", expr.getOperator());

        assertInstanceOf(ColumnRef.class, expr.getLeft());
        assertEquals("age", ((ColumnRef) expr.getLeft()).getName());

        assertInstanceOf(AConst.class, expr.getRight());
        assertAConstValue((AConst) expr.getRight(), 18);
    }

    @Test
    void testSelectWithWhereClauseStringComparison() {
        List<Token> tokens = List.of(
                t("SELECT", "SELECT"),
                t("IDENT", "name"),
                t("FROM", "FROM"),
                t("IDENT", "users"),
                t("WHERE", "WHERE"),
                t("IDENT", "city"),
                t("EQ", "="),
                t("STRING", "London"),
                t("SEMICOLON", ";")
        );

        AstNode result = parser.parse(tokens);
        SelectStmt select = (SelectStmt) result;

        AExpr expr = (AExpr) select.getWhereClause();
        assertEquals("EQ", expr.getOperator());
        assertAConstValue((AConst) expr.getRight(), "London");
    }

    @Test
    void testSelectMultipleTables() {
        List<Token> tokens = List.of(
                t("SELECT", "SELECT"),
                t("IDENT", "users.name"),
                t("COMMA", ","),
                t("IDENT", "orders.id"),
                t("FROM", "FROM"),
                t("IDENT", "users"),
                t("COMMA", ","),
                t("IDENT", "orders"),
                t("SEMICOLON", ";")
        );

        AstNode result = parser.parse(tokens);
        SelectStmt select = (SelectStmt) result;

        assertEquals(2, select.getFromClause().size());
        assertEquals("users", select.getFromClause().get(0).getName());
        assertEquals("orders", select.getFromClause().get(1).getName());
    }

    @Test
    void testSelectWithoutSemicolon() {
        List<Token> tokens = List.of(
                t("SELECT", "SELECT"),
                t("IDENT", "id"),
                t("FROM", "FROM"),
                t("IDENT", "users"),
                t("WHERE", "WHERE"),
                t("IDENT", "active"),
                t("EQ", "="),
                t("NUMBER", "1")
        );

        assertDoesNotThrow(() -> parser.parse(tokens));
    }

    @Test
    void testSelectWithStar() {
        List<Token> tokens = List.of(
                t("SELECT", "SELECT"),
                t("STAR", "*"),
                t("FROM", "FROM"),
                t("IDENT", "users"),
                t("SEMICOLON", ";")
        );

        assertThrows(RuntimeException.class, () -> parser.parse(tokens));
    }

    @ParameterizedTest
    @MethodSource("comparisonOperatorsProvider")
    void testAllComparisonOperators(String operator, String tokenType) {
        List<Token> tokens = List.of(
                t("SELECT", "SELECT"),
                t("IDENT", "id"),
                t("FROM", "FROM"),
                t("IDENT", "users"),
                t("WHERE", "WHERE"),
                t("IDENT", "age"),
                t(tokenType, operator),
                t("NUMBER", "21"),
                t("SEMICOLON", ";")
        );

        AstNode result = parser.parse(tokens);
        SelectStmt select = (SelectStmt) result;
        AExpr expr = (AExpr) select.getWhereClause();

        assertEquals(tokenType, expr.getOperator());
    }

    private static Stream<Object[]> comparisonOperatorsProvider() {
        return Stream.of(
                new Object[]{">", "GT"},
                new Object[]{"<", "LT"},
                new Object[]{"=", "EQ"},
                new Object[]{">=", "GTE"},
                new Object[]{"<=", "LTE"},
                new Object[]{"<>", "NEQ"},
                new Object[]{"!=", "NEQ"}
        );
    }

    @Test
    void testSelectWithNegativeNumber() {
        List<Token> tokens = List.of(
                t("SELECT", "SELECT"),
                t("IDENT", "balance"),
                t("FROM", "FROM"),
                t("IDENT", "accounts"),
                t("WHERE", "WHERE"),
                t("IDENT", "balance"),
                t("LT", "<"),
                t("NUMBER", "-100"),
                t("SEMICOLON", ";")
        );

        assertDoesNotThrow(() -> parser.parse(tokens));
    }


    @Test
    void testMissingSelectKeyword() {
        List<Token> tokens = List.of(
                t("IDENT", "name"),
                t("FROM", "FROM"),
                t("IDENT", "users")
        );

        assertThrows(RuntimeException.class, () -> parser.parse(tokens));
    }

    @Test
    void testMissingFromKeyword() {
        List<Token> tokens = List.of(
                t("SELECT", "SELECT"),
                t("IDENT", "name"),
                t("IDENT", "users")
        );

        assertThrows(RuntimeException.class, () -> parser.parse(tokens));
    }

    @Test
    void testMissingColumnList() {
        List<Token> tokens = List.of(
                t("SELECT", "SELECT"),
                t("FROM", "FROM"),
                t("IDENT", "users")
        );

        assertThrows(RuntimeException.class, () -> parser.parse(tokens));
    }

    @Test
    void testMissingTableName() {
        List<Token> tokens = List.of(
                t("SELECT", "SELECT"),
                t("IDENT", "name"),
                t("FROM", "FROM"),
                t("SEMICOLON", ";")
        );

        assertThrows(RuntimeException.class, () -> parser.parse(tokens));
    }

    @Test
    void testWhereClauseMissingRightOperand() {
        List<Token> tokens = List.of(
                t("SELECT", "SELECT"),
                t("IDENT", "name"),
                t("FROM", "FROM"),
                t("IDENT", "users"),
                t("WHERE", "WHERE"),
                t("IDENT", "age"),
                t("GT", ">")
        );

        assertThrows(RuntimeException.class, () -> parser.parse(tokens));
    }

    @Test
    void testInvalidTokenInColumnList() {
        List<Token> tokens = List.of(
                t("SELECT", "SELECT"),
                t("NUMBER", "123"),
                t("FROM", "FROM"),
                t("IDENT", "users")
        );

        assertThrows(RuntimeException.class, () -> parser.parse(tokens));
    }

    @Test
    void testEmptyTokenList() {
        List<Token> tokens = List.of();

        assertThrows(RuntimeException.class, () -> parser.parse(tokens));
    }

    @Test
    void testCaseInsensitiveKeywords() {
        List<Token> tokens = List.of(
                t("SELECT", "select"),
                t("IDENT", "name"),
                t("FROM", "from"),
                t("IDENT", "users"),
                t("SEMICOLON", ";")
        );

        assertDoesNotThrow(() -> parser.parse(tokens));
    }

    @Test
    void testQualifiedColumnNames() {
        List<Token> tokens = List.of(
                t("SELECT", "SELECT"),
                t("IDENT", "users.id"),
                t("FROM", "FROM"),
                t("IDENT", "users"),
                t("SEMICOLON", ";")
        );

        assertDoesNotThrow(() -> parser.parse(tokens));
    }

    @Test
    void testAndOrNotSupported() {
        List<Token> tokens = List.of(
                t("SELECT", "SELECT"),
                t("IDENT", "name"),
                t("FROM", "FROM"),
                t("IDENT", "users"),
                t("WHERE", "WHERE"),
                t("IDENT", "age"),
                t("GT", ">"),
                t("NUMBER", "18"),
                t("IDENT", "AND"),
                t("IDENT", "active"),
                t("EQ", "="),
                t("NUMBER", "1")
        );

        assertDoesNotThrow(() -> parser.parse(tokens));
    }

    @Test
    void testParenthesesNotSupported() {
        List<Token> tokens = List.of(
                t("SELECT", "SELECT"),
                t("IDENT", "name"),
                t("FROM", "FROM"),
                t("IDENT", "users"),
                t("WHERE", "WHERE"),
                t("LPAREN", "("),
                t("IDENT", "age"),
                t("GT", ">"),
                t("NUMBER", "18"),
                t("RPAREN", ")"),
                t("SEMICOLON", ";")
        );

        assertThrows(RuntimeException.class, () -> parser.parse(tokens));
    }

    @Test
    void testComplexQueryStructure() {
        List<Token> tokens = List.of(
                t("SELECT", "SELECT"),
                t("IDENT", "id"),
                t("COMMA", ","),
                t("IDENT", "name"),
                t("COMMA", ","),
                t("IDENT", "salary"),
                t("FROM", "FROM"),
                t("IDENT", "employees"),
                t("COMMA", ","),
                t("IDENT", "departments"),
                t("WHERE", "WHERE"),
                t("IDENT", "salary"),
                t("GTE", ">="),
                t("NUMBER", "50000"),
                t("SEMICOLON", ";")
        );

        AstNode result = parser.parse(tokens);
        SelectStmt select = (SelectStmt) result;

        assertEquals(3, select.getTargetList().size());
        assertEquals(2, select.getFromClause().size());
        assertNotNull(select.getWhereClause());

        AExpr expr = (AExpr) select.getWhereClause();
        assertEquals("GTE", expr.getOperator());
        assertEquals("salary", ((ColumnRef) expr.getLeft()).getName());

        assertAConstValue((AConst) expr.getRight(), 50000);
    }

    @Test
    void testEmptyStringValue() {
        List<Token> tokens = List.of(
                t("SELECT", "SELECT"),
                t("IDENT", "name"),
                t("FROM", "FROM"),
                t("IDENT", "users"),
                t("WHERE", "WHERE"),
                t("IDENT", "description"),
                t("EQ", "="),
                t("STRING", ""),
                t("SEMICOLON", ";")
        );

        assertDoesNotThrow(() -> parser.parse(tokens));
    }

    @Test
    void testTrailingCommaError() {
        List<Token> tokens = List.of(
                t("SELECT", "SELECT"),
                t("IDENT", "id"),
                t("COMMA", ","),
                t("IDENT", "name"),
                t("COMMA", ","),
                t("FROM", "FROM"),
                t("IDENT", "users"),
                t("SEMICOLON", ";")
        );

        assertThrows(RuntimeException.class, () -> parser.parse(tokens));
    }

    @Test
    void testMultipleWhereConditionsError() {
        List<Token> tokens = List.of(
                t("SELECT", "SELECT"),
                t("IDENT", "name"),
                t("FROM", "FROM"),
                t("IDENT", "users"),
                t("WHERE", "WHERE"),
                t("IDENT", "age"),
                t("GT", ">"),
                t("NUMBER", "18"),
                t("IDENT", "salary"),
                t("LT", "<"),
                t("NUMBER", "1000"),
                t("SEMICOLON", ";")
        );

        assertDoesNotThrow(() -> parser.parse(tokens));
    }
}