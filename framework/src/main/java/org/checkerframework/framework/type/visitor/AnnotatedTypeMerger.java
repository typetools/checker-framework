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
 * visitType.accept(new AnnotatedTypesMerger(), parameter);
 * }</pre>
 */
public class AnnotatedTypeMerger extends AnnotatedTypeComparer<Void> {

    /** Replaces or adds all annotations from {@code from} to {@code to}. */
    public static void merge(final AnnotatedTypeMirror from, final AnnotatedTypeMirror to) {
        if (from == to) {
            throw new BugInCF("From == to");
        }
        new AnnotatedTypeMerger().visit(from, to);
    }

    public static void merge(
            final AnnotatedTypeMirror from,
            final AnnotatedTypeMirror to,
            final AnnotationMirror top) {
        if (from == to) {
            throw new BugInCF("From == to");
        }
        new AnnotatedTypeMerger(top).visit(from, to);
    }

    // If top != null we replace only the annotations in the hierarchy of top.
    private final AnnotationMirror top;

    public AnnotatedTypeMerger() {
        this.top = null;
    }

    /**
     * @param top if top != null, then only annotation in the hierarchy of top are affected by this
     *     merger
     */
    public AnnotatedTypeMerger(final AnnotationMirror top) {
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

    protected void replaceAnnotations(
            final AnnotatedTypeMirror one, final AnnotatedTypeMirror two) {
        if (top == null) {
            two.replaceAnnotations(one.getAnnotations());
        } else {
            final AnnotationMirror replacement = one.getAnnotationInHierarchy(top);
            if (replacement != null) {
                two.replaceAnnotation(one.getAnnotationInHierarchy(top));
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
                    "ResolvePrimaries' from argument should be a type variable OR wildcard\n"
                            + "from="
                            + from.toString(true)
                            + "\n"
                            + "to="
                            + to.toString(true));
        }
    }
}
