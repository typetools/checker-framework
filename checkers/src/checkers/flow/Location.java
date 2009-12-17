package checkers.flow;

import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.SourcePositions;

/**
 * Represents the location of a {@link Tree}. A tree's location may be uniquely
 * identified by its source position (character offset) and {@link Kind}.
 * Source position alone is not sufficient, since many trees may share the same
 * source position (for instance, the member select "a.b" and the identifier "a"
 * in that select have the same source position, but different kinds).
 *
 * @see SourcePositions for determining a tree's source position (character offset)
 */
public final class Location {

    /** The tree's source position (as a character offset). */
    private final Long position;

    /** The tree's kind. */
    private final Tree tree;

    /**
     * Creates a new {@link Location} from a position and {@link Kind}.
     *
     * <p>
     *
     * Positions can be obtained from a tree using the {@link SourcePositions}
     * class.
     *
     * @param position the tree's position
     * @param tree the tree's kind
     *
     * @see SourcePositions
     */
    public Location(long position, Tree tree) {
        this.position = position;
        this.tree = tree;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Location)) return false;
        Location l = (Location)obj;
        return this.position.equals(l.position) && this.tree == l.tree;
    }

    @Override
    public int hashCode() {
        return 47 * position.hashCode() + 113 * tree.hashCode();
    }

    @Override
    public String toString() {
        return "{" + this.position + ", " + this.tree + "}";
    }
}