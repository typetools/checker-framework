package org.checkerframework.framework.util.typeinference.constraint;

import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNullType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedUnionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.visitor.AbstractAtmComboVisitor;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.plumelib.util.StringsPlume;

import java.util.List;
import java.util.Set;

import javax.lang.model.type.TypeKind;

/**
 * FIsAReducer takes an FIsA constraint that is not irreducible (@see AFConstraint.isIrreducible)
 * and reduces it by one step. The resulting constraint may still be reducible.
 *
 * <p>Generally reductions should map to corresponding rules in
 * https://docs.oracle.com/javase/specs/jls/se11/html/jls-15.html#jls-15.12.2.7
 */
public class FIsAReducer implements AFReducer {

    protected final FIsAReducingVisitor visitor;
    private final AnnotatedTypeFactory typeFactory;

    public FIsAReducer(final AnnotatedTypeFactory typeFactory) {
        this.typeFactory = typeFactory;
        this.visitor = new FIsAReducingVisitor();
    }

    @Override
    public boolean reduce(AFConstraint constraint, Set<AFConstraint> newConstraints) {
        if (constraint instanceof FIsA) {
            final FIsA fIsA = (FIsA) constraint;
            visitor.visit(fIsA.formalParameter, fIsA.argument, newConstraints);
            return true;

        } else {
            return false;
        }
    }

    /**
     * Given an FIsA constraint of the form: FIsA( typeFromFormalParameter, typeFromMethodArgument )
     *
     * <p>FIsAReducingVisitor visits the constraint as follows: visit ( typeFromFormalParameter,
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
     * From the JLS, in general there are 2 rules that govern F = A constraints: If F = Tj, then the
     * constraint Tj = A is implied. If F = U[], where the type U involves Tj, then if A is an array
     * type V[], or a type variable with an upper bound that is an array type V[], where V is a
     * reference type, this algorithm is applied recursively to the constraint {@code V >> U}.
     * Otherwise, no constraint is implied on Tj.
     *
     * <p>Since both F and A may have component types this visitor delves into their components and
     * applies these rules to the components. However, only one step is taken at a time (i.e. this
     * is not a scanner)
     */
    private class FIsAReducingVisitor extends AbstractAtmComboVisitor<Void, Set<AFConstraint>> {
        @Override
        protected String defaultErrorMessage(
                AnnotatedTypeMirror argument,
                AnnotatedTypeMirror parameter,
                Set<AFConstraint> afConstraints) {
            return StringsPlume.joinLines(
                    "Unexpected FIsA Combination:",
                    "argument=" + argument,
                    "parameter=" + parameter,
                    "constraints=[",
                    StringsPlume.join(", ", afConstraints),
                    "]");
        }
        // ------------------------------------------------------------------------
        // Arrays as arguments

        @Override
        public Void visitArray_Array(
                AnnotatedArrayType parameter,
                AnnotatedArrayType argument,
                Set<AFConstraint> constraints) {
            constraints.add(new FIsA(parameter.getComponentType(), argument.getComponentType()));
            return null;
        }

        @Override
        public Void visitArray_Declared(
                AnnotatedArrayType parameter,
                AnnotatedDeclaredType argument,
                Set<AFConstraint> constraints) {
            return null;
        }

        @Override
        public Void visitArray_Null(
                AnnotatedArrayType parameter,
                AnnotatedNullType argument,
                Set<AFConstraint> constraints) {
            return null;
        }

        @Override
        public Void visitArray_Wildcard(
                AnnotatedArrayType parameter,
                AnnotatedWildcardType argument,
                Set<AFConstraint> constraints) {
            constraints.add(new FIsA(parameter, argument.getExtendsBound()));
            return null;
        }

        // ------------------------------------------------------------------------
        // Declared as argument
        @Override
        public Void visitDeclared_Array(
                AnnotatedDeclaredType parameter,
                AnnotatedArrayType argument,
                Set<AFConstraint> constraints) {
            // should this be Array<String> - T[] the new A2F(String, T)
            return null;
        }

        @Override
        public Void visitDeclared_Declared(
                AnnotatedDeclaredType parameter,
                AnnotatedDeclaredType argument,
                Set<AFConstraint> constraints) {
            if (argument.isUnderlyingTypeRaw() || parameter.isUnderlyingTypeRaw()) {
                return null;
            }

            AnnotatedDeclaredType argumentAsParam =
                    AnnotatedTypes.castedAsSuper(typeFactory, argument, parameter);
            if (argumentAsParam == null) {
                return null;
            }

            final List<AnnotatedTypeMirror> argTypeArgs = argumentAsParam.getTypeArguments();
            final List<AnnotatedTypeMirror> paramTypeArgs = parameter.getTypeArguments();
            for (int i = 0; i < argTypeArgs.size(); i++) {
                final AnnotatedTypeMirror argTypeArg = argTypeArgs.get(i);
                final AnnotatedTypeMirror paramTypeArg = paramTypeArgs.get(i);

                if (paramTypeArg.getKind() == TypeKind.WILDCARD) {
                    final AnnotatedWildcardType paramWc = (AnnotatedWildcardType) paramTypeArg;

                    if (argTypeArg.getKind() == TypeKind.WILDCARD) {
                        final AnnotatedWildcardType argWc = (AnnotatedWildcardType) argTypeArg;
                        constraints.add(
                                new FIsA(paramWc.getExtendsBound(), argWc.getExtendsBound()));
                        constraints.add(new FIsA(paramWc.getSuperBound(), argWc.getSuperBound()));
                    }

                } else {
                    constraints.add(new FIsA(paramTypeArgs.get(i), argTypeArgs.get(i)));
                }
            }

            return null;
        }

        @Override
        public Void visitDeclared_Null(
                AnnotatedDeclaredType parameter,
                AnnotatedNullType argument,
                Set<AFConstraint> constraints) {
            return null;
        }

        @Override
        public Void visitDeclared_Primitive(
                AnnotatedDeclaredType parameter,
                AnnotatedPrimitiveType argument,
                Set<AFConstraint> constraints) {
            return null;
        }

        @Override
        public Void visitDeclared_Union(
                AnnotatedDeclaredType parameter,
                AnnotatedUnionType argument,
                Set<AFConstraint> constraints) {
            return null; // TODO: NOT SUPPORTED AT THE MOMENT
        }

        @Override
        public Void visitIntersection_Intersection(
                AnnotatedIntersectionType parameter,
                AnnotatedIntersectionType argument,
                Set<AFConstraint> constraints) {
            return null; // TODO: NOT SUPPORTED AT THE MOMENT
        }

        @Override
        public Void visitIntersection_Null(
                AnnotatedIntersectionType parameter,
                AnnotatedNullType argument,
                Set<AFConstraint> constraints) {
            return null;
        }

        @Override
        public Void visitNull_Null(
                AnnotatedNullType parameter,
                AnnotatedNullType argument,
                Set<AFConstraint> afConstraints) {
            // we sometimes get these when we have captured type variables passed as arguments
            // regardless they don't give any information
            return null;
        }

        // ------------------------------------------------------------------------
        // Primitive as argument
        @Override
        public Void visitPrimitive_Declared(
                AnnotatedPrimitiveType parameter,
                AnnotatedDeclaredType argument,
                Set<AFConstraint> constraints) {
            // we may be able to eliminate this case, since I believe the corresponding constraint
            // will just be discarded as the parameter must be a boxed primitive
            constraints.add(new FIsA(typeFactory.getBoxedType(parameter), argument));
            return null;
        }

        @Override
        public Void visitPrimitive_Primitive(
                AnnotatedPrimitiveType parameter,
                AnnotatedPrimitiveType argument,
                Set<AFConstraint> constraints) {
            return null;
        }

        @Override
        public Void visitTypevar_Typevar(
                AnnotatedTypeVariable parameter,
                AnnotatedTypeVariable argument,
                Set<AFConstraint> constraints) {
            // if we've reached this point and the two are corresponding type variables, then they
            // are NOT ones that may have a type variable we are inferring types for and therefore
            // we can discard this constraint
            return null;
        }

        @Override
        public Void visitTypevar_Null(
                AnnotatedTypeVariable argument,
                AnnotatedNullType parameter,
                Set<AFConstraint> constraints) {
            return null;
        }
    }
}
