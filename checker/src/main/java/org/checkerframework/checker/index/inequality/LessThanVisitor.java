package org.checkerframework.checker.index.inequality;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.Tree;

import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.index.Subsequence;
import org.checkerframework.checker.index.upperbound.OffsetEquation;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.JavaExpressionParseUtil;
import org.plumelib.util.CollectionsPlume;

import java.util.List;

import javax.lang.model.element.AnnotationMirror;

/** The visitor for the Less Than Checker. */
public class LessThanVisitor extends BaseTypeVisitor<LessThanAnnotatedTypeFactory> {

    private static final @CompilerMessageKey String FROM_GT_TO = "from.gt.to";

    public LessThanVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    protected void commonAssignmentCheck(
            Tree varTree,
            ExpressionTree valueTree,
            @CompilerMessageKey String errorKey,
            Object... extraArgs) {

        // check that when an assignment to a variable declared as @HasSubsequence(a, from, to)
        // occurs, from <= to.

        Subsequence subSeq = Subsequence.getSubsequenceFromTree(varTree, atypeFactory);
        if (subSeq != null) {
            AnnotationMirror anm;
            try {
                anm =
                        atypeFactory.getAnnotationMirrorFromJavaExpressionString(
                                subSeq.from, varTree, getCurrentPath());
            } catch (JavaExpressionParseUtil.JavaExpressionParseException e) {
                anm = null;
            }

            LessThanAnnotatedTypeFactory factory = getTypeFactory();

            if (anm == null || !factory.isLessThanOrEqual(anm, subSeq.to)) {
                // issue an error
                checker.reportError(
                        valueTree,
                        FROM_GT_TO,
                        subSeq.from,
                        subSeq.to,
                        anm == null ? "@LessThanUnknown" : anm,
                        subSeq.to,
                        subSeq.to);
            }
        }

        super.commonAssignmentCheck(varTree, valueTree, errorKey, extraArgs);
    }

    @Override
    protected void commonAssignmentCheck(
            AnnotatedTypeMirror varType,
            AnnotatedTypeMirror valueType,
            Tree valueTree,
            @CompilerMessageKey String errorKey,
            Object... extraArgs) {
        // If value is less than all expressions in the annotation in varType,
        // using the Value Checker, then skip the common assignment check.
        // Also skip the check if the only expression is "a + 1" and the valueTree is "a".
        List<String> expressions =
                getTypeFactory()
                        .getLessThanExpressions(
                                varType.getEffectiveAnnotationInHierarchy(
                                        atypeFactory.LESS_THAN_UNKNOWN));
        if (expressions != null) {
            boolean isLessThan = true;
            for (String expression : expressions) {
                if (!atypeFactory.isLessThanByValue(valueTree, expression, getCurrentPath())) {
                    isLessThan = false;
                }
            }
            if (!isLessThan && expressions.size() == 1) {
                String expression = expressions.get(0);
                if (expression.endsWith(" + 1")) {
                    String value = expression.substring(0, expression.length() - 4);
                    if (valueTree.getKind() == Tree.Kind.IDENTIFIER) {
                        String id = ((IdentifierTree) valueTree).getName().toString();
                        if (id.equals(value)) {
                            isLessThan = true;
                        }
                    }
                }
            }

            if (isLessThan) {
                // Print the messages because super isn't called.
                commonAssignmentCheckStartDiagnostic(varType, valueType, valueTree);
                commonAssignmentCheckEndDiagnostic(
                        true, "isLessThan", varType, valueType, valueTree);
                // skip call to super, everything is OK.
                return;
            }
        }
        super.commonAssignmentCheck(varType, valueType, valueTree, errorKey, extraArgs);
    }

    @Override
    protected boolean isTypeCastSafe(AnnotatedTypeMirror castType, AnnotatedTypeMirror exprType) {

        AnnotationMirror exprLTAnno =
                exprType.getEffectiveAnnotationInHierarchy(atypeFactory.LESS_THAN_UNKNOWN);

        if (exprLTAnno != null) {
            LessThanAnnotatedTypeFactory factory = getTypeFactory();
            List<String> initialAnnotations = factory.getLessThanExpressions(exprLTAnno);

            if (initialAnnotations != null) {
                List<String> updatedAnnotations =
                        CollectionsPlume.mapList(
                                annotation ->
                                        OffsetEquation.createOffsetFromJavaExpression(annotation)
                                                .toString(),
                                initialAnnotations);

                exprType.replaceAnnotation(
                        atypeFactory.createLessThanQualifier(updatedAnnotations));
            }
        }

        return super.isTypeCastSafe(castType, exprType);
    }
}
