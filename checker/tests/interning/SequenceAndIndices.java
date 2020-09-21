import org.checkerframework.checker.interning.qual.*;
import org.checkerframework.dataflow.qual.Pure;

/**
 * Data structure for storing triples of a sequence and start and end indices, to represent a
 * subsequence. Requires that the sequence be interned. Used for interning the repeated finding of
 * subsequences on the same sequence.
 */
public final class SequenceAndIndices<T extends @Interned Object> {
    public T seq;
    public int start;
    public int end;

    /**
     * Create a SequenceAndIndices.
     *
     * @param seqpar an interned array
     */
    public SequenceAndIndices(T seqpar, int start, int end) {
        this.seq = seqpar;
        this.start = start;
        this.end = end;
        // assert isInterned(seq);
    }

    @SuppressWarnings("unchecked")
    @Pure
    public boolean equals(Object other) {
        if (other instanceof SequenceAndIndices) {
            // Warning only with -AcheckCastElementType.
            // TODO:: warning: (cast.unsafe)
            return equals((SequenceAndIndices<T>) other); // unchecked
        } else {
            return false;
        }
    }

    public boolean equals(SequenceAndIndices<T> other) {
        return (this.seq == other.seq) && this.start == other.start && this.end == other.end;
    }

    @Pure
    public int hashCode() {
        return seq.hashCode() + start * 30 - end * 2;
    }

    // For debugging
    @Pure
    public String toString() {
        // return "SAI(" + start + "," + end + ") from: " + ArraysMDE.toString(seq);
        return "SAI(" + start + "," + end + ") from: " + seq;
    }
}
