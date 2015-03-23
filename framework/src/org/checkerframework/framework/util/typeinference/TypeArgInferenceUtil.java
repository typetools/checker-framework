package org.checkerframework.framework.util.typeinference;

import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.TypeVariableSubstitutor;
import org.checkerframework.framework.type.visitor.AnnotatedTypeScanner;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.javacutil.TreeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Types;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Type;

/**
 * Miscellaneous utilities to help in type argument inference.
 */
public class TypeArgInferenceUtil {

    /**
     * Takes an expression tree that must be either a MethodInovcationTree or a NewClassTree (constructor invocation)
     * and returns the arguments to its formal parameters.  An IllegalArgumentException will be thrown if it is neither
     * @param expression A MethodInvocationTree or a NewClassTree
     * @return The list of arguments to Expression
     */
    public static List<? extends ExpressionTree> expressionToArgTrees(final ExpressionTree expression) {
        final List<? extends ExpressionTree> argTrees;
        if (expression.getKind() == Kind.METHOD_INVOCATION) {
            argTrees = ((MethodInvocationTree) expression).getArguments();

        } else if (expression.getKind() == Kind.NEW_CLASS) {
            argTrees = ((NewClassTree) expression).getArguments();

        } else {
            argTrees = null;
        }


        if (argTrees == null) {
            throw new IllegalArgumentException(
                    "TypeArgumentInference.relationsFromMethodArguments:\n"
                            + "couldn't determine arguments from tree: " + expression
            );
        }

        return argTrees;
    }

    /**
     * Calls get annotated types on a List of trees using the given type factory.
     */
    public static List<AnnotatedTypeMirror> treesToTypes(final List<? extends ExpressionTree> argTrees,
                                                         final AnnotatedTypeFactory typeFactory) {
        final List<AnnotatedTypeMirror> argTypes = new ArrayList<>(argTrees.size());
        for (Tree arg : argTrees) {
            argTypes.add(typeFactory.getAnnotatedType(arg));
        }

        return argTypes;
    }

    /**
     * Given a set of type variables for which we are inferring a type, returns true if type is
     * a use of a type variable in the list of targetTypeVars.
     */
    public static boolean isATarget(final AnnotatedTypeMirror type, final Set<TypeVariable> targetTypeVars) {
        return type.getKind() == TypeKind.TYPEVAR && targetTypeVars.contains(type.getUnderlyingType());
    }

    /**
     * Given an AnnotatedExecutableType return a set of type variables that represents the
     * generic type parameters of that method
     */
    public static Set<TypeVariable> methodTypeToTargets(final AnnotatedExecutableType methodType) {
        final List<AnnotatedTypeVariable> annotatedTypeVars = methodType.getTypeVariables();
        final Set<TypeVariable> targets = new LinkedHashSet<>(annotatedTypeVars.size());

        for(final AnnotatedTypeVariable atv : annotatedTypeVars) {
            targets.add(atv.getUnderlyingType());
        }

        return targets;
    }

    /**
     * Returns true if this type is super bounded or unbounded.
     */
    public static boolean isUnboundedOrSuperBounded(final AnnotatedWildcardType wildcardType) {
        return ((Type.WildcardType) wildcardType.getUnderlyingType()).isSuperBound();
    }

    /**
     * Returns true if wildcard type was explicitly unbounded.
     */
    public static boolean isExplicitlyExtendsBounded(final AnnotatedWildcardType wildcardType) {
        return !isUnboundedOrSuperBounded(wildcardType);
    }

    /**
     *
     */
    /**
     * Returns true if this type is super bounded or unbounded.
     */
    public static boolean isUnboundedOrExtendsBounded(final AnnotatedWildcardType wildcardType) {
        return ((Type.WildcardType) wildcardType.getUnderlyingType()).isExtendsBound();
    }

    /**
     * Returns the annotated type that the leaf of path is assigned to, if it
     * is within an assignment context.
     * Returns the annotated type that the method invocation at the leaf
     * is assigned to.
     *
     * @return type that it path leaf is assigned to
     */
    public static AnnotatedTypeMirror assignedTo(AnnotatedTypeFactory atypeFactory, TreePath path) {
        final Types types = atypeFactory.getProcessingEnv().getTypeUtils();
        Tree assignmentContext = TreeUtils.getAssignmentContext(path);
        if (assignmentContext == null) {
            return null;
        } else if (assignmentContext instanceof AssignmentTree) {
            ExpressionTree variable = ((AssignmentTree)assignmentContext).getVariable();
            return atypeFactory.getAnnotatedType(variable);
        } else if (assignmentContext instanceof CompoundAssignmentTree) {
            ExpressionTree variable =
                    ((CompoundAssignmentTree)assignmentContext).getVariable();
            return atypeFactory.getAnnotatedType(variable);
        } else if (assignmentContext instanceof MethodInvocationTree) {
            MethodInvocationTree methodInvocation = (MethodInvocationTree)assignmentContext;
            // TODO move to getAssignmentContext
            if (methodInvocation.getMethodSelect() instanceof MemberSelectTree
                    && ((MemberSelectTree)methodInvocation.getMethodSelect()).getExpression() == path.getLeaf())
                return null;
            ExecutableElement methodElt = TreeUtils.elementFromUse(methodInvocation);
            AnnotatedTypeMirror receiver = atypeFactory.getReceiverType(methodInvocation);
            AnnotatedExecutableType method = AnnotatedTypes.asMemberOf(types, atypeFactory, receiver, methodElt);
            int treeIndex = -1;
            for (int i = 0; i < method.getParameterTypes().size(); ++i) {
                if (TreeUtils.skipParens(methodInvocation.getArguments().get(i)) == path.getLeaf()) {
                    treeIndex = i;
                    break;
                }
            }

            assert treeIndex != -1 :  "Could not find path in method invocation."
                                    + "treePath=" + path.toString() + "\n"
                                    + "methodInvocation=" + methodInvocation;
            if (treeIndex == -1) {
                return null;
            }

            final AnnotatedTypeMirror paramType = method.getParameterTypes().get(treeIndex);

            //Examples like this:
            // <T> T outMethod()
            // <U> void inMethod(U u);
            // inMethod(outMethod())
            // would require solving the constraints for both type argument inferences simultaneously
            if (paramType == null || containsUninferredTypeParameter(paramType, method)) {
                return null;
            }

            return paramType;
        } else if (assignmentContext instanceof NewArrayTree) {
            // FIXME: This may cause infinite loop
            AnnotatedTypeMirror type =
                    atypeFactory.getAnnotatedType((NewArrayTree)assignmentContext);
            type = AnnotatedTypes.innerMostType(type);
            return type;
        } else if (assignmentContext instanceof NewClassTree) {
            // This need to be basically like MethodTree
            NewClassTree newClassTree = (NewClassTree) assignmentContext;
            ExecutableElement constructorElt = InternalUtils.constructor(newClassTree);
            AnnotatedTypeMirror receiver = atypeFactory.getAnnotatedType(newClassTree.getIdentifier());
            AnnotatedExecutableType constructor = AnnotatedTypes.asMemberOf(types, atypeFactory, receiver, constructorElt);
            int treeIndex = -1;
            for (int i = 0; i < constructor.getParameterTypes().size(); ++i) {
                if (TreeUtils.skipParens(newClassTree.getArguments().get(i)) == path.getLeaf()) {
                    treeIndex = i;
                    break;
                }
            }

            assert treeIndex != -1 :  "Could not find path in NewClassTre."
                    + "treePath=" + path.toString() + "\n"
                    + "methodInvocation=" + newClassTree;
            if (treeIndex == -1) {
                return null;
            }

            return constructor.getParameterTypes().get(treeIndex);
        } else if (assignmentContext instanceof ReturnTree) {
            MethodTree method = TreeUtils.enclosingMethod(path);
            return (atypeFactory.getAnnotatedType(method)).getReturnType();
        } else if (assignmentContext instanceof VariableTree) {
            if (atypeFactory instanceof GenericAnnotatedTypeFactory<?,?,?,?>) {
                final GenericAnnotatedTypeFactory<?,?,?,?> gatf = ((GenericAnnotatedTypeFactory<?,?,?,?>) atypeFactory);
                boolean oldFlow = gatf.getUseFlow();
                gatf.setUseFlow(false);
                final AnnotatedTypeMirror type = gatf.getAnnotatedType(assignmentContext);
                gatf.setUseFlow(oldFlow);
                return type;

            } else {
                return atypeFactory.getAnnotatedType(assignmentContext);
            }

        }

        ErrorReporter.errorAbort("AnnotatedTypes.assignedTo: shouldn't be here!");
        return null; // dead code
    }

    /**
     * @return true if the type contains a use of a type variable from methodType
     */
    private static boolean containsUninferredTypeParameter(AnnotatedTypeMirror type,
                                                           AnnotatedExecutableType methodType) {
        final List<AnnotatedTypeVariable> annotatedTypeVars = methodType.getTypeVariables();
        final List<TypeVariable> typeVars = new ArrayList<>(annotatedTypeVars.size());

        for (AnnotatedTypeVariable annotatedTypeVar : annotatedTypeVars) {
            typeVars.add(annotatedTypeVar.getUnderlyingType());
        }

        //note NULL values creep in because the underlying visitor uses them in various places
        final Boolean result = type.accept(new TypeVariableFinder(), typeVars);
        return result != null && result;
    }

    /**
     * Used to detect if the visited type contains one of the type variables in the typeVars parameter
     */
    private static class TypeVariableFinder extends AnnotatedTypeScanner<Boolean, List<TypeVariable>> {

        @Override
        protected Boolean scan(Iterable<? extends AnnotatedTypeMirror> types, List<TypeVariable> typeVars) {
            if (types == null)
                return false;
            Boolean result = false;
            Boolean first = true;
            for (AnnotatedTypeMirror type : types) {
                result = (first ? scan(type, typeVars) : scanAndReduce(type, typeVars, result));
                first = false;
            }
            return result;
        }

        @Override
        protected Boolean reduce(Boolean r1, Boolean r2) {
            if (r1 == null) {
                return r2 != null && r2;

            } else if(r2 == null) {
                return r1;
            }

            return r1 || r2;
        }

        @Override
        public Boolean visitTypeVariable(AnnotatedTypeVariable type, List<TypeVariable> typeVars) {
            if (typeVars.contains(type.getUnderlyingType())) {
                return true;
            } else {
                return super.visitTypeVariable(type, typeVars);
            }
        }
    }

    /*
     * Various TypeArgumentInference steps require substituting types for type arguments that have already been
     * inferred into constraints that are used infer other type arguments.  Substituter is used in
     * the utility methods to do this.
     */
    private final static TypeVariableSubstitutor substitutor = new TypeVariableSubstitutor();

    //Substituter requires an input map that the substitute methods build.  We just reuse the same map rather than
    //recreate it each time.
    private final static Map<TypeVariable, AnnotatedTypeMirror> substituteMap = new HashMap<>(5);

    /**
     * Replace all uses of typeVariable with substitution in a copy of toModify using the normal substitution rules,
     * (@see TypeVariableSubstitutor).Return the copy
     */
    public static AnnotatedTypeMirror substitute(final TypeVariable typeVariable, final AnnotatedTypeMirror substitution,
                                                 final AnnotatedTypeMirror toModify) {
        substituteMap.clear();
        substituteMap.put(typeVariable, substitution.deepCopy());

        final AnnotatedTypeMirror toModifyCopy = toModify.deepCopy();
        substitutor.substitute(substituteMap, toModifyCopy);
        return toModifyCopy;
    }

    /**
     * Create a copy of toModify. In the copy,
     *      For each pair {@code typeVariable -> annotated type}
     *          replace uses of typeVariable with the corresponding annotated type using
     *          normal substitution rules (@see TypeVariableSubstitutor)
     * Return the copy
     */
    public static AnnotatedTypeMirror substitute(Map<TypeVariable, AnnotatedTypeMirror> substitutions,
                                                 final AnnotatedTypeMirror toModify) {
        final AnnotatedTypeMirror substitution = substitutions.get(toModify.getUnderlyingType());
        if (substitution != null) {
            return substitution.deepCopy();
        }

        final AnnotatedTypeMirror toModifyCopy = toModify.deepCopy();
        substitutor.substitute(substitutions, toModifyCopy);
        return toModifyCopy;
    }

    /**
     * Successively calls least upper bound on the elements of types.  Unlike leastUpperBound,
     * this method will box primitives if necessary
     */
    public static AnnotatedTypeMirror leastUpperBound(final AnnotatedTypeFactory typeFactory,
                                                      final Iterable<AnnotatedTypeMirror> types) {
        final Iterator<AnnotatedTypeMirror> typesIter = types.iterator();
        if (!typesIter.hasNext()) {
            ErrorReporter.errorAbort("Calling LUB on empty list!");
        }
        AnnotatedTypeMirror lubType = typesIter.next();
        AnnotatedTypeMirror nextType = null;
        while (typesIter.hasNext()) {
            nextType = typesIter.next();

            if (lubType.getKind().isPrimitive()) {
                if (!nextType.getKind().isPrimitive()) {
                    lubType = typeFactory.getBoxedType((AnnotatedPrimitiveType) lubType);
                }
            } else if (nextType.getKind().isPrimitive()) {
                if (!lubType.getKind().isPrimitive()) {
                    nextType = typeFactory.getBoxedType((AnnotatedPrimitiveType) nextType);
                }
            }
            lubType = AnnotatedTypes.leastUpperBound(typeFactory.getProcessingEnv(), typeFactory, lubType, nextType);
        }

        return lubType;
    }
}
