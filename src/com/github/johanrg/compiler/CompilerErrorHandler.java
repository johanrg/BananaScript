package com.github.johanrg.compiler;

/**
 * @author Johan Gustafsson
 * @since 7/12/2016.
 */
public class CompilerErrorHandler {
    static void error(String error, Location location) throws CompilerException {
        throw new CompilerException(String.format("%s : error : (%d,%d) %s", location.getFileName(), location.getLine(), location.getColumn(),
                error));
    }
}
