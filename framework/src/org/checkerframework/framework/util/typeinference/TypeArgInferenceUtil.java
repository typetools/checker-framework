package org.checkerframework.framework.util.typeinference;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.TypeVariableSubstitutor;
import org.checkerframework.framework.type.visitor.AnnotatedTypeScanner;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Miscellaneous utilities to help in type argument inference.
 */
public class TypeArgInferenceUtil {

    /**
     * Takes an expression tree that must be either a MethodInovcationTree or a NewClassTree (constructor invocation)
     * and returns the arguments to its formal parameters.  An IllegalArgumentException will be thrown if it is neither
     * @param expression a MethodInvocationTree or a NewClassTree
     * @return the list of arguments to Expression
     */
    public static List<? extends ExpressionTree> expressionToArgTrees(
            final ExpressionTree expression) {
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
                            + "couldn't determine arguments from tree: "
                            + expression);
        }

        return argTrees;
    }

    /**
     * Calls get annotated types on a List of trees using the given type factory.
     */
    public static List<AnnotatedTypeMirror> treesToTypes(
            final List<? extends ExpressionTree> argTrees, final AnnotatedTypeFactory typeFactory) {
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
    public static boolean isATarget(
            final AnnotatedTypeMirror type, final Set<TypeVariable> targetTypeVars) {
        return type.getKind() == TypeKind.TYPEVAR
                && targetTypeVars.contains(type.getUnderlyingType());
    }

    /**
     * Given an AnnotatedExecutableType return a set of type variables that represents the
     * generic type parameters of that method
     */
    public static Set<TypeVariable> methodTypeToTargets(final AnnotatedExecutableType methodType) {
        final List<AnnotatedTypeVariable> annotatedTypeVars = methodType.getTypeVariables();
        final Set<TypeVariable> targets = new LinkedHashSet<>(annotatedTypeVars.size());

        for (final AnnotatedTypeVariable atv : annotatedTypeVars) {
            targets.add(atv.getUnderlyingType());
        }

        return targets;
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
        Tree assignmentContext = TreeUtils.getAssignmentContext(path);
        if (assignmentContext == null) {
            return null;
        } else if (assignmentContext instanceof AssignmentTree) {
            ExpressionTree variable = ((AssignmentTree) assignmentContext).getVariable();
            return atypeFactory.getAnnotatedType(variable);
        } else if (assignmentContext instanceof CompoundAssignmentTree) {
            ExpressionTree variable = ((CompoundAssignmentTree) assignmentContext).getVariable();
            return atypeFactory.getAnnotatedType(variable);
        } else if (assignmentContext instanceof MethodInvocationTree) {
            MethodInvocationTree methodInvocation = (MethodInvocationTree) assignmentContext;
            // TODO move to getAssignmentContext
            if (methodInvocation.getMethodSelect() instanceof MemberSelectTree
                    && ((MemberSelectTree) methodInvocation.getMethodSelect()).getExpression()
                            == path.getLeaf()) {
                return null;
            }
            ExecutableElement methodElt = TreeUtils.elementFromUse(methodInvocation);
            AnnotatedTypeMirror receiver = atypeFactory.getReceiverType(methodInvocation);
            return assignedToExecutable(
                    atypeFactory, path, methodElt, receiver, methodInvocation.getArguments());
        } else if (assignmentContext instanceof NewArrayTree) {
            //TODO: I left the previous implementation below, it definitely caused infinite loops if you
            //TODO: called it from places like the TreeAnnotator
            return null;

            // FIXME: This may cause infinite loop
            //            AnnotatedTypeMirror type =
            //                    atypeFactory.getAnnotatedType((NewArrayTree)assignmentContext);
            //            type = AnnotatedTypes.innerMostType(type);
            //            return type;

        } else if (assignmentContext instanceof NewClassTree) {
            // This need to be basically like MethodTree
            NewClassTree newClassTree = (NewClassTree) assignmentContext;
            ExecutableElement constructorElt = InternalUtils.constructor(newClassTree);
            AnnotatedTypeMirror receiver = atypeFactory.fromNewClass(newClassTree);
            return assignedToExecutable(
                    atypeFactory, path, constructorElt, receiver, newClassTree.getArguments());
        } else if (assignmentContext instanceof ReturnTree) {
            HashSet<Kind> kinds = new HashSet<>(Arrays.asList(Kind.LAMBDA_EXPRESSION, Kind.METHOD));
            Tree enclosing = TreeUtils.enclosingOfKind(path, kinds);

            if (enclosing.getKind() == Kind.METHOD) {
                return (atypeFactory.getAnnotatedType((MethodTree) enclosing)).getReturnType();

            } else {
                return atypeFactory.getFnInterfaceFromTree((LambdaExpressionTree) enclosing).first;
            }

        } else if (assignmentContext instanceof VariableTree) {
            return assignedToVariable(atypeFactory, assignmentContext);
        }

        ErrorReporter.errorAbort("AnnotatedTypes.assignedTo: shouldn't be here!");
        return null; // dead code
    }

    private static AnnotatedTypeMirror assignedToExecutable(
            AnnotatedTypeFactory atypeFactory,
            TreePath path,
            ExecutableElement methodElt,
            AnnotatedTypeMirror receiver,
            List<? extends ExpressionTree> arguments) {
        AnnotatedExecutableType method =
                AnnotatedTypes.asMemberOf(
                        atypeFactory.getContext().getTypeUtils(),
                        atypeFactory,
                        receiver,
                        methodElt);
        int treeIndex = -1;
        for (int i = 0; i < arguments.size(); ++i) {
            ExpressionTree argumentTree = arguments.get(i);
            if (isArgument(path, argumentTree)) {
                treeIndex = i;
                break;
            }
        }
        assert treeIndex != -1
                : "Could not find path in MethodInvocationTree.\n" + "treePath=" + path.toString();
        final AnnotatedTypeMirror paramType;
        if (treeIndex >= method.getParameterTypes().size() && methodElt.isVarArgs()) {
            paramType = method.getParameterTypes().get(method.getParameterTypes().size() - 1);
        } else {
            paramType = method.getParameterTypes().get(treeIndex);
        }

        // Examples like this:
        // <T> T outMethod()
        // <U> void inMethod(U u);
        // inMethod(outMethod())
        // would require solving the constraints for both type argument inferences simultaneously
        if (paramType == null || containsUninferredTypeParameter(paramType, method)) {
            return null;
        }

        return paramType;
    }

    /**
     * Returns whether argumentTree is the tree at the leaf of path.
     * if tree is a conditional expression, isArgument is called recursively on the true
     * and false expressions.
     */
    private static boolean isArgument(TreePath path, ExpressionTree argumentTree) {
        argumentTree = TreeUtils.skipParens(argumentTree);
        if (argumentTree == path.getLeaf()) {
            return true;
        } else if (argumentTree.getKind() == Kind.CONDITIONAL_EXPRESSION) {
            ConditionalExpressionTree conditionalExpressionTree =
                    (ConditionalExpressionTree) argumentTree;
            return isArgument(path, conditionalExpressionTree.getTrueExpression())
                    || isArgument(path, conditionalExpressionTree.getFalseExpression());
        }
        return false;
    }

    /**
     * If the variable's type is a type variable, return getAnnotatedTypeLhsNoTypeVarDefault(tree).
     * Rational:
     *
     * For example:
     * <pre>{@code
     * <S> S bar () {...}
     *
     * <T> T foo(T p) {
     *     T local = bar();
     *     return local;
     *   }
     * }</pre>
     * During type argument inference of {@code bar}, the assignment context is  {@code local}.
     * If the local variable default is used, then the type of assignment context type is
     * {@code @Nullable T} and the type argument inferred for {@code bar()} is {@code @Nullable T}.  And an
     * incompatible types in return error is issued.
     * <p>
     * If instead, the local variable default is not applied, then the assignment context type
     * is {@code T} (with lower bound {@code @NonNull Void} and upper bound {@code @Nullable Object}) and the type
     * argument inferred for {@code bar()} is {@code T}.  During dataflow, the type of {@code local} is refined to
     * {@code T} and the return is legal.
     * <p>
     * If the assignment context type was a declared type, for example:
     * <pre>{@code
     * <S> S bar () {...}
     * Object foo() {
     *     Object local = bar();
     *     return local;
     * }
     * }</pre>
     *
     * The local variable default must be used or else the assignment context type is missing an annotation.
     * So, an incompatible types in return error is issued in the above code.  We could improve type argument
     * inference in this case and by using the lower bound of {@code S}  instead of the local variable default.
     *
     * @param atypeFactory AnnotatedTypeFactory
     * @param assignmentContext VariableTree
     * @return AnnotatedTypeMirror of Assignment context
     */
    public static AnnotatedTypeMirror assignedToVariable(
            AnnotatedTypeFactory atypeFactory, Tree assignmentContext) {
        if (atypeFactory instanceof GenericAnnotatedTypeFactory<?, ?, ?, ?>) {
            final GenericAnnotatedTypeFactory<?, ?, ?, ?> gatf =
                    ((GenericAnnotatedTypeFactory<?, ?, ?, ?>) atypeFactory);
            return gatf.getAnnotatedTypeLhsNoTypeVarDefault(assignmentContext);
        } else {
            return atypeFactory.getAnnotatedType(assignmentContext);
        }
    }

    /**
     * @return true if the type contains a use of a type variable from methodType
     */
    private static boolean containsUninferredTypeParameter(
            AnnotatedTypeMirror type, AnnotatedExecutableType methodType) {
        final List<AnnotatedTypeVariable> annotatedTypeVars = methodType.getTypeVariables();
        final List<TypeVariable> typeVars = new ArrayList<>(annotatedTypeVars.size());

        for (AnnotatedTypeVariable annotatedTypeVar : annotatedTypeVars) {
            typeVars.add(annotatedTypeVar.getUnderlyingType());
        }

        // note NULL values creep in because the underlying visitor uses them in various places
        final Boolean result = type.accept(new TypeVariableFinder(), typeVars);
        return result != null && result;
    }

    /**
     * Take a set of annotations and separate them into a mapping of ({@code hierarchy top -> annotations in hierarchy})
     */
    public static Map<AnnotationMirror, AnnotationMirror> createHierarchyMap(
            final Set<AnnotationMirror> annos, final QualifierHierarchy qualifierHierarchy) {
        Map<AnnotationMirror, AnnotationMirror> result = AnnotationUtils.createAnnotationMap();

        for (AnnotationMirror anno : annos) {
            result.put(qualifierHierarchy.getTopAnnotation(anno), anno);
        }

        return result;
    }

    /**
     * Used to detect if the visited type contains one of the type variables in the typeVars parameter
     */
    private static class TypeVariableFinder
            extends AnnotatedTypeScanner<Boolean, List<TypeVariable>> {

        @Override
        protected Boolean scan(
                Iterable<? extends AnnotatedTypeMirror> types, List<TypeVariable> typeVars) {
            if (types == null) {
                return false;
            }
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

            } else if (r2 == null) {
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

    // Substituter requires an input map that the substitute methods build.  We just reuse the same map rather than
    // recreate it each time.
    private final static Map<TypeVariable, AnnotatedTypeMirror> substituteMap = new HashMap<>(5);

    /**
     * Replace all uses of typeVariable with substitution in a copy of toModify using the normal substitution rules,
     * (@see TypeVariableSubstitutor).Return the copy
     */
    public static AnnotatedTypeMirror substitute(
            final TypeVariable typeVariable,
            final AnnotatedTypeMirror substitution,
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
    public static AnnotatedTypeMirror substitute(
            Map<TypeVariable, AnnotatedTypeMirror> substitutions,
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
    public static AnnotatedTypeMirror leastUpperBound(
            final AnnotatedTypeFactory typeFactory, final Iterable<AnnotatedTypeMirror> types) {
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
            lubType = AnnotatedTypes.leastUpperBound(typeFactory, lubType, nextType);
        }

        return lubType;
    }
}
