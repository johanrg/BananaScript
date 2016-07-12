package com.github.johanrg.compiler;

/**
 * @author Johan Gustafsson
 * @since 7/11/2016.
 */
class Symbols {
    enum Keyword {
        IF("if"),
        ELSE("else"),
        WHILE("while"),
        FOR("for"),
        ;

        private final String identifier;

        Keyword(String identifier) {
            this.identifier = identifier;
        }
    }

    static Keyword match(String identifier) {
        for (Keyword keyword : Keyword.values()) {
            if (keyword.identifier.equals(identifier)) {
                return keyword;
            }
        }
        return null;
    }
}
