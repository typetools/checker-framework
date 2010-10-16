package checkers.nonnull;

import checkers.quals.*;

import javax.annotation.processing.*;
import javax.lang.model.element.*;

/**
 * Represents the range for which a variable is "valid" with respect to some
 * property (for instance, the range for which a variable is nonnull).
 * Instances of {@link FlowScope} are produced by {@link FlowVisitor}, which
 * provides a flow-sensitive nonnull analysis for the {@link NonnullChecker}.
 * It uses source positions (character offsets from the start of a compilation
 * unit), so it is only valid for source-level analysis.
 *
 * @see SourcePositions
 */
@DefaultQualifier("checkers.nullness.quals.NonNull")
class FlowScope {

    /** The variable for this scope. */
    private final Element elt;

    /** The starting source position for which the variable is valid. */
    private final long start;

    /** The ending source position for which the variable is valid. */
    private long end;

    /** 
     * Creates a {@link FlowScope} for the given {@link Element} that begins a
     * the given source position.
     *
     * @param elt the variable for the scope
     * @param start the starting position for which the variable is valid
     */
    public FlowScope(Element elt, long start) {
        if (elt == null)
            throw new IllegalArgumentException("null element");
        this.elt = elt;
        this.start = start;
        this.end = 0;
    }

    /**
     * Defines the ending location for the scope. Once given, the ending
     * location cannot be provided again.
     *
     * @param end the ending position for which the variable is valid
     * @throws IllegalStateException if the scope has already been completed
     */
    public void complete(long end) {
        if (end < this.start)
            return;
        if (this.end == 0)
            this.end = end;
        else throw new IllegalStateException("already completed");
    }

    /**
     * Determines whether the scope has an ending location.
     *
     * @return true if the scope has been completed, false otherwise
     */
    public boolean isComplete() {
        return this.end != 0;
    }

    /**
     * Retrieves the variable for this scope as an {@link Element}.
     *
     * @return the variable for this scope
     */
    public Element getElement() {
        return this.elt;
    }

    /**
     * Determines whether the scope "contains" the given variable at the given
     * position, i.e., whether the usage of the element at the given position
     * is valid as determined by this scope.
     *
     * @param elt the variable to check
     * @param pos the position of the usage of the variable
     */
    public boolean contains(@Nullable Element elt, long pos) {
        return this.elt.equals(elt) && pos >= this.start && 
            pos <= this.end;
    }

    @Override
    public String toString() {
        return "[FlowScope " + elt + ", " + start + "-" + end + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof FlowScope) {
            FlowScope fs = (FlowScope)o;
            return this.elt.equals(fs.elt) && this.start == fs.start &&
                this.end == fs.end;
        }
        return false;
    }
}
