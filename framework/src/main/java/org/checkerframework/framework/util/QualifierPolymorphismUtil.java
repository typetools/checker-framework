package org.checkerframework.framework.util;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.visitor.AnnotatedTypeScanner;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeAnnotationUtils;

/**
 * A utility class made for {@link org.checkerframework.framework.type.poly.QualifierPolymorphism}.
 */
public class QualifierPolymorphismUtil {

    /** A set of {@code TreePath} which contains the visited {@code TreePath}s. */
    private static final Set<TreePath> visitedPaths = new HashSet<>();

    /**
     * Returns the annotated type that the leaf of path is assigned to, if it is within an
     * assignment context. Returns the annotated type that the method invocation at the leaf is
     * assigned to. If the result is a primitive, return the boxed version.
     *
     * @param atypeFactory the type factory
     * @param path the tree path
     * @return type that path leaf is assigned to
     */
    public static AnnotatedTypeMirror assignedTo(AnnotatedTypeFactory atypeFactory, TreePath path) {
        if (visitedPaths.contains(path)) {
            // inform the caller to skip assignment context resolution
            return null;
        }

        visitedPaths.add(path);

        Tree assignmentContext = TreeUtils.getAssignmentContext(path);
        AnnotatedTypeMirror res;
        if (assignmentContext == null) {
            res = null;
        } else if (assignmentContext instanceof AssignmentTree) {
            ExpressionTree variable = ((AssignmentTree) assignmentContext).getVariable();
            res = atypeFactory.getAnnotatedType(variable);
        } else if (assignmentContext instanceof CompoundAssignmentTree) {
            ExpressionTree variable = ((CompoundAssignmentTree) assignmentContext).getVariable();
            res = atypeFactory.getAnnotatedType(variable);
        } else if (assignmentContext instanceof MethodInvocationTree) {
            MethodInvocationTree methodInvocation = (MethodInvocationTree) assignmentContext;
            ExecutableElement methodElt = TreeUtils.elementFromUse(methodInvocation);
            // TODO move to getAssignmentContext
            //        	if (methodInvocation.getMethodSelect() instanceof MemberSelectTree
            //        			&& ((MemberSelectTree) methodInvocation.getMethodSelect()).getExpression()
            //        			== path.getLeaf()) {
            //        		// treepath's leaf is assigned to the method declared receiver type
            //        		AnnotatedExecutableType declMethodType =
            // atypeFactory.getAnnotatedType(methodElt);
            //        		return declMethodType.getReceiverType();
            //            } // Removing above if block causes StackOverflowException via
            // getReceiverType ->
            // assignedTo -> getReceiverType loop!
            //            AnnotatedTypeMirror receiver = null;
            //            try {
            //                receiver = atypeFactory.getReceiverType(methodInvocation);
            //            } catch (Throwable e) {
            //                new Object();
            //            }
            AnnotatedTypeMirror receiver = atypeFactory.getReceiverType(methodInvocation);
            res =
                    assignedToExecutable(
                            atypeFactory,
                            path,
                            methodElt,
                            receiver,
                            methodInvocation.getArguments());
        } else if (assignmentContext instanceof NewArrayTree) {
            // TODO: I left the previous implementation below, it definitely caused infinite loops
            // TODO: if you called it from places like the TreeAnnotator.
            res = null;

            // TODO: This may cause infinite loop
            //            AnnotatedTypeMirror type =
            //                    atypeFactory.getAnnotatedType((NewArrayTree)assignmentContext);
            //            type = AnnotatedTypes.innerMostType(type);
            //            return type;

        } else if (assignmentContext instanceof NewClassTree) {
            // This need to be basically like MethodTree
            NewClassTree newClassTree = (NewClassTree) assignmentContext;
            ExecutableElement constructorElt = TreeUtils.constructor(newClassTree);
            AnnotatedTypeMirror receiver = atypeFactory.fromNewClass(newClassTree);
            res =
                    assignedToExecutable(
                            atypeFactory,
                            path,
                            constructorElt,
                            receiver,
                            newClassTree.getArguments());
        } else if (assignmentContext instanceof ReturnTree) {
            HashSet<Kind> kinds = new HashSet<>(Arrays.asList(Kind.LAMBDA_EXPRESSION, Kind.METHOD));
            Tree enclosing = TreeUtils.enclosingOfKind(path, kinds);

            if (enclosing.getKind() == Kind.METHOD) {
                res = atypeFactory.getAnnotatedType((MethodTree) enclosing).getReturnType();
            } else {
                Pair<AnnotatedTypeMirror, AnnotatedExecutableType> fninf =
                        atypeFactory.getFnInterfaceFromTree((LambdaExpressionTree) enclosing);
                res = fninf.second.getReturnType();
            }

        } else if (assignmentContext instanceof VariableTree) {
            res = assignedToVariable(atypeFactory, assignmentContext);
        } else {
            throw new BugInCF("AnnotatedTypes.assignedTo: shouldn't be here");
        }
        visitedPaths.remove(path);
        return res;
    }

    /**
     * Return the annotated type that is assigned to executable.
     *
     * @param atypeFactory the type factory
     * @param path the tree path
     * @param methodElt the method element
     * @param receiver the receiver
     * @param arguments the passed arguments
     * @return the annotated type
     */
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
        final AnnotatedTypeMirror paramType;
        if (treeIndex == -1) {
            // The tree wasn't found as an argument, so it has to be the receiver.
            // This can happen for inner class constructors that take an outer class argument.
            paramType = method.getReceiverType();
        } else if (treeIndex + 1 >= method.getParameterTypes().size() && methodElt.isVarArgs()) {
            AnnotatedTypeMirror varArgType =
                    method.getParameterTypes().get(method.getParameterTypes().size() - 1);
            List<AnnotatedTypeMirror> params =
                    AnnotatedTypes.expandVarArgs(atypeFactory, method, arguments);
            if (params.get(params.size() - 1).equals(varArgType)) {
                return varArgType;
            }
            paramType = ((AnnotatedArrayType) varArgType).getComponentType();
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
     * Returns whether argumentTree is the tree at the leaf of path. if tree is a conditional
     * expression, isArgument is called recursively on the true and false expressions.
     *
     * @param path the tree path
     * @param argumentTree the argument tree
     * @return true if argumentTree is the tree at the leaf of path, else false
     */
    private static boolean isArgument(TreePath path, ExpressionTree argumentTree) {
        argumentTree = TreeUtils.withoutParens(argumentTree);
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
     * <p>For example:
     *
     * <pre>{@code
     * <S> S bar () {...}
     *
     * <T> T foo(T p) {
     *     T local = bar();
     *     return local;
     *   }
     * }</pre>
     *
     * During type argument inference of {@code bar}, the assignment context is {@code local}. If
     * the local variable default is used, then the type of assignment context type is
     * {@code @Nullable T} and the type argument inferred for {@code bar()} is {@code @Nullable T}.
     * And an incompatible types in return error is issued.
     *
     * <p>If instead, the local variable default is not applied, then the assignment context type is
     * {@code T} (with lower bound {@code @NonNull Void} and upper bound {@code @Nullable Object})
     * and the type argument inferred for {@code bar()} is {@code T}. During dataflow, the type of
     * {@code local} is refined to {@code T} and the return is legal.
     *
     * <p>If the assignment context type was a declared type, for example:
     *
     * <pre>{@code
     * <S> S bar () {...}
     * Object foo() {
     *     Object local = bar();
     *     return local;
     * }
     * }</pre>
     *
     * The local variable default must be used or else the assignment context type is missing an
     * annotation. So, an incompatible types in return error is issued in the above code. We could
     * improve type argument inference in this case and by using the lower bound of {@code S}
     * instead of the local variable default.
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
     * Check if the {@code type} contains a use of a type variable from {@code methodType}.
     *
     * @param type the annotated type
     * @param methodType the type of an executable
     * @return true if the {@code type} contains a use of a type variable from {@code methodType}.
     */
    private static boolean containsUninferredTypeParameter(
            AnnotatedTypeMirror type, AnnotatedExecutableType methodType) {
        final List<AnnotatedTypeVariable> annotatedTypeVars = methodType.getTypeVariables();
        final List<TypeVariable> typeVars = new ArrayList<>(annotatedTypeVars.size());

        for (AnnotatedTypeVariable annotatedTypeVar : annotatedTypeVars) {
            typeVars.add(
                    (TypeVariable)
                            TypeAnnotationUtils.unannotatedType(
                                    annotatedTypeVar.getUnderlyingType()));
        }

        return containsTypeParameter(type, typeVars);
    }

    /**
     * Returns true if {@code type} contains a use of a type variable in {@code typeVariables}.
     *
     * @param type type to search
     * @param typeVariables collection of type varibles
     * @return true if {@code type} contains a use of a type variable in {@code typeVariables}
     */
    public static boolean containsTypeParameter(
            AnnotatedTypeMirror type, Collection<TypeVariable> typeVariables) {
        // note NULL values creep in because the underlying visitor uses them in various places
        final Boolean result = type.accept(new TypeVariableFinder(), typeVariables);
        return result != null && result;
    }

    /**
     * Used to detect if the visited type contains one of the type variables in the typeVars
     * parameter.
     */
    private static class TypeVariableFinder
            extends AnnotatedTypeScanner<Boolean, Collection<TypeVariable>> {

        @Override
        protected Boolean scan(
                Iterable<? extends AnnotatedTypeMirror> types, Collection<TypeVariable> typeVars) {
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
        public Boolean visitTypeVariable(
                AnnotatedTypeVariable type, Collection<TypeVariable> typeVars) {
            if (typeVars.contains(
                    (TypeVariable) TypeAnnotationUtils.unannotatedType(type.getUnderlyingType()))) {
                return true;
            } else {
                return super.visitTypeVariable(type, typeVars);
            }
        }
    }
}
