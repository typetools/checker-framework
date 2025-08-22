package org.checkerframework.checker.index.lowerbound;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.Tree;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.index.Subsequence;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.dataflow.expression.JavaExpressionParseException;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

/**
 * Implements the actual checks to make sure that array accesses aren't too low. Will issue a
 * warning if a variable that can't be proved to be either "NonNegative" (i.e. &ge; 0) or "Positive"
 * (i.e. &ge; 1) is used as an array index.
 */
public class LowerBoundVisitor extends BaseTypeVisitor<LowerBoundAnnotatedTypeFactory> {

  /* This is a key into the messages.properties file in the same
   * directory, which includes the actual text of the warning.
   */
  private static final @CompilerMessageKey String LOWER_BOUND = "array.access.unsafe.low";
  private static final @CompilerMessageKey String NEGATIVE_ARRAY = "array.length.negative";
  private static final @CompilerMessageKey String FROM_NOT_NN = "from.not.nonnegative";

  public LowerBoundVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  @Override
  public Void visitArrayAccess(ArrayAccessTree tree, Void type) {
    ExpressionTree index = tree.getIndex();
    String arrName = tree.getExpression().toString();
    AnnotatedTypeMirror indexType = atypeFactory.getAnnotatedType(index);
    if (!(indexType.hasPrimaryAnnotation(NonNegative.class)
        || indexType.hasPrimaryAnnotation(Positive.class))) {
      checker.reportError(index, LOWER_BOUND, indexType.toString(), arrName);
    }

    return super.visitArrayAccess(tree, type);
  }

  @Override
  public Void visitNewArray(NewArrayTree tree, Void type) {
    if (!tree.getDimensions().isEmpty()) {
      for (ExpressionTree dim : tree.getDimensions()) {
        AnnotatedTypeMirror dimType = atypeFactory.getAnnotatedType(dim);
        if (!(dimType.hasPrimaryAnnotation(NonNegative.class)
            || dimType.hasPrimaryAnnotation(Positive.class))) {
          checker.reportError(dim, NEGATIVE_ARRAY, dimType.toString());
        }
      }
    }

    return super.visitNewArray(tree, type);
  }

  @Override
  protected boolean commonAssignmentCheck(
      Tree varTree,
      ExpressionTree valueTree,
      @CompilerMessageKey String errorKey,
      Object... extraArgs) {

    // check that when an assignment to a variable declared as @HasSubsequence(a, from, to)
    // occurs, from is non-negative.

    boolean result = true;

    Subsequence subSeq = Subsequence.getSubsequenceFromTree(varTree, atypeFactory);
    if (subSeq != null) {
      AnnotationMirror anm;
      try {
        anm =
            atypeFactory.getAnnotationMirrorFromJavaExpressionString(
                subSeq.from, varTree, getCurrentPath());
      } catch (JavaExpressionParseException e) {
        anm = null;
      }
      if (anm == null
          || !(atypeFactory.areSameByClass(anm, NonNegative.class)
              || atypeFactory.areSameByClass(anm, Positive.class))) {
        checker.reportError(
            valueTree, FROM_NOT_NN, subSeq.from, anm == null ? "@LowerBoundUnknown" : anm);
        result = false;
      }
    }

    result = super.commonAssignmentCheck(varTree, valueTree, errorKey, extraArgs) && result;
    return result;
  }
}
