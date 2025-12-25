package system.parser;

import system.lexer.Token;
import system.nodes.*;

import java.util.ArrayList;
import java.util.List;

public class MyParser implements Parser {
    private List<Token> tokens;
    private int pos;

    @Override
    public AstNode parse(List<Token> tokens) {
        this.tokens = tokens;
        this.pos = 0;
        return parseSelectStmt();
    }

    private SelectStmt parseSelectStmt() {
        expect("SELECT");
        List<ResTarget> targetList = parseTargetList();
        expect("FROM");
        List<RangeVar> fromClause = parseFromClause();
        AstNode whereClause = null;
        if (hasToken("WHERE")) {
            nextToken();
            whereClause = parseExpression();
        }
        if (hasToken("SEMICOLON")) {
            nextToken();
        }
        return new SelectStmt(targetList, fromClause, whereClause);
    }

    private List<ResTarget> parseTargetList() {
        List<ResTarget> targets = new ArrayList<>();
        targets.add(new ResTarget(parseColumnRef()));
        while (hasToken("COMMA")) {
            nextToken();
            targets.add(new ResTarget(parseColumnRef()));
        }
        return targets;
    }

    private List<RangeVar> parseFromClause() {
        List<RangeVar> tables = new ArrayList<>();
        tables.add(new RangeVar(expectIdent().getValue()));
        while (hasToken("COMMA")) {
            nextToken();
            tables.add(new RangeVar(expectIdent().getValue()));
        }
        return tables;
    }

    private AstNode parseExpression() {
        return parseComparison();
    }

    private AstNode parseComparison() {
        AstNode left = parsePrimary();
        if (hasToken("GT") || hasToken("LT") || hasToken("EQ") || hasToken("GTE") || hasToken("LTE") || hasToken("NEQ")) {
            String operator = currentToken().getType();
            nextToken();
            AstNode right = parsePrimary();
            return new AExpr(operator, left, right);
        }
        return left;
    }

    private AstNode parsePrimary() {
        if (hasToken("IDENT")) {
            return new ColumnRef(expectIdent().getValue());
        } else if (hasToken("NUMBER")) {
            Token number = nextToken();
            return new AConst(Integer.parseInt(number.getValue()));
        } else if (hasToken("STRING")) {
            Token string = nextToken();
            return new AConst(string.getValue());
        } else {
            throw new RuntimeException("Unexpected: " + currentToken());
        }
    }

    private ColumnRef parseColumnRef() {
        return new ColumnRef(expectIdent().getValue());
    }

    private Token currentToken() {
        if (pos >= tokens.size()) {
            throw new RuntimeException("Unexpected end of input");
        }
        return tokens.get(pos);
    }

    private Token nextToken() {
        Token token = currentToken();
        pos += 1;
        return token;
    }

    private boolean hasToken(String type) {
        if (pos >= tokens.size()) {
            return false;
        }
        return type.equals(currentToken().getType());
    }

    private void expect(String type) {
        if (!hasToken(type)) {
            throw new RuntimeException("Expected " + type + " but found " + currentToken().getType());
        }
        nextToken();
    }

    private Token expectIdent() {
        if (!hasToken("IDENT")) {
            throw new RuntimeException("Expected IDENT but found " + currentToken().getType());
        }
        return nextToken();
    }
}