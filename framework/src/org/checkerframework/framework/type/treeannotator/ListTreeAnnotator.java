package org.checkerframework.framework.type.treeannotator;

import com.sun.source.tree.Tree;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

/**
 * ListTreeAnnotator is a TreeVisitor that executes a list of
 * {@link TreeAnnotator}
 * for each tree visited.
 *
 * Checkers should not extend ListTreeAnnotator; they should instead
 * pass a custom TreeAnnotator to the constructor.
 *
 * @see ImplicitsTreeAnnotator
 * @see PropagationTreeAnnotator
 */
public class ListTreeAnnotator extends TreeAnnotator {

    protected final List<TreeAnnotator> annotators;

    /**
     * @param annotators the annotators that will be executed for
     *                   each tree scanned by this TreeAnnotator.
     *                   They are executed in the order passed in.
     */
    public ListTreeAnnotator(TreeAnnotator... annotators) {
        super(null);
        this.annotators = Collections.unmodifiableList(Arrays.asList(annotators));
    }

    @Override
    public Void defaultAction(Tree node, AnnotatedTypeMirror type) {
        for (TreeAnnotator annotator : annotators) {
            annotator.visit(node, type);
        }

        return null;
    }
}
