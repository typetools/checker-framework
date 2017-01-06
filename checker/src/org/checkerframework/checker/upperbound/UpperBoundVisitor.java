package org.checkerframework.checker.upperbound;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.upperbound.qual.LTLengthOf;
import org.checkerframework.checker.upperbound.qual.LTOMLengthOf;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

/** Warns about array accesses that could be too high. */
public class UpperBoundVisitor extends BaseTypeVisitor<UpperBoundAnnotatedTypeFactory> {

    protected final List<ExecutableElement> IndexFirstArgListMethods;

    private static final String UPPER_BOUND = "array.access.unsafe.high";
    private static final String UPPER_BOUND_LIST = "list.access.unsafe.high";

    public UpperBoundVisitor(BaseTypeChecker checker) {
        super(checker);
        this.IndexFirstArgListMethods = new ArrayList<ExecutableElement>();
        this.IndexFirstArgListMethods.add(
                TreeUtils.getMethod(
                        "java.util.List", "get", 1, checker.getProcessingEnvironment()));
        this.IndexFirstArgListMethods.add(
                TreeUtils.getMethod(
                        "java.util.List", "set", 2, checker.getProcessingEnvironment()));
        // can't handle until TreeUtils.getMethod has a way to precisely handle method overloading
        // this.IndexFirstArgListMethods.add(TreeUtils.getMethod("java.util.List", "remove", 1, checker.getProcessingEnvironment()));
        this.IndexFirstArgListMethods.add(
                TreeUtils.getMethod(
                        "java.util.List", "listIterator", 1, checker.getProcessingEnvironment()));
        this.IndexFirstArgListMethods.add(
                TreeUtils.getMethod(
                        "java.util.List", "addAll", 2, checker.getProcessingEnvironment()));
        this.IndexFirstArgListMethods.add(
                TreeUtils.getMethod(
                        "java.util.List", "add", 2, checker.getProcessingEnvironment()));
    }

    /**
     * When the visitor reachs an array access, it needs to check a couple of things. First, it
     * checks if the index has been assigned a reasonable UpperBound type: only an index with type
     * LTLengthOf(arr) is safe to access arr. If that fails, it checks if the access is still safe.
     * To do so, it checks if the MinLen checker knows the minimum length of arr by querying the
     * MinLenATF. If the MinLen of the array is known, the visitor can check if the index is less
     * than the MinLen, using the Value Checker. If so then the access is still safe. Otherwise,
     * report a potential unsafe access.
     */
    @Override
    public Void visitArrayAccess(ArrayAccessTree tree, Void type) {
        ExpressionTree indexTree = tree.getIndex();
        ExpressionTree arrTree = tree.getExpression();
        String arrName = FlowExpressions.internalReprOf(this.atypeFactory, arrTree).toString();
        AnnotatedTypeMirror indexType = atypeFactory.getAnnotatedType(indexTree);

        AnnotatedTypeMirror sameLenType = atypeFactory.sameLenTypeFromExpressionTree(arrTree);

        // Need to be able to check these as part of the conditional below.
        // Find max because it's important to determine whether the index is
        // less than the minimum length of the array. If it could be any
        // of several values, only the max is of interest.
        Integer valMax = atypeFactory.valMaxFromExpressionTree(indexTree);
        int minLen = atypeFactory.minLenFromExpressionTree(arrTree);

        // Is indexType LTL/LTOM of a set containing arrName?
        if ((indexType.hasAnnotation(LTLengthOf.class)
                        || indexType.hasAnnotation(LTOMLengthOf.class))
                && (UpperBoundUtils.hasValue(indexType, arrName, sameLenType))) {
            // If so, this is safe - get out of here.
            return super.visitArrayAccess(tree, type);
        } else if (valMax != null && minLen != -1 && valMax < minLen) {
            return super.visitArrayAccess(tree, type);
        } else {
            // Unsafe, since neither the Upper bound or MinLen checks succeeded.
            checker.report(
                    Result.warning(UPPER_BOUND, indexType.toString(), arrName, arrName), indexTree);
            return super.visitArrayAccess(tree, type);
        }
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree tree, Void type) {
        if (isFirstArgListMethod(tree)) {
            ExpressionTree indexTree = tree.getArguments().get(0);
            ExpressionTree lstTree = tree.getMethodSelect();
            String lstName = FlowExpressions.internalReprOf(this.atypeFactory, lstTree).toString();
            AnnotatedTypeMirror indexType = atypeFactory.getAnnotatedType(indexTree);

            // Need to be able to check these as part of the conditional below.
            // Find max because it's important to determine whether the index is
            // less than the minimum length of the array. If it could be any
            // of several values, only the max is of interest.
            Integer valMax = atypeFactory.valMaxFromExpressionTree(indexTree);
            int minLen = atypeFactory.minLenFromExpressionTree(lstTree);

            AnnotatedTypeMirror sameLenType = atypeFactory.sameLenTypeFromExpressionTree(lstTree);

            // Is indexType LTL of a set containing arrName?
            if ((indexType.hasAnnotation(LTLengthOf.class)
                            || indexType.hasAnnotation(LTOMLengthOf.class))
                    && (UpperBoundUtils.hasValue(indexType, lstName, sameLenType))) {
                // If so, this is safe - get out of here.
                return super.visitMethodInvocation(tree, type);
            } else if (valMax != null && minLen != -1 && valMax < minLen) {
                return super.visitMethodInvocation(tree, type);
            } else {
                // Unsafe, since neither the Upper bound or MinLen checks succeeded.
                checker.report(
                        Result.warning(UPPER_BOUND_LIST, indexType.toString(), lstName, lstName),
                        indexTree);
                return super.visitMethodInvocation(tree, type);
            }
        }
        return super.visitMethodInvocation(tree, type);
    }

    private boolean isFirstArgListMethod(MethodInvocationTree tree) {
        for (ExecutableElement e : this.IndexFirstArgListMethods) {
            if (TreeUtils.isMethodInvocation(tree, e, this.checker.getProcessingEnvironment())) {
                return true;
            }
        }
        return false;
    }
}
