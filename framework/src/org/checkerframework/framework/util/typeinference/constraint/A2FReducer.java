package org.checkerframework.framework.util.typeinference.constraint;

import com.sun.tools.javac.code.Type;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.*;
import org.checkerframework.framework.type.DefaultTypeHierarchy;
import org.checkerframework.framework.type.visitor.AbstractAtmComboVisitor;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.PluginUtil;
import org.checkerframework.framework.util.typeinference.TypeArgInferenceUtil;

import javax.lang.model.type.TypeKind;
import java.util.List;
import java.util.Set;

/**
 * A2FReducer takes an A2F constraint that is not irreducible (@see AFConstraint.isIrreducible)
 * and reduces it by one step.  The resulting constraint may still be reducible.
 *
 * Generally reductions should map to corresponding rules in
 * http://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.12.2.7
 */
public class A2FReducer implements AFReducer {

    protected final A2FReducingVisitor visitor;
    private final AnnotatedTypeFactory typeFactory;

    public A2FReducer(final AnnotatedTypeFactory typeFactory) {
        this.typeFactory = typeFactory;
        this.visitor = new A2FReducingVisitor();
    }

    @Override
    public boolean reduce(AFConstraint constraint, Set<AFConstraint> newConstraints, Set<AFConstraint> finished) {
        if (constraint instanceof A2F) {
            final A2F a2f = (A2F) constraint;
            visitor.visit(a2f.argument, a2f.formalParameter, newConstraints);
            return true;

        } else {
            return false;
        }
    }

    /**
     * Given an A2F constraint of the form:
     * A2F( typeFromMethodArgument, typeFromFormalParameter )
     *
     * A2FReducingVisitor visits the constraint as follows:
     * visit ( typeFromMethodArgument, typeFromFormalParameter, newConstraints )
     *
     * The visit method will determine if the given constraint should either:
     *    a) be discarded - in this case, the visitor just returns
     *    b) reduced to a simpler constraint or set of constraints - in this case, the new constraint
     *    or set of constraints is added to newConstraints
     *
     *  Sprinkled throughout this class are comments of the form:
     *
     *  //If F has the form G<..., Yk-1, ? super U, Yk+1, ...>, where U involves Tj
     *
     *  These are excerpts from the JLS, if you search for them you will find the corresponding
     *  JLS description of the case being covered.
     */
    class A2FReducingVisitor extends AbstractAtmComboVisitor<Void, Set<AFConstraint>> {

        /**
         * Called when we encounter an A2F constraint on a type combination that we did not think is possible.
         * This either implies that the type combination is possible, we accidentally created an invalid
         * A2F Constraint, or we called the visit method on two AnnotatedTypeMirrors that do not appear together
         * in an A2F constraint.
         */
        @Override
        protected String defaultErrorMessage(AnnotatedTypeMirror argument, AnnotatedTypeMirror parameter, Set<AFConstraint> constraints) {
            return "Unexpected A2F Combination:\b"
                    + "argument=" + argument + "\n"
                    + "parameter=" + parameter + "\n"
                    + "constraints=[\n" + PluginUtil.join(", ", constraints) + "\n]";
        }

        //------------------------------------------------------------------------
        //Arrays as arguments
        //From the JLS
        //    If F = U[], where the type U involves Tj, then if A is an array type V[], or a type variable with an
        //    upper bound that is an array type V[], where V is a reference type, this algorithm is applied recursively
        //    to the constraint V << U.

        @Override
        public Void visitArray_Array(AnnotatedArrayType argument, AnnotatedArrayType parameter, Set<AFConstraint> constraints) {
            //TODO: This method currently does not handle the -AinvariantArrays option
            constraints.add(new A2F(argument.getComponentType(), parameter.getComponentType()));
            return null;
        }

        @Override
        public Void visitArray_Declared(AnnotatedArrayType argument, AnnotatedDeclaredType parameter, Set<AFConstraint> constraints) {
            return null;
        }

        @Override
        public Void visitArray_Null(AnnotatedArrayType argument, AnnotatedNullType parameter, Set<AFConstraint> constraints) {
            return null;
        }

        @Override
        public Void visitArray_Wildcard(AnnotatedArrayType argument, AnnotatedWildcardType parameter, Set<AFConstraint> constraints) {
            constraints.add(new A2F(argument, parameter.getExtendsBound()));
            return null;
        }

        //despite the above the comment at the beginning of the "array as arguments" section, a type variable cannot
        //actually have an array type as its upper bound (e.g. <T extends Integer[]> is not allowed).
        //so the only cases in which we visitArray_Typevar would be cases in which either:
        //   1) Typevar is a type parameter for which we are inferring an argument, in which case the combination is
        //   already irreducible and we would not pass it to this class
        //   2) Typevar is an outer scope type variable, in which case it could NOT reference any of the type parameters
        //   for which we are inferring arguments and therefore will not lead to any meaningful AFConstraints
        //public void visitArray_Typevar

        //------------------------------------------------------------------------
        //Declared as argument

        /**
         * I believe there should be only 1 way to have an A2F of this form:
         * visit (Array<T>, T [])
         * At this point, I don't think that's a valid argument for a formal parameter.  If this occurs
         * is is because of idiosyncrasies with the Checker Framework .  We're going to skip this case for now.
         */
        @Override
        public Void visitDeclared_Array(AnnotatedDeclaredType argument, AnnotatedArrayType parameter, Set<AFConstraint> constraints) {
            return null;
        }

        //From the JLS Spec:
        //  If F has the form G<..., Yk-1, ? extends U, Yk+1, ...>, where U involves Tj
        @Override
        public Void visitDeclared_Declared(AnnotatedDeclaredType argument, AnnotatedDeclaredType parameter, Set<AFConstraint> constraints) {
            if (argument.wasRaw() || parameter.wasRaw()) {
                return null;
            }

            AnnotatedDeclaredType argumentAsParam = DefaultTypeHierarchy.castedAsSuper(argument, parameter);
            if (argumentAsParam == null) {
                return null;
            }

            final List<AnnotatedTypeMirror> argTypeArgs = argumentAsParam.getTypeArguments();
            final List<AnnotatedTypeMirror> paramTypeArgs = parameter.getTypeArguments();
            for (int i = 0; i < argTypeArgs.size(); i++) {
                final AnnotatedTypeMirror argTypeArg = argTypeArgs.get(i);
                final AnnotatedTypeMirror paramTypeArg = paramTypeArgs.get(i);

                //If F has the form G<..., Yk-1, ? extends U, Yk+1, ...>, where U involves Tj
                //If F has the form G<..., Yk-1, ? super U, Yk+1, ...>, where U involves Tj
                //Since we always have both bounds in the checker framework we always compare both
                if (paramTypeArg.getKind() == TypeKind.WILDCARD) {
                    final AnnotatedWildcardType paramWc = (AnnotatedWildcardType) paramTypeArg;

                    if (argTypeArg.getKind() == TypeKind.WILDCARD) {
                        final AnnotatedWildcardType argWc = (AnnotatedWildcardType) argTypeArg;
                        constraints.add(new A2F(argWc.getExtendsBound(), paramWc.getExtendsBound()));
                        constraints.add(new F2A(paramWc.getSuperBound(), argWc.getSuperBound()));
                    } else {
                        constraints.add(new A2F(argTypeArg, paramWc.getExtendsBound()));
                        constraints.add(new F2A(paramWc.getSuperBound(), argTypeArg));
                    }

                } else {
                    //if F has the form G<..., Yk-1, U, Yk+1, ...>, where U is a type expression that involves Tj
                    constraints.add(new FIsA(paramTypeArgs.get(i), argTypeArgs.get(i)));

                }
            }

            return null;
        }

        @Override
        public Void visitDeclared_Intersection(AnnotatedDeclaredType argument, AnnotatedIntersectionType parameter, Set<AFConstraint> constraints) {

            //Note: AnnotatedIntersectionTypes cannot have a type variable as one of the direct parameters but
            // a type variable may be the type argument to an intersection bound <e.g.   <T extends Serializable & Iterable<T>>
            for (final AnnotatedTypeMirror intersectionBound : parameter.directSuperTypes()) {
                if (intersectionBound instanceof AnnotatedDeclaredType
                && ((AnnotatedDeclaredType) intersectionBound).getTypeArguments().size() > 0) {
                    constraints.add(new A2F(argument, parameter));
                }
            }

            return null;
        }

        //Remember that NULL types can come from lower bounds
        @Override
        public Void visitDeclared_Null(AnnotatedDeclaredType argument, AnnotatedNullType parameter, Set<AFConstraint> constraints) {
            return null;
        }

        //a primitive parameter provides us no information on the type of any type parameters for that method
        @Override
        public Void visitDeclared_Primitive(AnnotatedDeclaredType argument, AnnotatedPrimitiveType parameter, Set<AFConstraint> constraints) {
            return null;
        }

        @Override
        public Void visitDeclared_Typevar(AnnotatedDeclaredType argument, AnnotatedTypeVariable parameter, Set<AFConstraint> constraints) {
            //Note: We expect the A2F constraints where F == a targeted type parameter to already be removed
            //Note: Therefore, parameter should NOT be a target
            constraints.add(new A2F(argument, parameter.getLowerBound()));
            return null;
        }

        @Override
        public Void visitDeclared_Union(AnnotatedDeclaredType argument, AnnotatedUnionType parameter, Set<AFConstraint> constraints) {
            return null;  //TODO: NOT SUPPORTED AT THE MOMENT
        }

        @Override
        public Void visitDeclared_Wildcard(AnnotatedDeclaredType argument, AnnotatedWildcardType parameter, Set<AFConstraint> constraints) {
            constraints.add(new A2F(argument, parameter.getSuperBound()));
            return null;
        }

        //------------------------------------------------------------------------
        //Intersection as argument
        @Override
        public Void visitIntersection_Declared(AnnotatedIntersectionType argument, AnnotatedDeclaredType parameter, Set<AFConstraint> constraints) {

            //at least one of the intersection bound types must be convertible to the param type
            final AnnotatedDeclaredType argumentAsParam = DefaultTypeHierarchy.castedAsSuper(argument, parameter);
            if (argumentAsParam != null) {
                constraints.add(new A2F(argument, parameter));
            }

            return null;
        }

        @Override
        public Void visitIntersection_Intersection(AnnotatedIntersectionType argument, AnnotatedIntersectionType parameter, Set<AFConstraint> constraints) {
            return null;  //TODO: NOT SUPPORTED AT THE MOMENT
        }

        //provides no information as the AnnotatedNullType cannot refer to a type parameter
        @Override
        public Void visitIntersection_Null(AnnotatedIntersectionType argument, AnnotatedNullType parameter, Set<AFConstraint> constraints) {
            return null;
        }

        //------------------------------------------------------------------------
        //Null as argument

        /**
         * NULL types only have primary annotations.  A type parameter could only appear as a component of the
         * parameter type and therefore has no relationship to these primary annotations
         */
        @Override
        public Void visitNull_Array(AnnotatedNullType argument, AnnotatedArrayType parameter, Set<AFConstraint> constraints) {
            return null;
        }

        /**
         * NULL types only have primary annotations.  A type parameter could only appear as a component of the
         * parameter type and therefore has no relationship to these primary annotations
         */
        @Override
        public Void visitNull_Declared(AnnotatedNullType argument, AnnotatedDeclaredType parameter, Set<AFConstraint> constraints) {
            return null;
        }

        /**
         * TODO: PERHAPS FOR ALL OF THESE WHERE WE COMPARE AGAINST THE LOWER BOUND, WE SHOULD INSTEAD COMPARE
         * TODO: against the UPPER_BOUND with the LOWER_BOUND's PRIMARY ANNOTATIONS
         * For captured types, the lower bound might be interesting so we compare against the lower bound but for
         * most types the constraint added in this method is probably discarded in the next round of
         * reduction (especially since we don't implement capture at the moment).
         */
        @Override
        public Void visitNull_Typevar(AnnotatedNullType argument, AnnotatedTypeVariable parameter, Set<AFConstraint> constraints) {
            //Note: We would expect that parameter is not one of the targets or else it would already be removed
            //NOTE: Therefore we compare NULL against it's bound
            constraints.add(new A2F(argument, parameter.getLowerBound()));
            return null;
        }

        @Override
        public Void visitNull_Wildcard(AnnotatedNullType argument, AnnotatedWildcardType parameter, Set<AFConstraint> constraints) {
            constraints.add(new A2F(argument, parameter.getSuperBound()));
            return null;
        }

        @Override
        public Void visitNull_Null(AnnotatedNullType argument, AnnotatedNullType parameter, Set<AFConstraint> constraints) {
            return null;
        }

        @Override
        public Void visitNull_Union(AnnotatedNullType argument, AnnotatedUnionType parameter, Set<AFConstraint> constraints) {
            return null; //TODO: UNIONS ARE NOT YET SUPPORTED
        }

        //Despite the fact that intersections are not yet supported, this is the right impelementation.  NULL types
        //only have primary annotations.  Since type parameters cannot be a member of the intersection's bounds
        //(though they can be component types), we do not need to do anything further
        @Override
        public Void visitNull_Intersection(AnnotatedNullType argument, AnnotatedIntersectionType parameter, Set<AFConstraint> constraints) {
            return null;
        }

        //Primitive parameter types tell us nothing about the type parameters
        @Override
        public Void visitNull_Primitive(AnnotatedNullType argument, AnnotatedPrimitiveType parameter, Set<AFConstraint> constraints) {
            return null;
        }

        //------------------------------------------------------------------------
        //Primitive as argument

        @Override
        public Void visitPrimitive_Declared(AnnotatedPrimitiveType argument, AnnotatedDeclaredType parameter, Set<AFConstraint> constraints) {
            //we may be able to eliminate this case, since I believe the corresponding constraint will just be discarded
            //as the parameter must be a boxed primitive
            constraints.add(new A2F(typeFactory.getBoxedType(argument), parameter));
            return null;
        }

        //Primitive parameter types tell us nothing about the type parameters
        @Override
        public Void visitPrimitive_Primitive(AnnotatedPrimitiveType argument, AnnotatedPrimitiveType parameter, Set<AFConstraint> constraints) {
            return null;
        }

        @Override
        public Void visitPrimitive_Intersection(AnnotatedPrimitiveType argument, AnnotatedIntersectionType parameter, Set<AFConstraint> constraints) {
            constraints.add(new A2F(typeFactory.getBoxedType(argument), parameter));
            return null;
        }

        //------------------------------------------------------------------------
        //Union as argument
        @Override
        public Void visitUnion_Declared(AnnotatedUnionType argument, AnnotatedDeclaredType parameter, Set<AFConstraint> constraints) {
            return null; //TODO: UNIONS ARE NOT CURRENTLY SUPPORTED
        }

        //------------------------------------------------------------------------
        //typevar as argument
        //If we've reached this point, the typevar is NOT one of the types we are inferring.

        @Override
        public Void visitTypevar_Declared(AnnotatedTypeVariable argument, AnnotatedDeclaredType parameter, Set<AFConstraint> constraints) {
            constraints.add(new A2F(argument.getUpperBound(), parameter));
            return null;
        }

        @Override
        public Void visitTypevar_Typevar(AnnotatedTypeVariable argument, AnnotatedTypeVariable parameter, Set<AFConstraint> constraints) {
            //if we've reached this point and the two are corresponding type variables, then they are NOT ones that
            //may have a type variable we are inferring types for and therefore we can discard this constraint
            if (!AnnotatedTypes.areCorrespondingTypeVariables(typeFactory.getElementUtils(), argument, parameter)) {
                constraints.add(new A2F(argument.getUpperBound(), parameter.getLowerBound()));
            }

            return null;
        }

        @Override
        public Void visitTypevar_Null(AnnotatedTypeVariable argument, AnnotatedNullType parameter, Set<AFConstraint> constraints) {
            constraints.add(new A2F(argument.getUpperBound(), parameter));
            return null;
        }

        @Override
        public Void visitTypevar_Wildcard(AnnotatedTypeVariable argument, AnnotatedWildcardType parameter, Set<AFConstraint> constraints) {
            constraints.add(new A2F(argument, parameter.getSuperBound()));
            return null;
        }

        //------------------------------------------------------------------------
        //wildcard as argument
        @Override
        public Void visitWildcard_Array(AnnotatedWildcardType argument, AnnotatedArrayType parameter, Set<AFConstraint> constraints) {
            constraints.add(new A2F(argument.getExtendsBound(), parameter));
            return null;
        }

        @Override
        public Void visitWildcard_Declared(AnnotatedWildcardType argument, AnnotatedDeclaredType parameter, Set<AFConstraint> constraints) {
            constraints.add(new A2F(argument.getExtendsBound(), parameter));
            return null;
        }

        @Override
        public Void visitWildcard_Intersection(AnnotatedWildcardType argument, AnnotatedIntersectionType parameter, Set<AFConstraint> constraints) {
            constraints.add(new A2F(argument.getExtendsBound(), parameter));
            return null;
        }

        @Override
        public Void visitWildcard_Primitive(AnnotatedWildcardType argument, AnnotatedPrimitiveType parameter, Set<AFConstraint> constraints) {
            return null;
        }

        @Override
        public Void visitWildcard_Typevar(AnnotatedWildcardType argument, AnnotatedTypeVariable parameter, Set<AFConstraint> constraints) {
            constraints.add(new A2F(argument.getExtendsBound(), parameter));
            return null;
        }

        @Override
        public Void visitWildcard_Wildcard(AnnotatedWildcardType argument, AnnotatedWildcardType parameter, Set<AFConstraint> constraints) {
            //since wildcards are handled in visitDeclared_Declared this could only occur if two wildcards
            //were passed to type argument inference at the top level.  This can only occur because we do not implement
            //capture conversion
            constraints.add(new A2F(argument.getExtendsBound(), parameter.getExtendsBound()));
            constraints.add(new F2A(parameter.getSuperBound(),  argument.getSuperBound()));
            return null;
        }
    }
}
