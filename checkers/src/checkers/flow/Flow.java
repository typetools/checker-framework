package checkers.flow;

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
     * @return The annotation inferred for a tree, or null if no annotation was
     *         inferred for that tree
     */
	AnnotationMirror test(Tree tree);
}