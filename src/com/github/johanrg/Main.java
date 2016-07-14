package com.github.johanrg;

import com.github.johanrg.backend.Diagram;
import com.github.johanrg.frontend.CompilerException;
import com.github.johanrg.frontend.Lexer;
import com.github.johanrg.frontend.Parser;

import java.io.*;
import java.nio.charset.Charset;


public class Main {
     static String readFile(String file, Charset cs) throws IOException {
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
        try {
            //String source = readFile("/home/johan/sourcefile", Charset.forName("utf8"));
            String source = readFile("scriptfile", Charset.forName("utf8"));
            Lexer lexer = new Lexer();
            lexer.lex("Lexer.java", source);
            lexer.getTokens().forEach(System.out::println);
            if (lexer.getErrors().size() > 0) {
                lexer.getErrors().forEach(System.out::println);
            } else {
                Parser parser = new Parser(lexer.getTokens());
                new Diagram(parser.getRoot());
            }
        } catch (CompilerException | IOException e) {
            System.err.println(e.getMessage());
        }
    }
}
