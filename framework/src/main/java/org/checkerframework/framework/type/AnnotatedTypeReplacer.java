package org.checkerframework.framework.type;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.visitor.DoubleAnnotatedTypeScanner;
import org.checkerframework.javacutil.BugInCF;

/**
 * Replaces or adds all the annotations in the parameter with the annotations from the visited type.
 * An annotation is replaced if the parameter type already has an annotation in the same hierarchy
 * at the same location as the visited type.
 *
 * <p>Example use:
 *
 * <pre>{@code
 * AnnotatedTypeMirror visitType = ...;
 * AnnotatedTypeMirror parameter = ...;
 * visitType.accept(new AnnotatedTypeReplacer(), parameter);
 * }</pre>
 */
public class AnnotatedTypeReplacer extends DoubleAnnotatedTypeScanner<Void> {

    /**
     * Replaces or adds all annotations from {@code from} to {@code to}. Annotations from {@code
     * from} will be used everywhere they exist, but annotations in {@code to} will be kept anywhere
     * that {@code from} is unannotated.
     *
     * @param from the annotated type mirror from which to take new annotations
     * @param to the annotated type mirror to which the annotations will be added
     */
    @SuppressWarnings("interning:not.interned") // assertion
    public static void replace(final AnnotatedTypeMirror from, final AnnotatedTypeMirror to) {
        if (from == to) {
            throw new BugInCF("From == to");
        }
        new AnnotatedTypeReplacer().visit(from, to);
    }

    /**
     * Replaces or adds annotations in {@code top}'s hierarchy from {@code from} to {@code to}.
     * Annotations from {@code from} will be used everywhere they exist, but annotations in {@code
     * to} will be kept anywhere that {@code from} is unannotated.
     *
     * @param from the annotated type mirror from which to take new annotations
     * @param to the annotated type mirror to which the annotations will be added
     * @param top the top type of the hierarchy whose annotations will be added
     */
    @SuppressWarnings("interning:not.interned") // assertion
    public static void replace(
            final AnnotatedTypeMirror from,
            final AnnotatedTypeMirror to,
            final AnnotationMirror top) {
        if (from == to) {
            throw new BugInCF("from == to: %s", from);
        }
        new AnnotatedTypeReplacer(top).visit(from, to);
    }

    /** If top != null we replace only the annotations in the hierarchy of top. */
    private final AnnotationMirror top;

    /** Construct an AnnotatedTypeReplacer that will replace all annotations. */
    public AnnotatedTypeReplacer() {
        this.top = null;
    }

    /**
     * Construct an AnnotatedTypeReplacer that will only replace annotations in {@code top}'s
     * hierarchy.
     *
     * @param top if top != null, then only annotation in the hierarchy of top are affected
     */
    public AnnotatedTypeReplacer(final AnnotationMirror top) {
        this.top = top;
    }

    @SuppressWarnings("interning:not.interned") // assertion
    @Override
    protected Void defaultAction(AnnotatedTypeMirror from, AnnotatedTypeMirror to) {
        assert from != to;
        if (from != null && to != null) {
            replaceAnnotations(from, to);
        }
        return null;
    }

    /**
     * Replace the annotations in to with the annotations in from, wherever from has an annotation.
     *
     * @param from the source of the annotations
     * @param to the destination of the annotations, modified by this method
     */
    protected void replaceAnnotations(
            final AnnotatedTypeMirror from, final AnnotatedTypeMirror to) {
        if (top == null) {
            to.replaceAnnotations(from.getAnnotations());
        } else {
            final AnnotationMirror replacement = from.getAnnotationInHierarchy(top);
            if (replacement != null) {
                to.replaceAnnotation(from.getAnnotationInHierarchy(top));
            }
        }
    }

    @Override
    public Void visitTypeVariable(AnnotatedTypeVariable from, AnnotatedTypeMirror to) {
        resolvePrimaries(from, to);
        return super.visitTypeVariable(from, to);
    }

    @Override
    public Void visitWildcard(AnnotatedWildcardType from, AnnotatedTypeMirror to) {
        resolvePrimaries(from, to);
        return super.visitWildcard(from, to);
    }

    /**
     * For type variables and wildcards, the absence of a primary annotations has an implied meaning
     * on substitution. Therefore, in these cases we remove the primary annotation and rely on the
     * fact that the bounds are also merged into the type to.
     *
     * @param from a type variable or wildcard
     * @param to the destination annotated type mirror
     */
    public void resolvePrimaries(AnnotatedTypeMirror from, AnnotatedTypeMirror to) {
        if (from.getKind() == TypeKind.WILDCARD || from.getKind() == TypeKind.TYPEVAR) {
            if (top != null) {
                if (from.getAnnotationInHierarchy(top) == null) {
                    to.removeAnnotationInHierarchy(top);
                }
            } else {
                for (final AnnotationMirror toPrimaryAnno : to.getAnnotations()) {
                    if (from.getAnnotationInHierarchy(toPrimaryAnno) == null) {
                        to.removeAnnotation(toPrimaryAnno);
                    }
                }
            }
        } else {
            throw new BugInCF(
                    "ResolvePrimaries's from argument should be a type variable OR wildcard%n"
                            + "from=%s%nto=%s",
                    from.toString(true), to.toString(true));
        }
    }
}
