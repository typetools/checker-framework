package org.checkerframework.framework.type.typeannotator;

import com.sun.source.tree.Tree;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

//TODO: ADD SIMILAR JAVADOC AS ListTreeAnnotator
public class ListTypeAnnotator extends TypeAnnotator {

    protected final List<TypeAnnotator> annotators;

    /**
     * @param annotators the annotators that will be executed for
     *                   each tree scanned by this TreeAnnotator.
     *                   They are executed in the order passed in.
     */
    public ListTypeAnnotator(TypeAnnotator... annotators) {
        super(null);
        this.annotators = Collections.unmodifiableList(Arrays.asList(annotators));
    }

    @Override
    protected Void scan(AnnotatedTypeMirror type, Void aVoid) {
        for (TypeAnnotator annotator : annotators) {
            annotator.visit(type, aVoid);
        }

        return null;
    }
}
