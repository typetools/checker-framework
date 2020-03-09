package org.checkerframework.framework.source.json;

/**
 * A range in a text document expressed as (zero-based) start and end positions. A range is
 * comparable to a selection in an editor. Therefore the end position is exclusive. If you want to
 * specify a range that contains a line including the line ending character(s) then use an end
 * position denoting the start of the next line.
 */
public class Range {

    /** Use this when you do not know the range but have to supply a Range object. */
    public static final Range NONE = new Range(Position.START, Position.START);

    /** The range's start position. */
    public Position start;
    /** The range's end position. */
    public Position end;

    /**
     * Create a new Range.
     *
     * @param start the range's start position
     * @param end the range's end position
     */
    public Range(Position start, Position end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public String toString() {
        return start + "-" + end;
    }
}
