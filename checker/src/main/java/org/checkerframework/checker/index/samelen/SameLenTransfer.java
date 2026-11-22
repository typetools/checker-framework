package org.checkerframework.checker.index.samelen;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.index.IndexUtil;
import org.checkerframework.checker.index.qual.SameLen;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.node.ArrayCreationNode;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.expression.JavaExpressionParseException;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;

/**
 * The transfer function for the SameLen checker. Contains three cases:
 *
 * <ul>
 *   <li>new array: "b = new T[a.length]" implies that b is the same length as a.
 *   <li>length equality: after "if (a.length == b.length)", a and b have the same length.
 *   <li>object equality: after "if (a == b)", a and b have the same length, if they are arrays or
 *       strings.
 * </ul>
 */
public class SameLenTransfer extends CFTransfer {

  /** The annotated type factory. */
  private final SameLenAnnotatedTypeFactory atypeFactory;

  /** Shorthand for atypeFactory.UNKNOWN. */
  private final AnnotationMirror UNKNOWN;

  /**
   * Create a new SameLenTransfer.
   *
   * @param analysis the CFAnalysis
   */
  public SameLenTransfer(CFAnalysis analysis) {
    super(analysis);
    this.atypeFactory = (SameLenAnnotatedTypeFactory) analysis.getTypeFactory();
    this.UNKNOWN = atypeFactory.UNKNOWN;
  }

  /**
   * Gets the receiver sequence of a length access node, or null if {@code lengthNode} is not a
   * length access.
   */
  private @Nullable Node getLengthReceiver(Node lengthNode) {
    if (isArrayLengthAccess(lengthNode)) {
      // lengthNode is a.length
      FieldAccessNode lengthFieldAccessNode = (FieldAccessNode) lengthNode;
      return lengthFieldAccessNode.getReceiver();
    } else if (atypeFactory.getMethodIdentifier().isLengthOfMethodInvocation(lengthNode)) {
      // lengthNode is s.length()
      MethodInvocationNode lengthMethodInvocationNode = (MethodInvocationNode) lengthNode;
      return lengthMethodInvocationNode.getTarget().getReceiver();
    } else {
      return null;
    }
  }

  @Override
  public TransferResult<CFValue, CFStore> visitAssignment(
      AssignmentNode node, TransferInput<CFValue, CFStore> in) {
    TransferResult<CFValue, CFStore> result = super.visitAssignment(node, in);

    Node exprNode = node.getExpression();
    Node recNode = node.getTarget();
    // If true, then after the assignment the target denotes a different location than the one that
    // was assigned, as in `n.values = id(n = other, a)`.  The base transfer function has removed
    // the target from the store, and the combined @SameLen annotation must not mention it.
    boolean rhsMayRelocateTarget = rhsMayRelocateTarget(node);

    // Handle b = new T[a.length]
    if (exprNode instanceof ArrayCreationNode acNode) {
      if (acNode.getDimensions().size() == 1) {

        Node lengthNode = acNode.getDimension(0);
        Node lengthNodeReceiver = getLengthReceiver(lengthNode);

        if (lengthNodeReceiver != null) {
          // "new T[a.length]" or "new T[s.length()]" is the right-hand side of the
          // assignment.
          // lengthNode is known to be "lengthNodeReceiver.length" or
          // "lengthNodeReceiver.length()"

          // targetRec is the receiver for the left-hand side of the assignment.
          JavaExpression targetRec = JavaExpression.fromNode(recNode);
          JavaExpression otherRec = JavaExpression.fromNode(lengthNodeReceiver);

          AnnotationMirror lengthNodeAnnotation =
              atypeFactory
                  .getAnnotatedType(lengthNodeReceiver.getTree())
                  .getPrimaryAnnotationInHierarchy(UNKNOWN);

          AnnotationMirror combinedSameLen =
              atypeFactory.createCombinedSameLen(
                  Arrays.asList(targetRec, otherRec),
                  Arrays.asList(
                      UNKNOWN, lengthNodeAnnotation == null ? UNKNOWN : lengthNodeAnnotation));
          if (rhsMayRelocateTarget) {
            combinedSameLen = removeFromSameLen(combinedSameLen, targetRec);
          }

          propagateCombinedSameLen(combinedSameLen, node, result.getRegularStore());
          return result;
        }
      }
    }

    AnnotationMirror rightAnno =
        atypeFactory.getAnnotatedType(exprNode.getTree()).getPrimaryAnnotationInHierarchy(UNKNOWN);

    // If the left side of the assignment is an array or a string, then have both the right and
    // left side be SameLen of each other.

    JavaExpression targetRec = JavaExpression.fromNode(recNode);

    JavaExpression exprRec = JavaExpression.fromNode(exprNode);

    if (IndexUtil.isSequenceType(recNode.getType())
        || (rightAnno != null && atypeFactory.areSameByClass(rightAnno, SameLen.class))) {

      AnnotationMirror rightAnnoOrUnknown = rightAnno == null ? UNKNOWN : rightAnno;

      AnnotationMirror combinedSameLen =
          atypeFactory.createCombinedSameLen(
              Arrays.asList(targetRec, exprRec), Arrays.asList(UNKNOWN, rightAnnoOrUnknown));
      if (rhsMayRelocateTarget) {
        combinedSameLen = removeFromSameLen(combinedSameLen, targetRec);
      }

      propagateCombinedSameLen(combinedSameLen, node, result.getRegularStore());
    }

    return result;
  }

  /**
   * Returns a copy of the given {@code @SameLen} annotation that does not contain the given
   * expression.
   *
   * @param sameLenAnno a {@code @SameLen} annotation
   * @param expr an expression to remove from {@code sameLenAnno}
   * @return a copy of {@code sameLenAnno} that does not contain {@code expr}
   */
  private AnnotationMirror removeFromSameLen(AnnotationMirror sameLenAnno, JavaExpression expr) {
    List<String> values =
        new ArrayList<>(
            AnnotationUtils.getElementValueArray(
                sameLenAnno, atypeFactory.sameLenValueElement, String.class));
    if (!values.remove(expr.toString())) {
      return sameLenAnno;
    }
    // Removing an element preserves the alphabetical order that createSameLen requires.
    return atypeFactory.createSameLen(values);
  }

  /**
   * Insert a @SameLen annotation into the store as the SameLen type of each array listed in it.
   *
   * @param sameLenAnno a {@code @SameLen} annotation. Not just an annotation in the SameLen
   *     hierarchy; this annotation must be {@code @SameLen(...)}.
   * @param node the node in the tree where the combination is happening. Used for context.
   * @param store the store to modify
   */
  private void propagateCombinedSameLen(AnnotationMirror sameLenAnno, Node node, CFStore store) {
    TreePath currentPath = atypeFactory.getPath(node.getTree());
    if (currentPath == null) {
      return;
    }
    for (String exprString :
        AnnotationUtils.getElementValueArray(
            sameLenAnno, atypeFactory.sameLenValueElement, String.class)) {
      JavaExpression je;
      try {
        je = atypeFactory.parseJavaExpressionString(exprString, currentPath);
      } catch (JavaExpressionParseException e) {
        // Do not report the error here; that would duplicate the one that DependentTypesHelper
        // issues for the `@SameLen` annotation, whose `value` element is a `@JavaExpression`.
        // Skipping the expression only loses precision.
        continue;
      }
      store.clearValue(je);
      store.insertValue(je, sameLenAnno);
    }
  }

  /** Returns true if node is of the form "someArray.length". */
  private boolean isArrayLengthAccess(Node node) {
    return (node instanceof FieldAccessNode fan
        && fan.getFieldName().equals("length")
        && fan.getReceiver().getType().getKind() == TypeKind.ARRAY);
  }

  /**
   * Handles refinement of equality comparisons. Assumes "a == b" or "a.length == b.length"
   * evaluates to true. The method gives a and b SameLen of each other in the store.
   *
   * @param left the first argument to the equality operator
   * @param right the second argument to the equality operator
   * @param store the store in which to perform refinement
   */
  private void refineEq(Node left, Node right, CFStore store) {
    List<JavaExpression> exprs = new ArrayList<>(2);
    List<AnnotationMirror> annos = new ArrayList<>(2);
    for (Node internal : nodesWithValueOf(left)) {
      exprs.add(JavaExpression.fromNode(internal));
      annos.add(annoWhenComparisonCompletes(internal));
    }
    for (Node internal : nodesWithValueOf(right)) {
      exprs.add(JavaExpression.fromNode(internal));
      annos.add(annoWhenComparisonCompletes(internal));
    }

    AnnotationMirror combinedSameLen = atypeFactory.createCombinedSameLen(exprs, annos);

    propagateCombinedSameLen(combinedSameLen, left, store);
  }

  /**
   * Returns {@code n}'s annotation from the SameLen hierarchy (see {@link #getAnno}), without the
   * expressions whose values might change after {@code n} is evaluated and before the comparison
   * that contains {@code n} completes.
   *
   * <p>The annotation of {@code n} relates {@code n} to the values that the expressions in the
   * annotation had when {@code n} was evaluated. For example, in {@code a.length == ((b = new
   * int[0]) == null ? c : c).length}, the type {@code @SameLen("b")} of {@code a} is about the array
   * that {@code b} referred to before the right operand reassigned {@code b}, so the comparison
   * does not imply that {@code b} has the same length as {@code c}.
   *
   * @param n a node in an operand of a comparison
   * @return {@code n}'s annotation from the SameLen hierarchy, without the expressions that might
   *     change before the comparison completes
   */
  private AnnotationMirror annoWhenComparisonCompletes(Node n) {
    AnnotationMirror anno = getAnno(n);
    if (anno == null
        || !atypeFactory.areSameByClass(anno, SameLen.class)
        || !containsExpressionChangedBeforeComparisonCompletes(anno, n)) {
      return anno;
    }
    List<String> kept = new ArrayList<>();
    for (String expr :
        AnnotationUtils.getElementValueArray(
            anno, atypeFactory.sameLenValueElement, String.class)) {
      if (!containsExpressionChangedBeforeComparisonCompletes(
          atypeFactory.createSameLen(Collections.singletonList(expr)), n)) {
        kept.add(expr);
      }
    }
    // Removing elements preserves the alphabetical order that createSameLen requires.
    return kept.isEmpty() ? UNKNOWN : atypeFactory.createSameLen(kept);
  }

  /**
   * Returns {@code n}'s annotation from the SameLen hierarchy.
   *
   * <p>analysis.getValue fails if called on an lvalue. However, this method needs to always
   * succeed, even when n is an lvalue. Consider this code:
   *
   * <pre>{@code if ((a = b) == c) {...}}</pre>
   *
   * where a, b, and c are all arrays, and a has type {@code @SameLen("d")}. Afterwards, all three
   * should have the type {@code @SameLen({"a", "b", "c", "d"})}, but in order to accomplish this,
   * this method must return the type of a, which is an lvalue.
   *
   * @param n a node whose SameLen annotation to return
   * @return {@code n}'s annotation from the SameLen hierarchy
   */
  AnnotationMirror getAnno(Node n) {
    if (n.isLValue()) {
      return atypeFactory.getAnnotatedType(n.getTree()).getPrimaryAnnotationInHierarchy(UNKNOWN);
    }
    CFValue cfValue = analysis.getValue(n);
    if (cfValue == null) {
      return UNKNOWN;
    }
    return atypeFactory
        .getQualifierHierarchy()
        .findAnnotationInHierarchy(cfValue.getAnnotations(), UNKNOWN);
  }

  /** Implements the transfer rules for both equal nodes and not-equals nodes. */
  @Override
  protected TransferResult<CFValue, CFStore> strengthenAnnotationOfEqualTo(
      TransferResult<CFValue, CFStore> result,
      Node firstNode,
      Node secondNode,
      CFValue firstValue,
      CFValue secondValue,
      boolean notEqualTo) {
    // If result is a Regular transfer, then the elseStore is a copy of the then store, that is
    // created when getElseStore is called.  So do that before refining any values.
    CFStore elseStore = result.getElseStore();
    CFStore thenStore = result.getThenStore();
    CFStore equalStore = notEqualTo ? elseStore : thenStore;

    Node firstLengthReceiver = getLengthReceiver(firstNode);
    Node secondLengthReceiver = getLengthReceiver(secondNode);

    if (firstLengthReceiver != null && secondLengthReceiver != null) {
      // Refinement in the else store if this is a.length == b.length (or length() in case of
      // strings).
      refineEq(firstLengthReceiver, secondLengthReceiver, equalStore);
    } else if (IndexUtil.isSequenceType(firstNode.getType())
        || IndexUtil.isSequenceType(secondNode.getType())) {
      refineEq(firstNode, secondNode, equalStore);
    }

    return new ConditionalTransferResult<>(result.getResultValue(), thenStore, elseStore);
  }

  /** Overridden to ensure that SameLen annotations on method parameters are symmetric. */
  @Override
  protected void addInformationFromPreconditions(
      CFStore info,
      AnnotatedTypeFactory factory,
      UnderlyingAST.CFGMethod method,
      MethodTree methodTree,
      ExecutableElement methodElement) {
    super.addInformationFromPreconditions(info, factory, method, methodTree, methodElement);

    List<? extends VariableTree> paramTrees = methodTree.getParameters();
    int numParams = paramTrees.size();
    List<String> paramNames = new ArrayList<>(numParams);
    List<AnnotatedTypeMirror> params = new ArrayList<>(numParams);

    for (VariableTree tree : paramTrees) {
      paramNames.add(tree.getName().toString());
      params.add(atypeFactory.getAnnotatedType(tree));
    }

    for (int index = 0; index < numParams; index++) {

      // If the parameter has a samelen annotation, then look for other parameters in that
      // annotation and propagate default the other annotation so that it is symmetric.
      AnnotatedTypeMirror atm = params.get(index);
      AnnotationMirror sameLenAnno = atm.getPrimaryAnnotation(SameLen.class);
      if (sameLenAnno == null) {
        continue;
      }

      List<String> values =
          AnnotationUtils.getElementValueArray(
              sameLenAnno, atypeFactory.sameLenValueElement, String.class);
      for (String value : values) {
        int otherParamIndex = paramNames.indexOf(value);
        if (otherParamIndex == -1) {
          continue;
        }

        // the SameLen value is in the list of params, so modify the type of
        // that param in the store
        AnnotationMirror newSameLen =
            atypeFactory.createSameLen(Collections.singletonList(paramNames.get(index)));
        JavaExpression otherParamRec =
            JavaExpression.fromVariableTree(paramTrees.get(otherParamIndex));
        info.insertValuePermitNondeterministic(otherParamRec, newSameLen);
      }
    }
  }
}
