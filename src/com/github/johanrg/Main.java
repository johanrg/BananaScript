package com.github.johanrg;

import com.github.johanrg.compiler.Lexer;
import com.github.johanrg.compiler.Token;

public class Main {

    public static void main(String[] args) {
        Lexer lexer = new Lexer();
        //lexer.lex("file.ban", "10+,:=10 2.f 3.0 .4 int for xxx () [] {} 'j' \"hello\" ");
        //lexer.lex("", "// /* hello /*\n// ");// */22*/ 33 ");
        lexer.lex("", "/* //\n//aa\n10*/ 22");
        for (Token token : lexer.getTokens()) {
            System.out.println(token);
        }
    }
}
