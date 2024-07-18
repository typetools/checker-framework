package org.checkerframework.checker.sqlquerytainting;

import com.sun.source.tree.BinaryTree;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.sqlquerytainting.qual.SqlEvenQuotes;
import org.checkerframework.checker.sqlquerytainting.qual.SqlOddQuotes;
import org.checkerframework.checker.sqlquerytainting.qual.SqlQuotesUnknown;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.TreeUtils;

/** Annotated type factory for the SQL Query Tainting Checker. */
public class SqlQueryTaintingAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /** The {@code @}{@link SqlEvenQuotes} annotation mirror. */
  private final AnnotationMirror SQL_EVEN_QUOTES;

  /** The {@code @}{@link SqlOddQuotes} annotation mirror. */
  private final AnnotationMirror SQL_ODD_QUOTES;

  /** The {@code @}{@link SqlQuotesUnknown} annotation mirror. */
  private final AnnotationMirror SQL_QUOTES_UNKNOWN;

  /** A singleton set containing the {@code @}{@link SqlEvenQuotes} annotation mirror. */
  private final AnnotationMirrorSet setOfSqlEvenQuotes;

  /**
   * Creates a {@link SqlQueryTaintingAnnotatedTypeFactory}.
   *
   * @param checker the SQL tainting checker
   */
  public SqlQueryTaintingAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    this.SQL_EVEN_QUOTES = AnnotationBuilder.fromClass(getElementUtils(), SqlEvenQuotes.class);
    this.SQL_ODD_QUOTES = AnnotationBuilder.fromClass(getElementUtils(), SqlOddQuotes.class);
    this.SQL_QUOTES_UNKNOWN =
        AnnotationBuilder.fromClass(getElementUtils(), SqlQuotesUnknown.class);
    this.setOfSqlEvenQuotes = AnnotationMirrorSet.singleton(SQL_EVEN_QUOTES);
    postInit();
  }

  @Override
  protected Set<AnnotationMirror> getEnumConstructorQualifiers() {
    return setOfSqlEvenQuotes;
  }

  @Override
  public TreeAnnotator createTreeAnnotator() {
    return new ListTreeAnnotator(
        super.createTreeAnnotator(),
        new SqlQueryTaintingAnnotatedTypeFactory.SqlQueryTaintingTreeAnnotator(this));
  }

  private class SqlQueryTaintingTreeAnnotator extends TreeAnnotator {
    public SqlQueryTaintingTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
      super(atypeFactory);
    }

    @Override
    public Void visitBinary(BinaryTree tree, AnnotatedTypeMirror type) {
      if (TreeUtils.isStringConcatenation(tree)) {
        AnnotatedTypeMirror leftType = getAnnotatedType(tree.getLeftOperand());
        AnnotatedTypeMirror rightType = getAnnotatedType(tree.getRightOperand());

        if (leftType.hasPrimaryAnnotation(SQL_QUOTES_UNKNOWN)
            || rightType.hasPrimaryAnnotation(SQL_QUOTES_UNKNOWN)) {
          type.replaceAnnotation(SQL_QUOTES_UNKNOWN);
          return null;
        }

        int leftParity = 0;
        if (leftType.hasPrimaryAnnotation(SQL_ODD_QUOTES)) {
          leftParity = 1;
        }

        int rightParity = 0;
        if (rightType.hasPrimaryAnnotation(SQL_ODD_QUOTES)) {
          rightParity = 1;
        }

        if ((leftParity + rightParity) % 2 == 0) {
          type.replaceAnnotation(SQL_EVEN_QUOTES);
        } else {
          type.replaceAnnotation(SQL_ODD_QUOTES);
        }
      }

      return null;
    }
  }
}
