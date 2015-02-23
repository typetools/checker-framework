package org.checkerframework.framework.util.typeinference.constraint;

import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.*;
import org.checkerframework.framework.type.DefaultTypeHierarchy;
import org.checkerframework.framework.type.visitor.AbstractAtmComboVisitor;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.PluginUtil;

import javax.lang.model.type.TypeKind;
import java.util.List;
import java.util.Set;
import static org.checkerframework.framework.util.typeinference.TypeArgInferenceUtil.*;

/**
 * Reduces F2A results
 * @see org.checkerframework.framework.util.typeinference.constraint.AFReducer
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

    class F2AReducingVisitor extends AbstractAtmComboVisitor<Void, Set<AFConstraint>> {

        @Override
        protected String defaultErrorMessage(AnnotatedTypeMirror argument, AnnotatedTypeMirror parameter, Set<AFConstraint> afConstraints) {
            return "Unexpected F2A Combination:\b"
                 + "argument=" + argument + "\n"
                 + "parameter=" + parameter + "\n"
                 + "constraints=[\n" + PluginUtil.join(", ", afConstraints) + "\n]";
        }
        @Override
        public Void visitArray_Array(AnnotatedArrayType parameter, AnnotatedArrayType argument, Set<AFConstraint> constraints) {
            constraints.add(new F2A(parameter.getComponentType(), argument.getComponentType()));   //TODO: HANDLE THE INVARIANT ARRAYS PARAM
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

                if (paramTypeArg.getKind() == TypeKind.WILDCARD) {
                    visitTypeArgWildF((AnnotatedWildcardType) paramTypeArg, argTypeArg, constraints);

                } else {
                    visitTypeArgNonWildF(paramTypeArg, argTypeArg, constraints);

                }
            }

            return null;
        }

        //If F has the form G<..., Yk-1, U, Yk+1, ...>, where U is a type expression that involves Tj, then:
        private void visitTypeArgNonWildF(AnnotatedTypeMirror parameterTypeArg, AnnotatedTypeMirror argumentTypeArg, Set<AFConstraint> constraints) {
            if (argumentTypeArg.getKind() != TypeKind.WILDCARD) {
                constraints.add(new FIsA(parameterTypeArg, argumentTypeArg));

            } else {
                AnnotatedWildcardType argWc = (AnnotatedWildcardType) argumentTypeArg;
                if (isExplicitlyExtendsBounded(argWc)) {
                    constraints.add(new F2A(parameterTypeArg, argWc.getExtendsBound()));

                } else {
                    constraints.add(new A2F(argWc.getSuperBound(), parameterTypeArg));
                }

            }
        }

        private void visitTypeArgWildF(AnnotatedWildcardType parameterTypeArg, AnnotatedTypeMirror argumentTypeArg, Set<AFConstraint> constraints) {
            if (isExplicitlyExtendsBounded(parameterTypeArg)) {
                visitTypeArgNonWildF(parameterTypeArg.getExtendsBound(), argumentTypeArg, constraints);

            } else if (isUnboundedOrSuperBounded((AnnotatedWildcardType) argumentTypeArg)) {
                constraints.add(new A2F(((AnnotatedWildcardType) argumentTypeArg).getSuperBound(), parameterTypeArg.getSuperBound()));
            }
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

        @Override
        public Void visitDeclared_Null(AnnotatedDeclaredType argument, AnnotatedNullType parameter, Set<AFConstraint> constraints) {
            return null;
        }

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
            return null;  //TODO: Just not handled at the moment
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
            return null; //TODO: Just not handled yet
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
            return null;
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
            constraints.add(new A2F(argument.getExtendsBound(), parameter.getSuperBound())); //TODO: NOT SURE ABOUT THIS ONE
            return null;
        }
    }
}
