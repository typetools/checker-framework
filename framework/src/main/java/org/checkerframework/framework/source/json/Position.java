package org.checkerframework.framework.source.json;

public class Position {

    /**
     * Use this when you have no position information. The LSP specification says that values such
     * as -1 are not supported.
     */
    public static final Position START = new Position(0, 0);

    /** Line position in a document (zero-based). */
    public final int line;

    /**
     * Character offset on a line in a document (zero-based). Assuming that the line is represented
     * as a string, the `character` value represents the gap between the `character` and `character
     * + 1`.
     */
    public final int character;

    public Position(int line, int character) {
        this.line = line;
        this.character = character;
    }

    @Override
    public String toString() {
        return line + "," + character;
    }
}
