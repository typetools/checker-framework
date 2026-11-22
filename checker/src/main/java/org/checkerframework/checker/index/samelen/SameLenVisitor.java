package org.checkerframework.checker.index.samelen;

import com.sun.source.tree.ExpressionTree;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.index.IndexUtil;
import org.checkerframework.checker.index.qual.PolySameLen;
import org.checkerframework.checker.index.qual.SameLen;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

public class SameLenVisitor extends BaseTypeVisitor<SameLenAnnotatedTypeFactory> {
  public SameLenVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  /**
   * Merges SameLen annotations, then calls super.
   *
   * <p>{@inheritDoc}
   */
  @Override
  protected boolean commonAssignmentCheck(
      AnnotatedTypeMirror varType,
      ExpressionTree valueTree,
      @CompilerMessageKey String errorKey,
      Object... extraArgs) {

    if (shouldSkipUses(valueTree)) {
      return true;
    }
    TypeMirror valueTypeMirror = TreeUtils.typeOf(valueTree);
    if (IndexUtil.isSequenceType(valueTypeMirror) && TreeUtils.isExpressionTree(valueTree)) {
      AnnotatedTypeMirror valueType = atypeFactory.getAnnotatedType(valueTree);
      // if both annotations are @PolySameLen, there is nothing to do
      if (!(valueType.hasPrimaryAnnotation(PolySameLen.class)
          && varType.hasPrimaryAnnotation(PolySameLen.class))) {
        JavaExpression rhs = JavaExpression.fromTree(valueTree);
        if (rhs != null && SameLenAnnotatedTypeFactory.mayAppearInSameLen(rhs)) {
          String rhsExpr = rhs.toString();
          AnnotationMirror sameLenAnno = valueType.getPrimaryAnnotation(SameLen.class);
          Collection<String> exprs;
          if (sameLenAnno == null) {
            exprs = Collections.singletonList(rhsExpr);
          } else {
            exprs =
                new TreeSet<>(
                    AnnotationUtils.getElementValueArray(
                        sameLenAnno, atypeFactory.sameLenValueElement, String.class));
            exprs.add(rhsExpr);
          }
          AnnotationMirror newSameLen = atypeFactory.createSameLen(exprs);
          valueType.replaceAnnotation(newSameLen);
          return super.commonAssignmentCheck(varType, valueType, valueTree, errorKey, extraArgs);
        }
      }
    }
    return super.commonAssignmentCheck(varType, valueTree, errorKey, extraArgs);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Also permits the reassignment if each sequence {@code r} in the {@code @SameLen} annotation
   * of {@code requiredType} is {@code variable} itself, is in the {@code @SameLen} annotation of
   * {@code currentType}, or has the same length as one of those sequences before the reassignment.
   * For example, if {@code b} is {@code @SameLen("a")} and {@code c} is {@code @SameLen("a")}, then
   * after {@code a = c}, {@code b} is still {@code @SameLen("a")}.
   */
  @Override
  protected boolean isValidAfterReassignment(
      VariableElement variable,
      AnnotatedTypeMirror currentType,
      AnnotatedTypeMirror requiredType,
      ExpressionTree reassignmentTree) {
    if (super.isValidAfterReassignment(variable, currentType, requiredType, reassignmentTree)) {
      return true;
    }
    AnnotationMirror requiredAnno = requiredType.getPrimaryAnnotation(SameLen.class);
    if (requiredAnno == null) {
      return false;
    }
    TreeSet<String> sameLens = new TreeSet<>();
    sameLens.add(variable.getSimpleName().toString());
    AnnotationMirror currentAnno = currentType.getPrimaryAnnotation(SameLen.class);
    if (currentAnno != null) {
      sameLens.addAll(
          AnnotationUtils.getElementValueArray(
              currentAnno, atypeFactory.sameLenValueElement, String.class));
    }
    for (String required :
        AnnotationUtils.getElementValueArray(
            requiredAnno, atypeFactory.sameLenValueElement, String.class)) {
      if (sameLens.contains(required)) {
        continue;
      }
      List<String> requiredSameLens =
          atypeFactory.getSameLensFromString(required, reassignmentTree, getCurrentPath());
      if (Collections.disjoint(requiredSameLens, sameLens)) {
        return false;
      }
    }
    return true;
  }
}
