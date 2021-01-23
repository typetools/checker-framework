package org.checkerframework.checker.index.inequality;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.index.IndexAbstractTransfer;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.common.value.ValueCheckerUtils;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.NumericalSubtractionNode;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.expression.ValueLiteral;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.util.JavaExpressionParseUtil;

/**
 * Implements 3 refinement rules:
 *
 * <ul>
 *   <li>1. if left &gt; right, right has type {@code @LessThan("left")}
 *   <li>2. if left &ge; right, right has type {@code @LessThan("left + 1")}
 *   <li>3. if {@code 0 < right}, {@code left - right} has type {@code @LessThan("left")}
 * </ul>
 *
 * These are implemented generally, so they also apply to e.g. &lt; and &le; comparisons.
 */
public class LessThanTransfer extends IndexAbstractTransfer {

    public LessThanTransfer(CFAnalysis analysis) {
        super(analysis);
    }

    /** Case 1. */
    @Override
    protected void refineGT(
            Node left,
            AnnotationMirror leftAnno,
            Node right,
            AnnotationMirror rightAnno,
            CFStore store,
            TransferInput<CFValue, CFStore> in) {
        LessThanAnnotatedTypeFactory factory =
                (LessThanAnnotatedTypeFactory) analysis.getTypeFactory();
        // left > right so right < left
        // Refine right to @LessThan("left")
        JavaExpression leftJe = JavaExpression.fromNode(factory, left);
        if (leftJe != null && leftJe.isUnassignableByOtherCode()) {
            List<String> lessThanExpressions =
                    LessThanAnnotatedTypeFactory.getLessThanExpressions(rightAnno);
            if (lessThanExpressions == null) {
                // right is already bottom, nothing to refine.
                return;
            }
            if (!isDoubleOrFloatLiteral(leftJe)) {
                lessThanExpressions.add(leftJe.toString());
            }
            JavaExpression rightJe = JavaExpression.fromNode(analysis.getTypeFactory(), right);
            store.insertValue(rightJe, factory.createLessThanQualifier(lessThanExpressions));
        }
    }

    /** Case 2. */
    @Override
    protected void refineGTE(
            Node left,
            AnnotationMirror leftAnno,
            Node right,
            AnnotationMirror rightAnno,
            CFStore store,
            TransferInput<CFValue, CFStore> in) {
        // left >= right so right is less than left
        // Refine right to @LessThan("left + 1")

        LessThanAnnotatedTypeFactory factory =
                (LessThanAnnotatedTypeFactory) analysis.getTypeFactory();
        // left > right so right is less than left
        // Refine right to @LessThan("left")
        JavaExpression leftJe = JavaExpression.fromNode(factory, left);
        if (leftJe != null && leftJe.isUnassignableByOtherCode()) {
            List<String> lessThanExpressions =
                    LessThanAnnotatedTypeFactory.getLessThanExpressions(rightAnno);
            if (lessThanExpressions == null) {
                // right is already bottom, nothing to refine.
                return;
            }
            if (!isDoubleOrFloatLiteral(leftJe)) {
                lessThanExpressions.add(incrementedExpression(leftJe));
            }
            JavaExpression rightJe = JavaExpression.fromNode(analysis.getTypeFactory(), right);
            store.insertValue(rightJe, factory.createLessThanQualifier(lessThanExpressions));
        }
    }

    /** Case 3. */
    @Override
    public TransferResult<CFValue, CFStore> visitNumericalSubtraction(
            NumericalSubtractionNode n, TransferInput<CFValue, CFStore> in) {
        LessThanAnnotatedTypeFactory factory =
                (LessThanAnnotatedTypeFactory) analysis.getTypeFactory();
        JavaExpression leftJe = JavaExpression.fromNode(factory, n.getLeftOperand());
        if (leftJe != null && leftJe.isUnassignableByOtherCode()) {
            ValueAnnotatedTypeFactory valueFactory = factory.getValueAnnotatedTypeFactory();
            Long right = ValueCheckerUtils.getMinValue(n.getRightOperand().getTree(), valueFactory);
            if (right != null && 0 < right) {
                // left - right < left iff 0 < right
                List<String> expressions = getLessThanExpressions(n.getLeftOperand());
                if (expressions == null) {
                    expressions = new ArrayList<>();
                }
                if (!isDoubleOrFloatLiteral(leftJe)) {
                    expressions.add(leftJe.toString());
                }
                AnnotationMirror refine = factory.createLessThanQualifier(expressions);
                CFValue value = analysis.createSingleAnnotationValue(refine, n.getType());
                CFStore info = in.getRegularStore();
                return new RegularTransferResult<>(finishValue(value, info), info);
            }
        }
        return super.visitNumericalSubtraction(n, in);
    }

    /** Return the expressions that {@code node} are less than. */
    private List<String> getLessThanExpressions(Node node) {
        Set<AnnotationMirror> s = analysis.getValue(node).getAnnotations();
        LessThanAnnotatedTypeFactory factory =
                (LessThanAnnotatedTypeFactory) analysis.getTypeFactory();
        if (s != null && !s.isEmpty()) {
            return LessThanAnnotatedTypeFactory.getLessThanExpressions(
                    factory.getQualifierHierarchy()
                            .findAnnotationInHierarchy(s, factory.LESS_THAN_UNKNOWN));
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Return true if {@code expr} is a double or float literal, which can't be parsed by {@link
     * JavaExpressionParseUtil}.
     */
    private boolean isDoubleOrFloatLiteral(JavaExpression expr) {
        if (expr instanceof ValueLiteral) {
            return expr.getType().getKind() == TypeKind.DOUBLE
                    || expr.getType().getKind() == TypeKind.FLOAT;
        } else {
            return false;
        }
    }

    /**
     * Return the string representation of {@code expr + 1}.
     *
     * @param expr a JavaExpression
     * @return the string representation of {@code expr + 1}
     */
    private String incrementedExpression(JavaExpression expr) {
        String exprString = expr.toString();
        if (expr instanceof ValueLiteral) {
            try {
                long literal = Long.parseLong(exprString);
                // It's a literal.
                return Long.toString(literal + 1);
            } catch (NumberFormatException e) {
                // It's not an integral literal.
            }
        }

        // Could do more optimization to merge with a literal at end of `exprString`.  Is that
        // needed?
        return exprString + " + 1";
    }
}
