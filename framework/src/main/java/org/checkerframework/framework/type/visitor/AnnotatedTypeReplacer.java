package org.checkerframework.framework.type.visitor;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
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
public class AnnotatedTypeReplacer extends AnnotatedTypeComparer<Void> {

    /**
     * Replaces or adds all annotations from {@code from} to {@code to}.
     *
     * @param from the annotated type mirror from which to take new annotations
     * @param to the annotated type mirror to which the annotations will be added
     */
    public static void replace(final AnnotatedTypeMirror from, final AnnotatedTypeMirror to) {
        if (from == to) {
            throw new BugInCF("From == to");
        }
        new AnnotatedTypeReplacer().visit(from, to);
    }

    /**
     * Replaces or adds annotations in {@code top}'s hierarchy from {@code from} to {@code to}.
     *
     * @param from the annotated type mirror from which to take new annotations
     * @param to the annotated type mirror to which the annotations will be added
     * @param top the top type of the hierarchy whose annotations will be added
     */
    public static void replace(
            final AnnotatedTypeMirror from,
            final AnnotatedTypeMirror to,
            final AnnotationMirror top) {
        if (from == to) {
            throw new BugInCF("From == to");
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

    @Override
    protected Void compare(AnnotatedTypeMirror one, AnnotatedTypeMirror two) {
        assert one != two;
        if (one != null && two != null) {
            replaceAnnotations(one, two);
        }
        return null;
    }

    @Override
    protected Void combineRs(Void r1, Void r2) {
        return r1;
    }

    /**
     * Replace the annotations in dst with the annotations in src
     *
     * @param src the source of the annotations
     * @param dst the destination of the annotations
     */
    protected void replaceAnnotations(
            final AnnotatedTypeMirror src, final AnnotatedTypeMirror dst) {
        if (top == null) {
            dst.replaceAnnotations(src.getAnnotations());
        } else {
            final AnnotationMirror replacement = src.getAnnotationInHierarchy(top);
            if (replacement != null) {
                dst.replaceAnnotation(src.getAnnotationInHierarchy(top));
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
