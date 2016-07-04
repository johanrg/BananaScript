package com.github.johanrg;

import com.github.johanrg.compiler.CompilerException;
import com.github.johanrg.compiler.Lexer;
import com.github.johanrg.compiler.Parser;

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

            }
        } catch (CompilerException e) {
            System.err.println(e.getMessage());
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}
