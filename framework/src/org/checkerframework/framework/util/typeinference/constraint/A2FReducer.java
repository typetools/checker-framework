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
 * Created by jburke on 1/21/15.
 */
//TODO: What about methods that are recursive
public class A2FReducer implements AFReducer {

    protected final A2FReducingVisitor visitor;
    private final AnnotatedTypeFactory typeFactory;

    public A2FReducer(final AnnotatedTypeFactory typeFactory) {
        this.typeFactory = typeFactory;
        this.visitor = new A2FReducingVisitor();
    }

    public boolean reduce(AFConstraint constraint, Set<AFConstraint> newConstraints, Set<AFConstraint> finished) {
        if (constraint instanceof A2F) {
            final A2F a2f = (A2F) constraint;
            visitor.visit(a2f.argument, a2f.formalParameter, newConstraints);
            return true;

        } else {
            return false;
        }
    }

    class A2FReducingVisitor extends AbstractAtmComboVisitor<Void, Set<AFConstraint>> {

        //TODO: COMMENT ASSUMES THE CONSTRAINT THAT GAVE RISE TO THIS CALL IS NOT IN COMPLEX CONSTRAINTS

        @Override
        protected String defaultErrorMessage(AnnotatedTypeMirror argument, AnnotatedTypeMirror parameter, Set<AFConstraint> afConstraints) {
            return "Unexpected A2F Combination:\b"
                    + "argument=" + argument + "\n"
                    + "parameter=" + parameter + "\n"
                    + "constraints=[\n" + PluginUtil.join(", ", afConstraints) + "\n]";
        }

        //------------------------------------------------------------------------
        //Arrays as arguments

        @Override
        public Void visitArray_Array(AnnotatedArrayType argument, AnnotatedArrayType parameter, Set<AFConstraint> constraints) {
            constraints.add(new A2F(argument.getComponentType(), parameter.getComponentType()));   //TODO: HANDLE THE INVARIANT ARRAYS PARAM
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

        //------------------------------------------------------------------------
        //Declared as argument
        @Override
        public Void visitDeclared_Array(AnnotatedDeclaredType argument, AnnotatedArrayType parameter, Set<AFConstraint> constraints) {
            //should this be Array<String> - T[] the new A2F(String, T)
            return null;
        }

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

                if (paramTypeArg.getKind() == TypeKind.WILDCARD) {
                    final AnnotatedWildcardType paramWc = (AnnotatedWildcardType) paramTypeArg;

                    //TODO: This section needs to be squared with the fact that we have two bounds.  This more closely
                    //TODO: mimics the Java one bounded approach
                    if (TypeArgInferenceUtil.isExplicitlyExtendsBounded(paramWc)) {   //TODO: DIFFERENT METHOD?
                        //normally, if it were a <?> and not a <? extends SomeClass> Java would do nothing with
                        //this argument, but since our bounds might have annotation on them we use the !isSuperBound condition
                        //to handle both <?> and <? extends SomeClass>
                        final AnnotatedTypeMirror paramUpperBound = ((AnnotatedWildcardType) paramTypeArg).getExtendsBound();

                        if (argTypeArg.getKind() == TypeKind.WILDCARD) {
                            final AnnotatedTypeMirror argUpperBound = ((AnnotatedWildcardType) argTypeArg).getExtendsBound();
                            constraints.add(new A2F(argUpperBound, paramUpperBound));

                        } else {
                            constraints.add(new A2F(argTypeArg, paramUpperBound));
                        }

                    } else { //handle super bound

                        if (argTypeArg.getKind() == TypeKind.WILDCARD) {
                            if (!((Type.WildcardType) argTypeArg.getUnderlyingType()).isSuperBound()) {
                                final AnnotatedTypeMirror argLowerBound = ((AnnotatedWildcardType) argTypeArg).getSuperBound();
                                constraints.add(new F2A(paramWc.getSuperBound(), argLowerBound));
                            }

                        } else {
                            constraints.add(new F2A(paramWc.getSuperBound(), argTypeArg));
                        }

                    }

                } else {
                    constraints.add(new FIsA(paramTypeArgs.get(i), argTypeArgs.get(i)));

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

        @Override  //TODO: COULD THE WILDCARD EXTEND A TYPE VARIABLE? MAKE TEST
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
