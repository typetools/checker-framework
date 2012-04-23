package checkers.flow;

import java.io.PrintStream;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;

import com.sun.source.tree.Tree;

/**
 * Interface for a generalized flow-sensitive qualifier inference for the checkers
 * framework.
 */
public interface Flow {

    /**
     * Scan the given (root) tree and infer the refined types.
     *
     * @param tree The tree to scan
     */
    void scan(Tree tree);

    /**
     * Determines the inference result for a tree.
     *
     * @param tree The tree to test
     * @return The set of annotations inferred for a tree, or null if no annotation was
     *         inferred for that tree
     */
    Set<AnnotationMirror> test(Tree tree);

    /**
     * Sets the {@link PrintStream} for printing debug messages, such as
     * {@link System#out} or {@link System#err}, or null if no debugging output
     * should be emitted (the default).
     */
    void setDebug(PrintStream debug);
}
