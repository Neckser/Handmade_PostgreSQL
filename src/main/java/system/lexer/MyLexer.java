package system.lexer;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;

public class MyLexer implements Lexer {
    private static final Set<String> KEYWORDS = Set.of(
            "SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES", "UPDATE", "SET", "DELETE",
            "CREATE", "TABLE", "DROP", "ALTER", "TRUNCATE", "DISTINCT", "ALL", "AS",
            "JOIN", "INNER", "LEFT", "RIGHT", "OUTER", "FULL", "CROSS", "ON",
            "COUNT", "SUM", "AVG", "MIN", "MAX",
            "GROUP", "BY", "HAVING", "ORDER", "ASC", "DESC", "LIMIT", "OFFSET",
            "AND", "OR", "NOT", "IN", "BETWEEN", "LIKE", "IS", "NULL", "EXISTS",
            "INT", "INTEGER", "BIGINT", "SMALLINT", "TINYINT",
            "FLOAT", "DOUBLE", "DECIMAL", "NUMERIC",
            "CHAR", "VARCHAR", "TEXT", "BOOLEAN", "BOOL",
            "DATE", "TIME", "TIMESTAMP", "DATETIME",
            "PRIMARY", "KEY", "FOREIGN", "REFERENCES", "UNIQUE", "CHECK", "DEFAULT", "CONSTRAINT",
            "BEGIN", "COMMIT", "ROLLBACK", "TRANSACTION",
            "CASE", "WHEN", "THEN", "ELSE", "END",
            "UNION", "INTERSECT", "EXCEPT",
            "INDEX", "VIEW", "SEQUENCE",
            "GRANT", "REVOKE", "PRIVILEGES"
    );
    @Override
    public List<Token> tokenize(String sql) {
        List<Token> tokens = new ArrayList<>();
        int pos = 0;
        int size = sql.length();
        while (pos < size) {
            char current = sql.charAt(pos);
            if (Character.isSpaceChar(current)) {
                pos += 1;
                continue;
            }
            if (Character.isLetter(current) || current == '_') {
                int start = pos;
                while (pos < sql.length() && (Character.isLetterOrDigit(sql.charAt(pos)) || sql.charAt(pos) == '_')) {
                    pos += 1;
                }
                String word = sql.substring(start, pos);
                String type = KEYWORDS.contains(word.toUpperCase()) ? word.toUpperCase() : "IDENT";
                tokens.add(new Token(type, word));
            }

            else if (Character.isDigit(current)) {
                int start = pos;
                while (pos < sql.length() && Character.isDigit(sql.charAt(pos))) {
                    pos += 1;
                }
                if (pos < sql.length() && sql.charAt(pos) == '.') {
                    pos += 1;
                    while (pos < sql.length() && Character.isDigit(sql.charAt(pos))) {
                        pos += 1;
                    }
                }
                String number = sql.substring(start, pos);
                tokens.add(new Token("NUMBER", number));
            }
            else if (current == '\'' || current == '"') {
                pos += 1;
                StringBuilder sb = new StringBuilder();
                while (pos < sql.length() && sql.charAt(pos) != current) {
                    if (sql.charAt(pos) == '\\' && pos + 1 < sql.length()) {
                        pos++;
                    }
                    sb.append(sql.charAt(pos));
                    pos++;
                }
                if (pos < sql.length() && sql.charAt(pos) == current) {
                    pos += 1;
                }
                String stringValue = sb.toString();
                tokens.add(new Token("STRING", stringValue));
            }
            else {
                Token token = null;
                switch (current) {
                    case ',':
                        token = new Token("COMMA", ",");
                        break;
                    case ';':
                        token = new Token("SEMICOLON", ";");
                        break;
                    case '(':
                        token = new Token("LPAREN", "(");
                        break;
                    case ')':
                        token = new Token("RPAREN", ")");
                        break;
                    case '.':
                        token = new Token("DOT", ".");
                        break;
                    case '=':
                        token = new Token("EQ", "=");
                        break;
                    case '>':
                        if (pos + 1 < sql.length() && sql.charAt(pos + 1) == '=') {
                            token = new Token("GTE", ">=");
                            pos += 1;
                        } else {
                            token = new Token("GT", ">");
                        }
                        break;
                    case '<':
                        if (pos + 1 < sql.length() && sql.charAt(pos + 1) == '=') {
                            token = new Token("LTE", "<=");
                            pos += 1;
                        } else if (pos + 1 < sql.length() && sql.charAt(pos + 1) == '>') {
                            token = new Token("NEQ", "<>");
                            pos += 1;
                        } else {
                            token = new Token("LT", "<");
                        }
                        break;
                    case '!':
                        if (pos + 1 < sql.length() && sql.charAt(pos + 1) == '=') {
                            token = new Token("NEQ", "!=");
                            pos += 1;
                        }
                        break;
                    case '+':
                        token = new Token("PLUS", "+");
                        break;
                    case '-':
                        token = new Token("MINUS", "-");
                        break;
                    case '*':
                        token = new Token("STAR", "*");
                        break;
                    case '/':
                        token = new Token("SLASH", "/");
                        break;
                }

                if (token != null) {
                    tokens.add(token);
                }
                pos += 1;
            }
        }

        return tokens;
    }
}