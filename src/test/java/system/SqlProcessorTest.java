package system;

import org.junit.jupiter.api.Test;
import system.ast.*;
import system.catalog.manager.DefaultCatalogManager;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class SqlProcessorTest {

    @Test
    void createTable_buildsHw5QueryTree() {
        SqlProcessor p = new SqlProcessor(new DefaultCatalogManager());

        QueryTree q = p.process("CREATE TABLE t (id integer, name varchar);");

        assertEquals(QueryType.CREATE, q.commandType);
        assertEquals(1, q.rangeTable.size());
        assertEquals("t", q.rangeTable.get(0).tableName);

        assertEquals(2, q.targetList.size());

        assertNull(q.targetList.get(0).expr);
        assertEquals("id", q.targetList.get(0).alias);
        assertEquals("integer", q.targetList.get(0).resultType);

        assertNull(q.targetList.get(1).expr);
        assertEquals("name", q.targetList.get(1).alias);
        assertEquals("varchar", q.targetList.get(1).resultType);
    }

    @Test
    void insert_buildsHw5QueryTree() {
        SqlProcessor p = new SqlProcessor(new DefaultCatalogManager());

        QueryTree q = p.process("INSERT INTO t VALUES (1, 'a');");

        assertEquals(QueryType.INSERT, q.commandType);
        assertEquals(1, q.rangeTable.size());
        assertEquals("t", q.rangeTable.get(0).tableName);

        assertEquals(2, q.targetList.size());
        assertTrue(q.targetList.get(0).expr instanceof AConst);
        assertTrue(q.targetList.get(1).expr instanceof AConst);

        Object v1 = ((AConst) q.targetList.get(0).expr).value;
        Object v2 = ((AConst) q.targetList.get(1).expr).value;

        assertEquals(1L, v1);     // NUMBER -> Long
        assertEquals("a", v2);    // STRING -> String
    }

    @Test
    void select_buildsHw5QueryTree() {
        SqlProcessor p = new SqlProcessor(new DefaultCatalogManager());

        QueryTree q = p.process("SELECT id, name FROM t;");

        assertEquals(QueryType.SELECT, q.commandType);
        assertEquals(1, q.rangeTable.size());
        assertEquals("t", q.rangeTable.get(0).tableName);

        assertEquals(2, q.targetList.size());
        assertNull(q.whereClause);

        assertTrue(q.targetList.get(0).expr instanceof ColumnRef);
        assertTrue(q.targetList.get(1).expr instanceof ColumnRef);

        ColumnRef c1 = (ColumnRef) q.targetList.get(0).expr;
        ColumnRef c2 = (ColumnRef) q.targetList.get(1).expr;

        assertNull(c1.table);
        assertEquals("id", c1.column);

        assertNull(c2.table);
        assertEquals("name", c2.column);
    }

    @Test
    void selectWithWhere_buildsHw5ExprTree() {
        SqlProcessor p = new SqlProcessor(new DefaultCatalogManager());

        QueryTree q = p.process("SELECT id FROM t WHERE id > 10;");

        assertEquals(QueryType.SELECT, q.commandType);
        assertNotNull(q.whereClause);
        assertTrue(q.whereClause instanceof AExpr);

        AExpr where = (AExpr) q.whereClause;
        assertEquals(">", where.getOp());

        AstNode left = getPrivateField(where, "left", AstNode.class);
        AstNode right = where.getRight();

        assertTrue(left instanceof ColumnRef);
        assertTrue(right instanceof AConst);

        ColumnRef l = (ColumnRef) left;
        AConst r = (AConst) right;

        assertEquals("id", l.column);
        assertTrue(r.value instanceof Number);
        assertEquals(10L, ((Number) r.value).longValue());
    }

    @Test
    void update_isRejectedExplicitly() {
        SqlProcessor p = new SqlProcessor(new DefaultCatalogManager());

        assertThrows(IllegalArgumentException.class,
                () -> p.process("UPDATE t SET id = 1;"));
    }

    private static <T> T getPrivateField(Object obj, String fieldName, Class<T> type) {
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            Object v = f.get(obj);
            return type.cast(v);
        } catch (Exception e) {
            throw new AssertionError("Cannot access field '" + fieldName + "' via reflection", e);
        }
    }
}
