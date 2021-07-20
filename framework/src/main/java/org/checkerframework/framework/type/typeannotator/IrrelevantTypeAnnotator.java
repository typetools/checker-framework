package org.checkerframework.framework.type.typeannotator;

import org.checkerframework.framework.qual.RelevantJavaTypes;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;

import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;

/**
 * Adds annotations to types that are not relevant specified by the {@link RelevantJavaTypes} on a
 * checker.
 */
public class IrrelevantTypeAnnotator extends TypeAnnotator {

    /** Annotations to add. */
    private Set<? extends AnnotationMirror> annotations;

    /**
     * Annotate every type with the annotationMirror except for those whose underlying Java type is
     * one of (or a subtype of) a class in relevantClasses. (Only adds annotationMirror if no
     * annotation in the hierarchy are already on the type.) If relevantClasses includes
     * Object[].class, then all arrays are considered relevant.
     *
     * @param typeFactory AnnotatedTypeFactory
     * @param annotations annotations to add
     */
    @SuppressWarnings("rawtypes")
    public IrrelevantTypeAnnotator(
            GenericAnnotatedTypeFactory typeFactory, Set<? extends AnnotationMirror> annotations) {
        super(typeFactory);
        this.annotations = annotations;
    }

    @Override
    protected Void scan(AnnotatedTypeMirror type, Void aVoid) {
        switch (type.getKind()) {
            case TYPEVAR:
            case WILDCARD:
            case EXECUTABLE:
            case INTERSECTION:
            case UNION:
            case NULL:
            case NONE:
            case PACKAGE:
            case VOID:
                return super.scan(type, aVoid);
            default:
                // go on
        }

        TypeMirror typeMirror = type.getUnderlyingType();

        if (!((GenericAnnotatedTypeFactory) typeFactory).isRelevant(typeMirror)) {
            type.addMissingAnnotations(annotations);
        }
        return super.scan(type, aVoid);
    }
}
