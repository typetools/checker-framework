package org.checkerframework.framework.util.typeinference.constraint;

import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

import java.util.Set;

/**
 * F2AReducer takes an F2A constraint that is not irreducible (@see AFConstraint.isIrreducible) and
 * reduces it by one step. The resulting constraint may still be reducible.
 *
 * <p>Generally reductions should map to corresponding rules in
 * https://docs.oracle.com/javase/specs/jls/se11/html/jls-15.html#jls-15.12.2.7
 */
public class F2AReducer implements AFReducer {

    protected final F2AReducingVisitor visitor;

    public F2AReducer(final AnnotatedTypeFactory typeFactory) {
        this.visitor = new F2AReducingVisitor(typeFactory);
    }

    @Override
    public boolean reduce(AFConstraint constraint, Set<AFConstraint> newConstraints) {
        if (constraint instanceof F2A) {
            final F2A f2A = (F2A) constraint;
            visitor.visit(f2A.formalParameter, f2A.argument, newConstraints);
            return true;

        } else {
            return false;
        }
    }

    /**
     * Given an F2A constraint of the form: F2A( typeFromFormalParameter, typeFromMethodArgument )
     *
     * <p>F2AReducingVisitor visits the constraint as follows: visit ( typeFromFormalParameter,
     * typeFromMethodArgument, newConstraints )
     *
     * <p>The visit method will determine if the given constraint should either:
     *
     * <ul>
     *   <li>be discarded -- in this case, the visitor just returns
     *   <li>reduced to a simpler constraint or set of constraints -- in this case, the new
     *       constraint or set of constraints is added to newConstraints
     * </ul>
     *
     * Sprinkled throughout this class are comments of the form:
     *
     * <pre>{@code
     * // If F has the form G<..., Yk-1, ? super U, Yk+1, ...>, where U involves Tj
     * }</pre>
     *
     * These are excerpts from the JLS, if you search for them you will find the corresponding JLS
     * description of the case being covered.
     */
    private static class F2AReducingVisitor extends AFReducingVisitor {

        public F2AReducingVisitor(AnnotatedTypeFactory typeFactory) {
            super(F2A.class, typeFactory);
        }

        @Override
        public AFConstraint makeConstraint(
                AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype) {
            return new F2A(subtype, supertype);
        }

        @Override
        public AFConstraint makeInverseConstraint(
                AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype) {
            return new A2F(subtype, supertype);
        }

        @Override
        public AFConstraint makeEqualityConstraint(
                AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype) {
            return new FIsA(subtype, supertype);
        }
    }
}
