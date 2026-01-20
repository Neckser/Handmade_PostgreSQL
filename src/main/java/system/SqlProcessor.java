package system;

import system.ast.AConst;
import system.ast.AExpr;
import system.ast.ColumnRef;
import system.ast.Expr;
import system.ast.QueryTree;
import system.ast.QueryType;
import system.ast.RangeTblEntry;
import system.ast.TargetEntry;
import system.catalog.manager.CatalogManager;
import system.lexer.DefaultLexer;
import system.lexer.Lexer;
import system.lexer.Token;
import system.parser.DefaultParser;
import system.parser.Parser;
import system.parser.nodes.ColumnDef;
import system.parser.nodes.CreateStmt;
import system.parser.nodes.RangeVar;
import system.parser.nodes.ResTarget;
import system.parser.nodes.SelectStmt;
import system.parser.nodes.UpdateStmt;

import java.util.ArrayList;
import java.util.List;

public class SqlProcessor {

    private final Lexer lexer;
    private final Parser parser;
    private final CatalogManager catalogManager;

    public SqlProcessor(CatalogManager catalogManager) {
        this(new DefaultLexer(), new DefaultParser(), catalogManager);
    }

    public SqlProcessor(Lexer lexer, Parser parser, CatalogManager catalogManager) {
        this.lexer = lexer;
        this.parser = parser;
        this.catalogManager = catalogManager;
    }

    public QueryTree process(String sql) {
        if (sql == null) throw new IllegalArgumentException("sql is null");

        List<Token> tokens = lexer.tokenize(sql);
        if (tokens.isEmpty()) throw new IllegalArgumentException("Empty SQL");

        String first = tokens.get(0).getType();

        return switch (first) {
            case "CREATE", "SELECT", "UPDATE" -> translateParsedAst(parser.parse(tokens));
            case "INSERT" -> parseInsertToQueryTree(tokens);
            default -> throw new IllegalArgumentException("Unsupported statement: " + first);
        };
    }

    private QueryTree translateParsedAst(system.parser.nodes.AstNode ast) {
        if (ast instanceof CreateStmt cs) {
            return translateCreate(cs);
        }
        if (ast instanceof SelectStmt ss) {
            return translateSelect(ss);
        }
        if (ast instanceof UpdateStmt) {
            throw new IllegalArgumentException("UPDATE is not supported in HW5 planner/executor");
        }
        throw new IllegalArgumentException("Unsupported AST node: " + ast.getClass().getSimpleName());
    }

    private QueryTree translateCreate(CreateStmt cs) {
        QueryTree q = new QueryTree();
        q.commandType = QueryType.CREATE;
        q.rangeTable.add(new RangeTblEntry(cs.getTableName()));

        for (ColumnDef col : cs.getColumns()) {
            TargetEntry te = new TargetEntry(null, col.getName());
            te.resultType = col.getType();
            q.targetList.add(te);
        }
        return q;
    }

    private QueryTree translateSelect(SelectStmt ss) {
        QueryTree q = new QueryTree();
        q.commandType = QueryType.SELECT;

        if (ss.getFromClause() != null && !ss.getFromClause().isEmpty()) {
            RangeVar rv = ss.getFromClause().get(0);
            RangeTblEntry rte = new RangeTblEntry(rv.getRelName());
            rte.alias = rv.getAlias();
            rte.index = 0;
            q.rangeTable.add(rte);
        } else {
            throw new IllegalArgumentException("SELECT without FROM is not supported");
        }

        for (ResTarget rt : ss.getTargetList()) {
            system.parser.nodes.ColumnRef c = rt.getVal();
            String[] qc = splitQualifiedColumn(c.getName());
            ColumnRef expr = (qc[0] == null) ? new ColumnRef(qc[1]) : new ColumnRef(qc[0], qc[1]);
            TargetEntry te = new TargetEntry(expr, null);
            q.targetList.add(te);
        }

        if (ss.getWhereClause() != null) {
            Expr where = translateExpr(ss.getWhereClause());
            q.whereClause = where;
        }

        return q;
    }

    private Expr translateExpr(system.parser.nodes.AstNode node) {
        if (node instanceof system.parser.nodes.ColumnRef c) {
            String[] qc = splitQualifiedColumn(c.getName());
            return (qc[0] == null) ? new ColumnRef(qc[1]) : new ColumnRef(qc[0], qc[1]);
        }
        if (node instanceof system.parser.nodes.AConst c) {
            return new AConst(c.getValue());
        }
        if (node instanceof system.parser.nodes.AExpr e) {
            Expr left = translateExpr((system.parser.nodes.AstNode) e.getLeft());
            Expr right = translateExpr((system.parser.nodes.AstNode) e.getRight());
            return new AExpr(e.getOp(), left, right);
        }
        throw new IllegalArgumentException("Unsupported expression node: " + node.getClass().getSimpleName());
    }


    private String[] splitQualifiedColumn(String name) {
        if (name == null) return new String[]{null, null};
        String trimmed = name.trim();
        int dot = trimmed.indexOf('.');
        if (dot >= 0) {
            String t = trimmed.substring(0, dot);
            String c = trimmed.substring(dot + 1);
            return new String[]{t, c};
        }
        return new String[]{null, trimmed};
    }

    private QueryTree parseInsertToQueryTree(List<Token> tokens) {
        int pos = 0;

        pos = expect(tokens, pos, "INSERT");
        pos = expect(tokens, pos, "INTO");

        Token tableTok = expectToken(tokens, pos, "IDENT");
        String tableName = tableTok.getValue();
        pos++;

        pos = expect(tokens, pos, "VALUES");
        pos = expect(tokens, pos, "LPAREN");

        List<Expr> values = new ArrayList<>();
        boolean first = true;
        while (pos < tokens.size()) {
            Token t = tokens.get(pos);

            if (t.getType().equals("RPAREN")) {
                pos++;
                break;
            }

            if (!first) {
                pos = expect(tokens, pos, "COMMA");
                t = tokens.get(pos);
            }
            first = false;

            values.add(parseValueToken(t));
            pos++;
        }

        if (pos < tokens.size() && tokens.get(pos).getType().equals("SEMICOLON")) pos++;

        QueryTree q = new QueryTree();
        q.commandType = QueryType.INSERT;
        q.rangeTable.add(new RangeTblEntry(tableName));
        for (Expr v : values) {
            q.targetList.add(new TargetEntry(v, null));
        }
        return q;
    }

    private Expr parseValueToken(Token t) {
        return switch (t.getType()) {
            case "NUMBER" -> {
                String s = t.getValue();
                if (s.contains(".")) {
                    yield new AConst(Double.parseDouble(s));
                } else {
                    yield new AConst(Long.parseLong(s));
                }
            }
            case "STRING" -> new AConst(t.getValue());
            case "NULL" -> new AConst(null);
            case "TRUE" -> new AConst(true);
            case "FALSE" -> new AConst(false);
            default -> throw new IllegalArgumentException("Unsupported INSERT value token: " + t);
        };
    }

    private int expect(List<Token> tokens, int pos, String type) {
        expectToken(tokens, pos, type);
        return pos + 1;
    }

    private Token expectToken(List<Token> tokens, int pos, String type) {
        if (pos >= tokens.size()) {
            throw new IllegalArgumentException("Expected token " + type + " but reached end of input");
        }
        Token t = tokens.get(pos);
        if (!t.getType().equals(type)) {
            throw new IllegalArgumentException("Expected token " + type + " but got " + t.getType() + " (" + t.getValue() + ")");
        }
        return t;
    }
}