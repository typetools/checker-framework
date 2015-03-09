package org.checkerframework.framework.util.typeinference.constraint;

import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.*;
import org.checkerframework.framework.type.DefaultTypeHierarchy;
import org.checkerframework.framework.type.visitor.AbstractAtmComboVisitor;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.PluginUtil;
import org.checkerframework.javacutil.ErrorReporter;

import javax.lang.model.type.TypeKind;
import java.util.List;
import java.util.Set;
import static org.checkerframework.framework.util.typeinference.TypeArgInferenceUtil.*;

/**
 * F2AReducer takes an F2A constraint that is not irreducible (@see AFConstraint.isIrreducible)
 * and reduces it by one step.  The resulting constraint may still be reducible.
 *
 * Generally reductions should map to corresponding rules in
 * http://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.12.2.7
 */
public class F2AReducer implements AFReducer {

    protected final F2AReducingVisitor visitor;
    private final AnnotatedTypeFactory typeFactory;

    public F2AReducer(final AnnotatedTypeFactory typeFactory) {
        this.typeFactory = typeFactory;
        this.visitor = new F2AReducingVisitor();
    }

    @Override
    public boolean reduce(AFConstraint constraint, Set<AFConstraint> newConstraints, Set<AFConstraint> finished) {
        if (constraint instanceof F2A) {
            final F2A f2A = (F2A) constraint;
            visitor.visit(f2A.formalParameter, f2A.argument, newConstraints);
            return true;

        } else {
            return false;
        }
    }

    /**
     * Given an F2A constraint of the form:
     * F2A( typeFromFormalParameter, typeFromMethodArgument )
     *
     * F2AReducingVisitor visits the constraint as follows:
     * visit ( typeFromFormalParameter, typeFromMethodArgument, newConstraints )
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
    class F2AReducingVisitor extends AbstractAtmComboVisitor<Void, Set<AFConstraint>> {

        @Override
        protected String defaultErrorMessage(AnnotatedTypeMirror argument, AnnotatedTypeMirror parameter, Set<AFConstraint> afConstraints) {
            return "Unexpected F2A Combination:\b"
                 + "argument=" + argument + "\n"
                 + "parameter=" + parameter + "\n"
                 + "constraints=[\n" + PluginUtil.join(", ", afConstraints) + "\n]";
        }

        //------------------------------------------------------------------------
        //Arrays as arguments
        //From the JLS
        //If F = U[], where the type U involves Tj, then if A is an array type V[], or a type variable with an
        //upper bound that is an array type V[], where V is a reference type, this algorithm is applied
        // recursively to the constraint V >> U. Otherwise, no constraint is implied on Tj.

        @Override
        public Void visitArray_Array(AnnotatedArrayType parameter, AnnotatedArrayType argument, Set<AFConstraint> constraints) {
            //TODO: This method currently does not handle the -AinvariantArrays option
            constraints.add(new F2A(parameter.getComponentType(), argument.getComponentType()));
            return null;
        }

        @Override
        public Void visitArray_Declared(AnnotatedArrayType parameter, AnnotatedDeclaredType argument, Set<AFConstraint> constraints) {
            return null;
        }

        @Override
        public Void visitArray_Null(AnnotatedArrayType parameter, AnnotatedNullType argument, Set<AFConstraint> constraints) {
            return null;
        }

        @Override
        public Void visitArray_Wildcard(AnnotatedArrayType parameter, AnnotatedWildcardType argument, Set<AFConstraint> constraints) {
            constraints.add(new F2A(parameter, argument.getExtendsBound()));
            return null;
        }

        //------------------------------------------------------------------------
        //Declared as argument
        /**
         * I believe there should be only 1 way to have an A2F of this form:
         * visit (Array<T>, T [])
         * At this point, I don't think that's a valid argument for a formal parameter.  If this occurs
         * is is because of idiosyncrasies with the Checker Framework .  We're going to skip this case for now.
         */
        @Override
        public Void visitDeclared_Array(AnnotatedDeclaredType parameter, AnnotatedArrayType argument, Set<AFConstraint> constraints) {
            //should this be Array<String> - T[] the new A2F(String, T)
            return null;
        }

        @Override
        public Void visitDeclared_Declared(AnnotatedDeclaredType parameter, AnnotatedDeclaredType argument, Set<AFConstraint> constraints) {
            if (argument.wasRaw() || parameter.wasRaw()) {
                return null;
            }

            final AnnotatedDeclaredType paramAsArgument = DefaultTypeHierarchy.castedAsSuper(parameter, argument);
            if (paramAsArgument == null) {
                return null;
            }

            final List<AnnotatedTypeMirror> argTypeArgs = argument.getTypeArguments();
            final List<AnnotatedTypeMirror> paramTypeArgs = paramAsArgument.getTypeArguments();
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
                        constraints.add(new F2A(paramWc.getExtendsBound(), argWc.getExtendsBound()));
                        constraints.add(new A2F(argWc.getSuperBound(), paramWc.getExtendsBound()));
                    } else {
                        //no constraint implied

                    }

                } else {
                    //If F has the form G<..., Yk-1, U, Yk+1, ...>, where U is a type expression that involves Tj, then:
                    if (argTypeArg.getKind() != TypeKind.WILDCARD) {
                        constraints.add(new FIsA(paramTypeArg, argTypeArg));

                    } else {
                        final AnnotatedWildcardType argWc = (AnnotatedWildcardType) argTypeArg;
                        constraints.add(new F2A(paramTypeArg, argWc.getExtendsBound()));
                        constraints.add(new A2F(argWc.getSuperBound(), paramTypeArg));
                    }

                }
            }

            return null;
        }

        @Override
        public Void visitDeclared_Intersection(AnnotatedDeclaredType argument, AnnotatedIntersectionType parameter, Set<AFConstraint> constraints) {

            //Note: AnnotatedIntersectionTypes cannot have a type variable as one of the direct parameters but
            // a type variable may be the type argument to an intersection
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


        @Override
        public Void visitIntersection_Null(AnnotatedIntersectionType argument, AnnotatedNullType parameter, Set<AFConstraint> constraints) {
            return null;
        }

        //------------------------------------------------------------------------
        //Null as argument
        @Override
        public Void visitNull_Array(AnnotatedNullType argument, AnnotatedArrayType parameter, Set<AFConstraint> constraints) {
            return null;
        }

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
            //Note: We expect the A2F constraints where F == a targeted type parameter to already be removed
            //Note: Therefore, parameter should NOT be a target
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

        @Override
        public Void visitNull_Intersection(AnnotatedNullType argument, AnnotatedIntersectionType parameter, Set<AFConstraint> constraints) {
            return null;
        }


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
            return null; //TODO: Just not thought through
        }

        //------------------------------------------------------------------------
        //typevar as argument
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
        public Void visitWildcard_Null(AnnotatedWildcardType argument, AnnotatedNullType parameter, Set<AFConstraint> constraints) {
            return null; //this can happen when we add the constraints between the inferred type and the type parameter bound
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
            constraints.add(new F2A(parameter.getExtendsBound(), parameter.getExtendsBound()));
            constraints.add(new A2F(argument.getSuperBound(),  parameter.getSuperBound()));
            return null;
        }
    }
}
