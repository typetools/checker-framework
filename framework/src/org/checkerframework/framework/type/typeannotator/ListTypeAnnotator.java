package org.checkerframework.framework.type.typeannotator;

import org.checkerframework.framework.type.AnnotatedTypeMirror;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * ListTypeAnnotator is a TypeAnnotator that executes a list of {@link TypeAnnotator}
 * for each type visited.
 *
 * Checkers should not extend ListTypeAnnotator; they should instead
 * pass a custom TypeAnnotator to the constructor.
 *
 * @see org.checkerframework.framework.type.typeannotator.ImplicitsTypeAnnotator
 * @see org.checkerframework.framework.type.typeannotator.PropagationTypeAnnotator
 */
public final class ListTypeAnnotator extends TypeAnnotator {

    protected final List<TypeAnnotator> annotators;

    /**
     * @param annotators the annotators that will be executed for
     *                   each type scanned by this TypeAnnotator.
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
