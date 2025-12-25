package semantic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import system.model.TableDefinition;
import system.model.ColumnDefinition;
import system.nodes.*;
import java.util.List;
import java.util.ArrayList;
import system.semantic.QueryTree;

import static org.junit.jupiter.api.Assertions.*;

class QueryTreeTest {

    private ColumnDefinition idColumn;
    private ColumnDefinition nameColumn;
    private ColumnDefinition ageColumn;
    private TableDefinition usersTable;
    private TableDefinition ordersTable;
    private AExpr sampleWhereClause;

    @BeforeEach
    void setUp() {
        idColumn = new ColumnDefinition(1, 1, 23, "id", 0);
        nameColumn = new ColumnDefinition(2, 1, 25, "name", 1);
        ageColumn = new ColumnDefinition(3, 1, 23, "age", 2);

        usersTable = new TableDefinition(1, "users", "r", "users.dat", 0);
        ordersTable = new TableDefinition(2, "orders", "r", "orders.dat", 0);

        sampleWhereClause = new AExpr("GT",
                new ColumnRef("age"),
                new AConst(18)
        );
    }

    @Test
    void testQueryTreeCreation() {
        List<ColumnDefinition> columns = List.of(idColumn, nameColumn);
        List<TableDefinition> tables = List.of(usersTable);

        QueryTree queryTree = new QueryTree(columns, tables, null);

        assertNotNull(queryTree);
        assertEquals(2, queryTree.getTargetList().size());
        assertEquals(1, queryTree.getFromTables().size());
        assertNull(queryTree.getWhereClause());
    }

    @Test
    void testQueryTreeWithWhereClause() {
        List<ColumnDefinition> columns = List.of(nameColumn);
        List<TableDefinition> tables = List.of(usersTable);

        QueryTree queryTree = new QueryTree(columns, tables, sampleWhereClause);

        assertNotNull(queryTree);
        assertEquals(1, queryTree.getTargetList().size());
        assertEquals(1, queryTree.getFromTables().size());
        assertNotNull(queryTree.getWhereClause());
        assertInstanceOf(AExpr.class, queryTree.getWhereClause());
    }

    @Test
    void testQueryTreeMultipleTables() {
        List<ColumnDefinition> columns = List.of(idColumn);
        List<TableDefinition> tables = List.of(usersTable, ordersTable);

        QueryTree queryTree = new QueryTree(columns, tables, null);

        assertNotNull(queryTree);
        assertEquals(1, queryTree.getTargetList().size());
        assertEquals(2, queryTree.getFromTables().size());
        assertEquals("users", queryTree.getFromTables().get(0).getName());
        assertEquals("orders", queryTree.getFromTables().get(1).getName());
    }

    @Test
    void testQueryTreeGetTargetList() {
        List<ColumnDefinition> columns = new ArrayList<>();
        columns.add(idColumn);
        columns.add(nameColumn);
        columns.add(ageColumn);

        QueryTree queryTree = new QueryTree(columns, List.of(usersTable), null);

        List<ColumnDefinition> result = queryTree.getTargetList();
        assertEquals(3, result.size());
        assertEquals("id", result.get(0).getName());
        assertEquals("name", result.get(1).getName());
        assertEquals("age", result.get(2).getName());

    }

    @Test
    void testQueryTreeEmptyLists() {
        List<ColumnDefinition> emptyColumns = new ArrayList<>();
        List<TableDefinition> emptyTables = new ArrayList<>();

        QueryTree queryTree = new QueryTree(emptyColumns, emptyTables, null);

        assertEquals(0, queryTree.getTargetList().size());
        assertEquals(0, queryTree.getFromTables().size());
        assertNull(queryTree.getWhereClause());
    }

    @Test
    void testToStringMethod() {
        List<ColumnDefinition> columns = List.of(idColumn, nameColumn);
        List<TableDefinition> tables = List.of(usersTable);

        QueryTree queryTree = new QueryTree(columns, tables, sampleWhereClause);

        String result = queryTree.toString();

        assertNotNull(result);
        assertTrue(result.contains("QueryTree"));
        assertTrue(result.contains("targetList"));
        assertTrue(result.contains("fromTables"));
        assertTrue(result.contains("whereClause"));
        assertTrue(result.contains("Column"));
        assertTrue(result.contains("TableDefinition"));
    }

    @Test
    void testToStringWithoutWhereClause() {
        List<ColumnDefinition> columns = List.of(idColumn);
        List<TableDefinition> tables = List.of(usersTable);

        QueryTree queryTree = new QueryTree(columns, tables, null);

        String result = queryTree.toString();

        assertNotNull(result);
        assertTrue(result.contains("QueryTree"));
        assertTrue(result.contains("whereClause: null"));
    }

    @Test
    void testToStringFormatColumns() {
        List<ColumnDefinition> columns = List.of(idColumn, nameColumn);
        List<TableDefinition> tables = List.of(usersTable);

        QueryTree queryTree = new QueryTree(columns, tables, null);
        String result = queryTree.toString();

        assertTrue(result.contains("Column(\"id: 23\")"));
        assertTrue(result.contains("Column(\"name: 25\")"));
    }

    @Test
    void testQueryTreeWithComplexWhere() {
        AExpr complexWhere = new AExpr("AND",
                new AExpr("GT", new ColumnRef("age"), new AConst(18)),
                new AExpr("EQ", new ColumnRef("active"), new AConst(true))
        );

        QueryTree queryTree = new QueryTree(
                List.of(nameColumn),
                List.of(usersTable),
                complexWhere
        );

        assertNotNull(queryTree.getWhereClause());
        assertInstanceOf(AExpr.class, queryTree.getWhereClause());

        AExpr whereExpr = (AExpr) queryTree.getWhereClause();
        assertEquals("AND", whereExpr.getOperator());
    }

    @Test
    void testQueryTreeEquality() {
        QueryTree qt1 = new QueryTree(
                List.of(idColumn, nameColumn),
                List.of(usersTable),
                sampleWhereClause
        );

        QueryTree qt2 = new QueryTree(
                List.of(idColumn, nameColumn),
                List.of(usersTable),
                sampleWhereClause
        );

        assertEquals(qt1.toString(), qt2.toString());
    }

    @Test
    void testNullSafety() {
        QueryTree queryTree = new QueryTree(null, null, null);
    }
}