package org.checkerframework.checker.index.samelen;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
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
                  Arrays.asList(targetRec, otherRec), Arrays.asList(UNKNOWN, lengthNodeAnnotation));

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

      propagateCombinedSameLen(combinedSameLen, node, result.getRegularStore());
    }

    return result;
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
    TreePath currentPath = atypeFactory.getPath(left.getTree());
    if (currentPath == null) {
      // propagateCombinedSameLen would do nothing.
      return;
    }
    List<JavaExpression> exprs = new ArrayList<>(2);
    List<AnnotationMirror> annos = new ArrayList<>(2);
    addOperandSameLens(left, currentPath, exprs, annos);
    addOperandSameLens(right, currentPath, exprs, annos);

    AnnotationMirror combinedSameLen = atypeFactory.createCombinedSameLen(exprs, annos);

    propagateCombinedSameLen(combinedSameLen, left, store);
  }

  /**
   * Adds to {@code exprs} the expressions that have the value of {@code operand} when the
   * comparison completes, and adds their SameLen annotations to {@code annos}.
   *
   * <p>Each annotation was computed when its expression was evaluated. It omits each name whose
   * expression {@code operand} or the rest of the comparison might change, because such a name
   * might not have the same length when the comparison completes. For example, in {@code a.length
   * == f(c = new int[0], b).length}, if the type of {@code a} is {@code @SameLen("c")}, the
   * annotation that this method adds for {@code a} omits {@code c}.
   *
   * @param operand an operand of a comparison, possibly an assignment
   * @param currentPath the path at which to parse the expressions in SameLen annotations
   * @param exprs the list of expressions to add to
   * @param annos the list of annotations to add to, in parallel with {@code exprs}
   */
  private void addOperandSameLens(
      Node operand,
      TreePath currentPath,
      List<JavaExpression> exprs,
      List<AnnotationMirror> annos) {
    // Computed lazily, because computing it can be costly and most annotations name nothing.
    Predicate<JavaExpression> mayBeChanged = null;
    for (Node internal : nodesWithValueOf(operand)) {
      exprs.add(JavaExpression.fromNode(internal));
      AnnotationMirror anno = getAnno(internal);
      if (anno != null && atypeFactory.areSameByClass(anno, SameLen.class)) {
        List<String> names =
            AnnotationUtils.getElementValueArray(
                anno, atypeFactory.sameLenValueElement, String.class);
        if (!names.isEmpty()) {
          if (mayBeChanged == null) {
            mayBeChanged = mayBeChangedByOrAfter(operand);
          }
          List<String> unchangedNames = new ArrayList<>(names.size());
          for (String name : names) {
            JavaExpression je;
            try {
              je = atypeFactory.parseJavaExpressionString(name, currentPath);
            } catch (JavaExpressionParseException e) {
              // The name cannot be checked, and propagateCombinedSameLen would skip it anyway.
              continue;
            }
            if (!mayBeChanged.test(je)) {
              unchangedNames.add(name);
            }
          }
          if (unchangedNames.size() != names.size()) {
            // `unchangedNames` is a sublist of `names`, so it is in alphabetical order.
            anno = atypeFactory.createSameLen(unchangedNames);
          }
        }
      }
      annos.add(anno);
    }
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
