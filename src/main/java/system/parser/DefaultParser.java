package system.parser;

import system.lexer.Token;
import system.parser.nodes.*;

import java.util.*;

public class DefaultParser implements Parser {
    private List<Token> tokens;
    private int curPosition;

    @Override
    public AstNode parse(List<Token> tokens) {
        this.tokens = tokens;
        this.curPosition = 0;

        String first = currentToken().getType();

        return switch (first) {
            case "SELECT" -> parseSelect();
            case "CREATE" -> parseCreate();
            case "UPDATE" -> parseUpdate();
            case "INSERT" -> parseInsert();
            default -> throw new RuntimeException("Unsupported statement: " + first);
        };
    }


    private List<ResTarget> parseTargetList() {
        List<ResTarget> targets = new ArrayList<>();
        targets.add(parseResTarget());

        while (currentToken().getType().equals("COMMA")) {
            match("COMMA");
            targets.add(parseResTarget());
        }

        return targets;
    }

    private SelectStmt parseSelect() {
        match("SELECT");

        List<ResTarget> targetList = parseTargetList();
        match("FROM");
        List<RangeVar> fromClause = parseFromClause();

        AstNode whereClause = null;
        if (currentToken().getType().equals("WHERE")) {
            match("WHERE");
            whereClause = parseWhereClause();
        }

        if (currentToken().getType().equals("SEMICOLON")) {
            match("SEMICOLON");
        }

        return new SelectStmt(targetList, fromClause, whereClause);
    }

    private CreateStmt parseCreate() {
        match("CREATE");
        match("TABLE");

        String tableName = match("IDENT").getValue();
        match("LPAREN");

        List<ColumnDef> columns = new ArrayList<>();

        do {
            String colName = match("IDENT").getValue();
            String colType = match("IDENT").getValue();
            columns.add(new ColumnDef(colName, colType));

            if (currentToken().getType().equals("COMMA")) {
                match("COMMA");
            } else break;
        } while (true);

        match("RPAREN");
        if (currentToken().getType().equals("SEMICOLON")) match("SEMICOLON");

        return new CreateStmt(tableName, columns);
    }


    private UpdateStmt parseUpdate() {
        match("UPDATE");
        String tableName = match("IDENT").getValue();
        match("SET");

        String column = match("IDENT").getValue();
        match("EQ");
        AstNode newValue = parseExpression();

        AstNode where = null;
        if (currentToken().getType().equals("WHERE")) {
            match("WHERE");
            where = parseWhereClause();
        }

        if (currentToken().getType().equals("SEMICOLON")) {
            match("SEMICOLON");
        }

        return new UpdateStmt(tableName, column, newValue, where);
    }

    private InsertStmt parseInsert() {
        match("INSERT");
        match("INTO");

        String tableName = match("IDENT").getValue();

        match("VALUES");
        match("LPAREN");

        List<AstNode> values = new ArrayList<>();

        values.add(parseInsertValue());

        while (currentToken().getType().equals("COMMA")) {
            match("COMMA");
            values.add(parseInsertValue());
        }

        match("RPAREN");

        if (currentToken().getType().equals("SEMICOLON")) {
            match("SEMICOLON");
        }

        return new InsertStmt(tableName, values);
    }

    private AstNode parseInsertValue() {
        Token token = currentToken();

        if (token.getType().equals("NUMBER")) {
            Token t = match("NUMBER");
            // оставим как INT как у тебя в parseExpression()
            int v = Integer.parseInt(t.getValue());
            return new AConst(v, "INT");
        } else if (token.getType().equals("STRING")) {
            Token t = match("STRING");
            return new AConst(t.getValue(), "STRING");
        } else if (token.getType().equals("NULL")) {
            match("NULL");
            return new AConst(null, "NULL");
        } else if (token.getType().equals("TRUE")) {
            match("TRUE");
            return new AConst(true, "BOOL");
        } else if (token.getType().equals("FALSE")) {
            match("FALSE");
            return new AConst(false, "BOOL");
        }

        throw new RuntimeException("Expected INSERT value, but got: " + token);
    }




    private ResTarget parseResTarget() {
        Token token = currentToken();

        if (token.getType().equals("ASTERISK")) {
            match("ASTERISK");
            ColumnRef columnRef = new ColumnRef("*");
            return new ResTarget(columnRef);
        } else if (token.getType().equals("IDENT")) {
            Token first = match("IDENT");
            String name = first.getValue();

            if (currentToken().getType().equals("DOT")) {
                match("DOT");
                Token second = match("IDENT");
                name = name + "." + second.getValue();
            }
            ColumnRef columnRef = new ColumnRef(name);
            return new ResTarget(columnRef);
        } else {
            throw new RuntimeException("Expected column id, but got: " + token);
        }
    }

    private List<RangeVar> parseFromClause() {
        List<RangeVar> tables = new ArrayList<>();
        tables.add(parseRangeVar());

        while (currentToken().getType().equals("COMMA")) {
            match("COMMA");
            tables.add(parseRangeVar());
        }
        return tables;
    }

    private RangeVar parseRangeVar() {
        Token tableToken = match("IDENT");
        String tableName = tableToken.getValue();
        String alias = null;
        if (currentToken().getType().equals("IDENT")) {
            Token aliasToken = match("IDENT");
            alias = aliasToken.getValue();
        }
        return new RangeVar(tableName, alias);
    }

    private AstNode parseWhereClause() {
        AstNode left = parseExpression();
        String operator = parseOperator();
        AstNode right = parseExpression();
        return new AExpr(operator, left, right);
    }

    private AstNode parseExpression() {
        Token token = currentToken();

        if (token.getType().equals("NUMBER")) {
            Token t = match("NUMBER");
            int v = Integer.parseInt(t.getValue());
            return new AConst(v, "INT");
        }
        else if (token.getType().equals("STRING")) {
            Token t = match("STRING");
            return new AConst(t.getValue(), "STRING");
        }
        else if (token.getType().equals("IDENT")) {
            Token first = match("IDENT");
            String name = first.getValue();
            if (currentToken().getType().equals("DOT")) {
                match("DOT");
                Token second = match("IDENT");
                name = name + "." + second.getValue();
            }
            return new ColumnRef(name);
        } else {
            throw new RuntimeException("Expected expression, but got: " + token);
        }
    }

    private String parseOperator() {
        Token token = currentToken();
        switch (token.getType()) {
            case "GT":
                match("GT");
                return ">";
            case "LT":
                match("LT");
                return "<";
            case "EQ":
                match("EQ");
                return "=";
            case "NEQ":
                match("NEQ");
                return "!=";
            case "GE":
                match("GE");
                return ">=";
            case "LE":
                match("LE");
                return "<=";
            default:
                throw new RuntimeException("Unknown operator: " + token);
        }
    }

    private Token currentToken() {
        if (curPosition >= tokens.size()) return new Token("EOF", "");
        return tokens.get(curPosition);
    }

    private Token match(String expectedType) {
        Token token = currentToken();
        if (token.getType().equals(expectedType)) {
            curPosition++;
            return token;
        }
        throw new RuntimeException("Expected:  " + expectedType + ", but got: " + token.getType());
    }
}
