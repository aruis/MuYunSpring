package net.ximatai.muyun.spring.common.formula;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class FormulaTokenizer {
    enum TokenType {
        VALUE, OP, LPAREN, RPAREN, COMMA
    }

    record Token(TokenType type, String value) {
    }

    List<Token> tokenize(String expression) {
        if (expression == null) {
            return List.of();
        }
        List<Token> tokens = new ArrayList<>();
        Set<String> twoCharOps = Set.of(">=", "<=", "==", "!=", "&&", "||");
        Set<Character> oneCharOps = Set.of('+', '-', '*', '/', '%', '>', '<', '!', '=');
        String text = expression;
        int i = 0;
        while (i < text.length()) {
            char ch = text.charAt(i);
            if (Character.isWhitespace(ch)) {
                i++;
                continue;
            }
            if (ch == '{') {
                int end = text.indexOf('}', i + 1);
                if (end > -1) {
                    tokens.add(new Token(TokenType.VALUE, text.substring(i, end + 1)));
                    i = end + 1;
                    continue;
                }
            }
            if (ch == '\'' || ch == '"') {
                int end = scanQuoted(text, i, ch);
                tokens.add(new Token(TokenType.VALUE, text.substring(i, end + 1)));
                i = end + 1;
                continue;
            }
            if (i + 1 < text.length() && twoCharOps.contains(text.substring(i, i + 2))) {
                tokens.add(new Token(TokenType.OP, text.substring(i, i + 2)));
                i += 2;
                continue;
            }
            if (oneCharOps.contains(ch)) {
                tokens.add(new Token(TokenType.OP, String.valueOf(ch)));
                i++;
                continue;
            }
            if (ch == '(') {
                tokens.add(new Token(TokenType.LPAREN, "("));
                i++;
                continue;
            }
            if (ch == ')') {
                tokens.add(new Token(TokenType.RPAREN, ")"));
                i++;
                continue;
            }
            if (ch == ',') {
                tokens.add(new Token(TokenType.COMMA, ","));
                i++;
                continue;
            }
            int j = i;
            while (j < text.length() && !isBoundary(text, j, oneCharOps, twoCharOps)) {
                j++;
            }
            tokens.add(new Token(TokenType.VALUE, text.substring(i, j)));
            i = j;
        }
        return tokens;
    }

    private int scanQuoted(String text, int start, char quote) {
        int i = start + 1;
        while (i < text.length()) {
            if (text.charAt(i) == quote && text.charAt(i - 1) != '\\') {
                return i;
            }
            i++;
        }
        return text.length() - 1;
    }

    private boolean isBoundary(String text, int index, Set<Character> oneCharOps, Set<String> twoCharOps) {
        char ch = text.charAt(index);
        String two = index + 1 < text.length() ? text.substring(index, index + 2) : "";
        return Character.isWhitespace(ch)
                || ch == '{'
                || ch == '\''
                || ch == '"'
                || ch == '('
                || ch == ')'
                || ch == ','
                || oneCharOps.contains(ch)
                || twoCharOps.contains(two);
    }
}
