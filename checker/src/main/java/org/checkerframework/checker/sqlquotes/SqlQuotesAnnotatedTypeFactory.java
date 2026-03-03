package org.checkerframework.checker.sqlquotes;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.sqlquotes.qual.SqlEvenQuotes;
import org.checkerframework.checker.sqlquotes.qual.SqlOddQuotes;
import org.checkerframework.checker.sqlquotes.qual.SqlQuotesBottom;
import org.checkerframework.checker.sqlquotes.qual.SqlQuotesUnknown;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.LiteralTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.TreeUtils;

/** Annotated type factory for the SQL Quotes Checker. */
public class SqlQuotesAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /** The {@code @}{@link SqlEvenQuotes} annotation mirror. */
  private final AnnotationMirror SQL_EVEN_QUOTES;

  /** The {@code @}{@link SqlOddQuotes} annotation mirror. */
  private final AnnotationMirror SQL_ODD_QUOTES;

  /** The {@code @}{@link SqlQuotesUnknown} annotation mirror. */
  private final AnnotationMirror SQL_QUOTES_UNKNOWN;

  /** The {@code @}{@link SqlQuotesBottom} annotation mirror. */
  private final AnnotationMirror SQL_QUOTES_BOTTOM;

  /** A singleton set containing the {@code @}{@link SqlEvenQuotes} annotation mirror. */
  private final AnnotationMirrorSet setOfSqlEvenQuotes;

  /**
   * Creates a {@link SqlQuotesAnnotatedTypeFactory}.
   *
   * @param checker the SQL tainting checker
   */
  @SuppressWarnings("this-escape")
  public SqlQuotesAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    this.SQL_EVEN_QUOTES = AnnotationBuilder.fromClass(getElementUtils(), SqlEvenQuotes.class);
    this.SQL_ODD_QUOTES = AnnotationBuilder.fromClass(getElementUtils(), SqlOddQuotes.class);
    this.SQL_QUOTES_UNKNOWN =
        AnnotationBuilder.fromClass(getElementUtils(), SqlQuotesUnknown.class);
    this.SQL_QUOTES_BOTTOM = AnnotationBuilder.fromClass(getElementUtils(), SqlQuotesBottom.class);
    this.setOfSqlEvenQuotes = AnnotationMirrorSet.singleton(SQL_EVEN_QUOTES);
    postInit();
  }

  @Override
  protected Set<AnnotationMirror> getEnumConstructorQualifiers() {
    return setOfSqlEvenQuotes;
  }

  @Override
  public TreeAnnotator createTreeAnnotator() {
    // Don't call super.createTreeAnnotator() because it includes PropagationTreeAnnotator,
    // but we want to use UnitsPropagationTreeAnnotator instead.
    return new ListTreeAnnotator(
        new SqlQuotesPropagationTreeAnnotator(this),
        new LiteralTreeAnnotator(this).addStandardLiteralQualifiers(),
        new SqlQuotesAnnotatedTypeFactory.SqlQuotesTreeAnnotator(this));
  }

  /** Disables {@link #visitBinary} to avoid runaway recursion. */
  private static class SqlQuotesPropagationTreeAnnotator extends PropagationTreeAnnotator {

    /**
     * Creates a new SqlQuotesPropagationTreeAnnotator.
     *
     * @param atypeFactory the type factory
     */
    public SqlQuotesPropagationTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
      super(atypeFactory);
    }

    // Completely handled by SqlQuotesTreeAnnotator.
    @Override
    public Void visitBinary(BinaryTree tree, AnnotatedTypeMirror type) {
      return null;
    }

    // Completely handled by SqlQuotesTreeAnnotator.
    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree tree, AnnotatedTypeMirror type) {
      return null;
    }
  }

  /**
   * A TreeAnnotator to enforce SqlQuotes String concatenation rules:
   *
   * <ul>
   *   <li>(SqlOddQuotes + SqlEvenQuotes) returns SqlOddQuotes (commutatively);
   *   <li>(SqlOddQuotes + SqlOddQuotes) returns SqlEvenQuotes;
   *   <li>(SqlEvenQuotes + SqlEvenQuotes) returns SqlEvenQuotes;
   *   <li>SqlQuotesUnknown dominates other types in concatenation;
   *   <li>Non-bottom types dominate SqlQuotesBottom in concatenation.
   * </ul>
   *
   * and also to set the type of literals.
   */
  private class SqlQuotesTreeAnnotator extends TreeAnnotator {
    /**
     * Creates a {@link SqlQuotesTreeAnnotator}
     *
     * @param atypeFactory the annotated type factory
     */
    public SqlQuotesTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
      super(atypeFactory);
    }

    @Override
    public Void visitLiteral(LiteralTree tree, AnnotatedTypeMirror type) {
      if (tree.getKind() == Tree.Kind.STRING_LITERAL) {
        String string = (String) tree.getValue();
        int numQuotes = 0;
        int len = string.length();
        for (int i = 0; i < len; i++) {
          if (string.charAt(i) == '\'') {
            numQuotes++;
          }
        }
        if ((numQuotes & 1) == 0) {
          type.replaceAnnotation(SQL_EVEN_QUOTES);
        } else {
          type.replaceAnnotation(SQL_ODD_QUOTES);
        }
      }
      return null;
    }

    @Override
    public Void visitBinary(BinaryTree tree, AnnotatedTypeMirror type) {
      if (TreeUtils.isStringConcatenation(tree)) {
        AnnotatedTypeMirror leftType = getAnnotatedType(tree.getLeftOperand());
        AnnotatedTypeMirror rightType = getAnnotatedType(tree.getRightOperand());
        type.replaceAnnotation(getResultingType(leftType, rightType));
      }
      return null;
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree tree, AnnotatedTypeMirror type) {
      if (TreeUtils.isStringCompoundConcatenation(tree)) {
        AnnotatedTypeMirror leftType = getAnnotatedType(tree.getVariable());
        AnnotatedTypeMirror rightType = getAnnotatedType(tree.getExpression());
        type.replaceAnnotation(getResultingType(leftType, rightType));
      }
      return null;
    }

    /**
     * Returns the type of concatenating leftType and rightType.
     *
     * @param leftType the type on the left of the expression
     * @param rightType the type on the right of the expression
     * @return the resulting type after concatenation
     */
    private AnnotationMirror getResultingType(
        AnnotatedTypeMirror leftType, AnnotatedTypeMirror rightType) {
      if (leftType.hasPrimaryAnnotation(SQL_QUOTES_UNKNOWN)
          || rightType.hasPrimaryAnnotation(SQL_QUOTES_UNKNOWN)) {
        return SQL_QUOTES_UNKNOWN;
      }

      if (leftType.hasPrimaryAnnotation(SQL_QUOTES_BOTTOM)) {
        return rightType.getPrimaryAnnotation();
      } else if (rightType.hasPrimaryAnnotation(SQL_QUOTES_BOTTOM)) {
        return leftType.getPrimaryAnnotation();
      }

      int leftParity = 0;
      if (leftType.hasPrimaryAnnotation(SQL_ODD_QUOTES)) {
        leftParity = 1;
      }

      int rightParity = 0;
      if (rightType.hasPrimaryAnnotation(SQL_ODD_QUOTES)) {
        rightParity = 1;
      }

      int parity = leftParity + rightParity;
      if (parity == 0 || parity == 2) {
        return SQL_EVEN_QUOTES;
      } else {
        return SQL_ODD_QUOTES;
      }
    }
  }
}
