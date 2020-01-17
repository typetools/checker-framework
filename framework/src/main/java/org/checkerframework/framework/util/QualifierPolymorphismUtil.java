package org.checkerframework.framework.util;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompoundAssignmentTree;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.util.typeinference.TypeArgInferenceUtil;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;

public class QualifierPolymorphismUtil {

    private static final Set<TreePath> visitedPaths = new HashSet<>();

    /**
     * Returns the annotated type that the leaf of path is assigned to, if it is within an
     * assignment context. Returns the annotated type that the method invocation at the leaf is
     * assigned to.
     *
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
            AnnotatedTypeMirror receiver = atypeFactory.getReceiverType(methodInvocation);
            res =
                    TypeArgInferenceUtil.assignedToExecutable(
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
                    TypeArgInferenceUtil.assignedToExecutable(
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
            res = TypeArgInferenceUtil.assignedToVariable(atypeFactory, assignmentContext);
        } else {
            throw new BugInCF("AnnotatedTypes.assignedTo: shouldn't be here");
        }
        visitedPaths.remove(path);
        return res;
    }
}
