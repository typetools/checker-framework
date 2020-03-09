package org.checkerframework.framework.source.json;

/**
 * Position in a text document expressed as zero-based line and zero-based character offset. A
 * position is between two characters like an ‘insert’ cursor in a editor. Special values like for
 * example -1 to denote the end of a line are not supported.
 */
public class Position {

    /** Use this when you have no position information. */
    public static final Position START = new Position(0, 0);

    /** Line position in a document (zero-based). */
    public final int line;

    /**
     * Character offset on a line in a document (zero-based). Assuming that the line is represented
     * as a string, the `character` value represents the gap between the `character` and `character
     * + 1`.
     */
    public final int character;

    /**
     * Create a new Position.
     *
     * @param line line position in a document (zero-based)
     * @param character character offset on a line in a document (zero-based)
     */
    public Position(int line, int character) {
        this.line = line;
        this.character = character;
    }

    @Override
    public String toString() {
        return line + "," + character;
    }
}
