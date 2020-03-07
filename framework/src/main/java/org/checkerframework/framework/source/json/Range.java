package org.checkerframework.framework.source.json;

public class Range {

    public static final Range NONE = new Range(Position.START, Position.START);

    public Position start;
    public Position end;

    public Range(Position start, Position end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public String toString() {
        return start + "-" + end;
    }
}
