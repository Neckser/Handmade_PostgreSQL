package system.lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DefaultLexer implements Lexer {
    private static final Set<String> KEYWORDS = Set.of(
            "SELECT", "FROM", "WHERE", "AND", "OR",
            "INSERT", "INTO", "VALUES", "UPDATE", "SET",
            "DELETE", "CREATE", "TABLE"
    );

    public List<Token> tokenize(String sql) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        while (i < sql.length()) {
            char c = sql.charAt(i);

            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            switch (c) {
                case ',' -> {
                    tokens.add(new Token("COMMA", ","));
                    i++;
                    continue;
                }
                case ';' -> {
                    tokens.add(new Token("SEMICOLON", ";"));
                    i++;
                    continue;
                }
                case '*' -> {
                    tokens.add(new Token("ASTERISK", "*"));
                    i++;
                    continue;
                }
                case '.' -> {
                    tokens.add(new Token("DOT", "."));
                    i++;
                    continue;
                }
                case '(' -> {
                    tokens.add(new Token("LPAREN", "("));
                    i++;
                    continue;
                }
                case ')' -> {
                    tokens.add(new Token("RPAREN", ")"));
                    i++;
                    continue;
                }
            }

            if (c == '>') {
                if (i + 1 < sql.length() && sql.charAt(i + 1) == '=') {
                    tokens.add(new Token("GE", ">="));
                    i += 2;
                } else {
                    tokens.add(new Token("GT", ">"));
                    i++;
                }
                continue;
            }

            if (c == '<') {
                if (i + 1 < sql.length() && sql.charAt(i + 1) == '=') {
                    tokens.add(new Token("LE", "<="));
                    i += 2;
                } else if (i + 1 < sql.length() && sql.charAt(i + 1) == '>') {
                    tokens.add(new Token("NEQ", "<>"));
                    i += 2;
                } else {
                    tokens.add(new Token("LT", "<"));
                    i++;
                }
                continue;
            }

            if (c == '=') {
                tokens.add(new Token("EQ", "="));
                i++;
                continue;
            }

            if (Character.isDigit(c)) {
                StringBuilder number = new StringBuilder();
                while (i < sql.length() && Character.isDigit(sql.charAt(i))) {
                    number.append(sql.charAt(i++));
                }
                tokens.add(new Token("NUMBER", number.toString()));
                continue;
            }

            if (c == '\'') {
                i++;
                StringBuilder str = new StringBuilder();
                while (i < sql.length()) {
                    char ch = sql.charAt(i);
                    if (ch == '\'') {
                        if (i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                            str.append('\'');
                            i += 2;
                        } else {
                            i++;
                            break;
                        }
                    } else {
                        str.append(ch);
                        i++;
                    }
                }
                tokens.add(new Token("STRING", str.toString()));
                continue;
            }

            if (Character.isLetter(c) || c == '_') {
                StringBuilder ident = new StringBuilder();
                while (i < sql.length() &&
                        (Character.isLetterOrDigit(sql.charAt(i)) || sql.charAt(i) == '_')) {
                    ident.append(sql.charAt(i++));
                }
                String word = ident.toString();
                String wordUpper = word.toUpperCase();
                if (KEYWORDS.contains(wordUpper)) {
                    tokens.add(new Token(wordUpper, wordUpper));
                } else {
                    tokens.add(new Token("IDENT", word));
                }
                continue;
            }

            throw new RuntimeException("Unexpected character: " + c);
        }
        return tokens;
    }
}
