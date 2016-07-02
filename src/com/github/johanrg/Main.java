package com.github.johanrg;

import com.github.johanrg.compiler.Lexer;
import java.io.*;
import java.nio.charset.Charset;

public class Main {
    public static String readFile(String file, Charset cs) throws IOException {
        try (FileInputStream stream = new FileInputStream(file)) {
            Reader reader = new BufferedReader(new InputStreamReader(stream, cs));
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[8192];
            int read;
            while ((read = reader.read(buffer, 0, buffer.length)) > 0) {
                builder.append(buffer, 0, read);
            }
            return builder.toString();
        }
    }

    public static void main(String[] args) {
        double a = .2;
        try {
            String source = readFile("/home/johan/IdeaProjects/BananaScript/src/com/github/johanrg/compiler/Lexer.java", Charset.forName("utf8"));
            Lexer lexer = new Lexer();
            lexer.lex("Lexer.java", source);
            if (lexer.getErrors().size() > 0) {
                lexer.getErrors().forEach(System.out::println);
            } else {
                lexer.getTokens().forEach(System.out::println);
                System.out.printf("Number of tokens: %d\n", lexer.getTokens().size());
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}
