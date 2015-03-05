package org.checkerframework.framework.type;

import com.sun.source.tree.Tree;
import com.sun.source.util.SimpleTreeVisitor;

import java.util.List;

/**
 * ListTreeAnnotator is a TreeVisitor that executes a list of
 * {@link org.checkerframework.framework.type.TreeAnnotator}
 * for each tree visited.
 *
 * Checkers should not override ListTreeAnnotator; they should instead
 * pass a custom TreeAnnotator the constructor.
 *
 * @see org.checkerframework.framework.type.ImplicitsTreeAnnotator
 * @see org.checkerframework.framework.type.PropagationTreeAnnotator
 */
public final class ListTreeAnnotator extends TreeAnnotator {

    private final TreeAnnotator[] annotators;

    /**
     * @param annotators the annotators that will be executed for
     *                   each tree scanned by this TreeAnnotator.
     *                   They are executed in the order passed in.
     */
    public ListTreeAnnotator(TreeAnnotator... annotators) {
        super(null);
        this.annotators = annotators;
    }

    @Override
    public Void defaultAction(Tree node, AnnotatedTypeMirror type) {
        for (TreeAnnotator annotator : annotators) {
            annotator.visit(node, type);
        }

        return null;
    }
}
