package com.github.johanrg.ast1;

/**
 * @author johan
 * @since 2016-06-30.
 */
public class Location {
    private final String fileName;
    private final int line;
    private final int column;

    public Location(String fileName, int line, int column) {
        this.fileName = fileName;
        this.line = line;
        this.column = column;
    }

    public String getFileName() {
        return fileName;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }
}
