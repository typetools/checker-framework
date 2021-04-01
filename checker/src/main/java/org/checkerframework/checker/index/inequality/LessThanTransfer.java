package org.checkerframework.checker.index.inequality;

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
import org.plumelib.util.CollectionsPlume;

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
    // left > right so right < left
    // Refine right to @LessThan("left")
    JavaExpression leftJe = JavaExpression.fromNode(left);
    if (leftJe != null && leftJe.isUnassignableByOtherCode()) {
      if (isDoubleOrFloatLiteral(leftJe)) {
        return;
      }
      LessThanAnnotatedTypeFactory factory =
          (LessThanAnnotatedTypeFactory) analysis.getTypeFactory();
      List<String> lessThanExpressions = factory.getLessThanExpressions(rightAnno);
      if (lessThanExpressions == null) {
        // right is already bottom, nothing to refine.
        return;
      }
      String leftString = leftJe.toString();
      if (!lessThanExpressions.contains(leftString)) {
        lessThanExpressions = CollectionsPlume.append(lessThanExpressions, leftString);
        JavaExpression rightJe = JavaExpression.fromNode(right);
        store.insertValue(rightJe, factory.createLessThanQualifier(lessThanExpressions));
      }
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

    // left > right so right is less than left
    // Refine right to @LessThan("left")
    JavaExpression leftJe = JavaExpression.fromNode(left);
    if (leftJe != null && leftJe.isUnassignableByOtherCode()) {
      if (isDoubleOrFloatLiteral(leftJe)) {
        return;
      }
      LessThanAnnotatedTypeFactory factory =
          (LessThanAnnotatedTypeFactory) analysis.getTypeFactory();
      List<String> lessThanExpressions = factory.getLessThanExpressions(rightAnno);
      if (lessThanExpressions == null) {
        // right is already bottom, nothing to refine.
        return;
      }
      String leftIncremented = incrementedExpression(leftJe);
      if (!lessThanExpressions.contains(leftIncremented)) {
        lessThanExpressions = CollectionsPlume.append(lessThanExpressions, leftIncremented);
        JavaExpression rightJe = JavaExpression.fromNode(right);
        store.insertValue(rightJe, factory.createLessThanQualifier(lessThanExpressions));
      }
    }
  }

  /** Case 3. */
  @Override
  public TransferResult<CFValue, CFStore> visitNumericalSubtraction(
      NumericalSubtractionNode n, TransferInput<CFValue, CFStore> in) {
    LessThanAnnotatedTypeFactory factory = (LessThanAnnotatedTypeFactory) analysis.getTypeFactory();
    JavaExpression leftJe = JavaExpression.fromNode(n.getLeftOperand());
    if (leftJe != null && leftJe.isUnassignableByOtherCode()) {
      ValueAnnotatedTypeFactory valueFactory = factory.getValueAnnotatedTypeFactory();
      Long right = ValueCheckerUtils.getMinValue(n.getRightOperand().getTree(), valueFactory);
      if (right != null && 0 < right) {
        // left - right < left iff 0 < right
        List<String> expressions = getLessThanExpressions(n.getLeftOperand());
        if (!isDoubleOrFloatLiteral(leftJe)) {
          if (expressions == null) {
            expressions = Collections.singletonList(leftJe.toString());
          } else {
            expressions = CollectionsPlume.append(expressions, leftJe.toString());
          }
        }
        AnnotationMirror refine = factory.createLessThanQualifier(expressions);
        CFValue value = analysis.createSingleAnnotationValue(refine, n.getType());
        CFStore info = in.getRegularStore();
        return new RegularTransferResult<>(finishValue(value, info), info);
      }
    }
    return super.visitNumericalSubtraction(n, in);
  }

  /**
   * Return the expressions that {@code node} is less than.
   *
   * @param node a CFG node
   * @return the expressions that {@code node} is less than
   */
  private List<String> getLessThanExpressions(Node node) {
    Set<AnnotationMirror> s = analysis.getValue(node).getAnnotations();
    if (s != null && !s.isEmpty()) {
      LessThanAnnotatedTypeFactory factory =
          (LessThanAnnotatedTypeFactory) analysis.getTypeFactory();
      return factory.getLessThanExpressions(
          factory.getQualifierHierarchy().findAnnotationInHierarchy(s, factory.LESS_THAN_UNKNOWN));
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
    expr = ValueCheckerUtils.optimize(expr, analysis.getTypeFactory());
    if (expr instanceof ValueLiteral) {
      ValueLiteral literal = (ValueLiteral) expr;
      if (literal.getValue() instanceof Number) {
        long longLiteral = ((Number) literal.getValue()).longValue();
        if (longLiteral != Long.MAX_VALUE) {
          return (longLiteral + 1) + "L";
        }
      }
    }

    // Could do more optimization to merge with a literal at end of `exprString`.  Is that needed?
    return expr + " + 1";
  }
}
