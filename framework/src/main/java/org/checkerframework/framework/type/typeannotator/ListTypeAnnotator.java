package org.checkerframework.framework.type.typeannotator;

import org.checkerframework.framework.type.AnnotatedTypeMirror;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * ListTypeAnnotator is a TypeAnnotator that executes a list of {@link TypeAnnotator} for each type
 * visited.
 *
 * <p>Checkers should not extend ListTypeAnnotator; they should instead pass a custom TypeAnnotator
 * to the constructor.
 *
 * @see DefaultForTypeAnnotator
 * @see org.checkerframework.framework.type.typeannotator.PropagationTypeAnnotator
 */
public final class ListTypeAnnotator extends TypeAnnotator {

    /**
     * The annotators that will be executed for each type scanned by this TypeAnnotator. They are
     * executed in order.
     */
    final List<TypeAnnotator> annotators;

    /**
     * Create a new ListTypeAnnotator.
     *
     * @param annotators the annotators that will be executed for each type scanned by this
     *     TypeAnnotator. They are executed in the order passed in.
     */
    public ListTypeAnnotator(TypeAnnotator... annotators) {
        this(Arrays.asList(annotators));
    }

    /**
     * @param annotators the annotators that will be executed for each type scanned by this
     *     TypeAnnotator. They are executed in the order passed in.
     */
    public ListTypeAnnotator(List<TypeAnnotator> annotators) {
        super(null);
        List<TypeAnnotator> annotatorList = new ArrayList<>(annotators.size());
        for (TypeAnnotator annotator : annotators) {
            if (annotator instanceof ListTypeAnnotator) {
                annotatorList.addAll(((ListTypeAnnotator) annotator).annotators);
            } else {
                annotatorList.add(annotator);
            }
        }
        this.annotators = Collections.unmodifiableList(annotatorList);
    }

    @Override
    protected Void scan(AnnotatedTypeMirror type, Void aVoid) {
        for (TypeAnnotator annotator : annotators) {
            annotator.visit(type, aVoid);
        }

        return null;
    }

    @Override
    public String toString() {
        return "ListTypeAnnotator" + annotators;
    }
}
