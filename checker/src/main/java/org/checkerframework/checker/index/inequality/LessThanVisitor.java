package org.checkerframework.checker.index.inequality;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.index.Subsequence;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.FlowExpressionParseUtil;

public class LessThanVisitor extends BaseTypeVisitor<LessThanAnnotatedTypeFactory> {

    private static final @CompilerMessageKey String FROM_GT_TO = "from.gt.to";

    public LessThanVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    protected void commonAssignmentCheck(
            Tree varTree, ExpressionTree valueTree, @CompilerMessageKey String errorKey) {

        // check that when an assignment to a variable declared as @HasSubsequence(a, from, to)
        // occurs, from <= to.

        Subsequence subSeq = Subsequence.getSubsequenceFromTree(varTree, atypeFactory);
        if (subSeq != null) {
            AnnotationMirror anm;
            try {
                anm =
                        atypeFactory.getAnnotationMirrorFromJavaExpressionString(
                                subSeq.from, varTree, getCurrentPath());
            } catch (FlowExpressionParseUtil.FlowExpressionParseException e) {
                anm = null;
            }

            if (anm == null || !LessThanAnnotatedTypeFactory.isLessThanOrEqual(anm, subSeq.to)) {
                // issue an error
                checker.report(
                        Result.failure(
                                FROM_GT_TO,
                                subSeq.from,
                                subSeq.to,
                                anm == null ? "@LessThanUnknown" : anm,
                                subSeq.to,
                                subSeq.to),
                        valueTree);
            }
        }

        super.commonAssignmentCheck(varTree, valueTree, errorKey);
    }

    @Override
    protected void commonAssignmentCheck(
            AnnotatedTypeMirror varType,
            AnnotatedTypeMirror valueType,
            Tree valueTree,
            @CompilerMessageKey String errorKey) {
        // If value is less than all expressions in the annotation in varType,
        // using the Value Checker, then skip the common assignment check.
        List<String> expressions =
                LessThanAnnotatedTypeFactory.getLessThanExpressions(
                        varType.getEffectiveAnnotationInHierarchy(atypeFactory.UNKNOWN));
        if (expressions != null) {
            boolean isLessThan = true;
            for (String expression : expressions) {
                if (!atypeFactory.isLessThanByValue(valueTree, expression, getCurrentPath())) {
                    isLessThan = false;
                }
            }
            if (isLessThan) {
                if (checker.hasOption("showchecks")) {
                    // Print the success message because super isn't called.
                    long valuePos = positions.getStartPosition(root, valueTree);
                    System.out.printf(
                            " %s (line %3d): %s %s%n     actual: %s %s%n   expected: %s %s%n",
                            "success: actual is subtype of expected",
                            (root.getLineMap() != null
                                    ? root.getLineMap().getLineNumber(valuePos)
                                    : -1),
                            valueTree.getKind(),
                            valueTree,
                            valueType.getKind(),
                            valueType.toString(),
                            varType.getKind(),
                            varType.toString());
                }
                // skip call to super.
                return;
            }
        }
        super.commonAssignmentCheck(varType, valueType, valueTree, errorKey);
    }
}
