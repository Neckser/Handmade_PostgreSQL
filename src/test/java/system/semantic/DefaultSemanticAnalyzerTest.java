package system.semantic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import system.catalog.manager.DefaultCatalogManager;
import system.catalog.model.ColumnDefinition;
import system.catalog.model.TableDefinition;
import system.catalog.model.TypeDefinition;
import system.parser.nodes.*;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DefaultSemanticAnalyzerTest {

    private DefaultCatalogManager catalog;
    private DefaultSemanticAnalyzer analyzer;

    private String users;
    private String orders;

    @BeforeEach
    void setup() {
        deleteIfExists("table_definitions.dat");
        deleteIfExists("column_definitions.dat");
        deleteIfExists("types_definitions.dat");

        catalog = new DefaultCatalogManager();
        analyzer = new DefaultSemanticAnalyzer();

        ensureTypeAliasExists("INT", 4);
        ensureTypeAliasExists("STRING", 255);

        users = "users_semantic_test";
        orders = "orders_semantic_test";

        TypeDefinition intType = catalog.getType("INT");
        TypeDefinition strType = catalog.getType("STRING");
        assertNotNull(intType);
        assertNotNull(strType);

        catalog.createTable(users, List.of(
                new ColumnDefinition(intType.getOid(), "age", 0),
                new ColumnDefinition(strType.getOid(), "name", 1)
        ));

        catalog.createTable(orders, List.of(
                new ColumnDefinition(intType.getOid(), "age", 0),
                new ColumnDefinition(strType.getOid(), "name", 1)
        ));
    }


    @Test
    void select_simpleTargets_noWhere() {
        SelectStmt stmt = new SelectStmt(
                List.of(new ResTarget(new ColumnRef("name"))),
                List.of(new RangeVar(users)),
                null
        );

        QueryTree tree = analyzer.analyze(stmt, catalog);

        assertEquals("SELECT", tree.getQueryType());
        assertEquals(List.of(users), tree.getFromTables());
        assertEquals(1, tree.getTargetList().size());
        assertNull(tree.getFilter());
    }

    @Test
    void select_where_columnVsConst_compatibleTypes() {
        AExpr where = new AExpr(">", new ColumnRef("age"), new AConst(18, "INT"));

        SelectStmt stmt = new SelectStmt(
                List.of(new ResTarget(new ColumnRef("name"))),
                List.of(new RangeVar(users)),
                where
        );

        QueryTree tree = analyzer.analyze(stmt, catalog);

        assertEquals("SELECT", tree.getQueryType());
        assertNotNull(tree.getFilter());
        assertEquals(">", tree.getFilter().getOp());
        assertEquals("age", tree.getFilter().getLeft().getColumn().getName());
        assertEquals(18, tree.getFilter().getRightValue());
    }

    @Test
    void select_where_columnVsColumn_compatibleTypes() {
        AExpr where = new AExpr("=", new ColumnRef("age"), new ColumnRef(users + ".age"));

        SelectStmt stmt = new SelectStmt(
                List.of(new ResTarget(new ColumnRef("age"))),
                List.of(new RangeVar(users)),
                where
        );

        QueryTree tree = analyzer.analyze(stmt, catalog);

        assertEquals("SELECT", tree.getQueryType());
        assertNotNull(tree.getFilter());
        assertEquals("=", tree.getFilter().getOp());
        assertTrue(tree.getFilter().getRightValue().toString().contains("age"));
    }

    @Test
    void select_qualifiedColumnResolvesCorrectly() {
        SelectStmt stmt = new SelectStmt(
                List.of(new ResTarget(new ColumnRef(users + ".name"))),
                List.of(new RangeVar(users)),
                null
        );

        QueryTree tree = analyzer.analyze(stmt, catalog);

        assertEquals("SELECT", tree.getQueryType());
        assertEquals(1, tree.getTargetList().size());
        assertEquals("name", tree.getTargetList().get(0).getColumn().getName());
    }

    @Test
    void select_unknownTableInFrom_shouldFail() {
        SelectStmt stmt = new SelectStmt(
                List.of(new ResTarget(new ColumnRef("name"))),
                List.of(new RangeVar("no_such_table")),
                null
        );

        RuntimeException ex = assertThrows(RuntimeException.class, () -> analyzer.analyze(stmt, catalog));
        assertTrue(ex.getMessage().toLowerCase().contains("table") || ex.getMessage().toLowerCase().contains("column"));
    }

    @Test
    void select_unknownColumn_shouldFail() {
        SelectStmt stmt = new SelectStmt(
                List.of(new ResTarget(new ColumnRef("no_such_col"))),
                List.of(new RangeVar(users)),
                null
        );

        RuntimeException ex = assertThrows(RuntimeException.class, () -> analyzer.analyze(stmt, catalog));
        assertTrue(ex.getMessage().toLowerCase().contains("column not found"));
    }

    @Test
    void select_ambiguousColumn_whenTwoFromTables_shouldFail() {
        SelectStmt stmt = new SelectStmt(
                List.of(new ResTarget(new ColumnRef("age"))),
                List.of(new RangeVar(users), new RangeVar(orders)),
                null
        );

        RuntimeException ex = assertThrows(RuntimeException.class, () -> analyzer.analyze(stmt, catalog));
        assertTrue(ex.getMessage().toLowerCase().contains("ambiguous"));
    }

    @Test
    void select_where_incompatibleTypes_shouldFail() {
        AExpr where = new AExpr(">", new ColumnRef("age"), new AConst("abc", "STRING"));

        SelectStmt stmt = new SelectStmt(
                List.of(new ResTarget(new ColumnRef("name"))),
                List.of(new RangeVar(users)),
                where
        );

        RuntimeException ex = assertThrows(RuntimeException.class, () -> analyzer.analyze(stmt, catalog));
        assertTrue(ex.getMessage().toLowerCase().contains("incompatible"));
    }

    @Test
    void select_where_unknownConstType_shouldFail() {
        AExpr where = new AExpr("=", new ColumnRef("age"), new AConst(1, "UNKNOWN_TYPE"));

        SelectStmt stmt = new SelectStmt(
                List.of(new ResTarget(new ColumnRef("age"))),
                List.of(new RangeVar(users)),
                where
        );

        RuntimeException ex = assertThrows(RuntimeException.class, () -> analyzer.analyze(stmt, catalog));
        assertTrue(ex.getMessage().toLowerCase().contains("unknown type"));
    }

    @Test
    void createTable_ok_createsInCatalog() {
        CreateStmt stmt = new CreateStmt(
                "accounts_semantic_test",
                List.of(
                        new ColumnDef("id", "INT"),
                        new ColumnDef("balance", "INT")
                )
        );

        QueryTree tree = analyzer.analyze(stmt, catalog);

        assertEquals("CREATE", tree.getQueryType());
        assertEquals("accounts_semantic_test", tree.getTableName());
        assertEquals(2, tree.getCreateColumns().size());

        TableDefinition created = catalog.getTable("accounts_semantic_test");
        assertNotNull(created);
    }

    @Test
    void createTable_unknownType_shouldFail() {
        CreateStmt stmt = new CreateStmt(
                "bad_table_semantic_test",
                List.of(new ColumnDef("id", "UNKNOWN_TYPE"))
        );

        RuntimeException ex = assertThrows(RuntimeException.class, () -> analyzer.analyze(stmt, catalog));
        assertTrue(ex.getMessage().toLowerCase().contains("unknown type"));
    }

    @Test
    void update_ok_withWhere() {
        UpdateStmt stmt = new UpdateStmt(
                users,
                "age",
                new AConst(30, "INT"),
                new AExpr("=", new ColumnRef("name"), new AConst("Alice", "STRING"))
        );

        QueryTree tree = analyzer.analyze(stmt, catalog);

        assertEquals("UPDATE", tree.getQueryType());
        assertEquals(users, tree.getTableName());
        assertNotNull(tree.getSetValues());
        assertEquals(1, tree.getSetValues().size());
        assertNotNull(tree.getFilter());
        assertEquals("=", tree.getFilter().getOp());
    }

    @Test
    void update_unknownTable_shouldFail() {
        UpdateStmt stmt = new UpdateStmt(
                "no_such_table",
                "age",
                new AConst(30, "INT"),
                null
        );

        RuntimeException ex = assertThrows(RuntimeException.class, () -> analyzer.analyze(stmt, catalog));
        assertTrue(ex.getMessage().toLowerCase().contains("table not found"));
    }

    @Test
    void update_unknownColumn_shouldFail() {
        UpdateStmt stmt = new UpdateStmt(
                users,
                "no_such_col",
                new AConst(30, "INT"),
                null
        );

        RuntimeException ex = assertThrows(RuntimeException.class, () -> analyzer.analyze(stmt, catalog));
        assertTrue(ex.getMessage().toLowerCase().contains("column not found"));
    }

    @Test
    void update_nonConstSetValue_shouldFail() {
        UpdateStmt stmt = new UpdateStmt(
                users,
                "age",
                new ColumnRef("age"),
                null
        );

        RuntimeException ex = assertThrows(RuntimeException.class, () -> analyzer.analyze(stmt, catalog));
        assertTrue(ex.getMessage().toLowerCase().contains("only constant"));
    }


    private void deleteIfExists(String filename) {
        File f = new File(filename);
        if (f.exists() && !f.delete()) {
            throw new RuntimeException("Cannot delete file: " + filename);
        }
    }

    private void ensureTypeAliasExists(String name, int byteLen) {
        if (catalog.getType(name) != null) return;

        try {
            Method m = DefaultCatalogManager.class.getDeclaredMethod("createType", String.class, int.class);
            m.setAccessible(true);
            m.invoke(catalog, name, byteLen);
        } catch (Exception e) {
            fail("Cannot create type alias '" + name + "' via reflection. " + e);
        }

        assertNotNull(catalog.getType(name), "Type alias '" + name + "' must exist after creation");
    }
}
