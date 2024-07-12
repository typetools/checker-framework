package org.checkerframework.checker.sqlquerytainting;

import com.sun.source.tree.BinaryTree;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.sqlquerytainting.qual.SqlEvenQuotes;
import org.checkerframework.checker.sqlquerytainting.qual.SqlOddQuotes;
import org.checkerframework.checker.sqlquerytainting.qual.SqlQueryUnknown;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.TreeUtils;

/** Annotated type factory for the SQL Query Tainting Checker. */
public class SqlQueryTaintingAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /** The {@code @}{@link SqlEvenQuotes} annotation mirror. */
  private final AnnotationMirror SQLEVENQUOTES;

  /** The {@code @}{@link SqlOddQuotes} annotation mirror. */
  private final AnnotationMirror SQLODDQUOTES;

  /** The {@code @}{@link SqlQueryUnknown} annotation mirror. */
  private final AnnotationMirror SQLQUERYUNKNOWN;

  /** A singleton set containing the {@code @}{@link SqlEvenQuotes} annotation mirror. */
  private final AnnotationMirrorSet setOfSqlEvenQuotes;

  /**
   * Creates a {@link SqlQueryTaintingAnnotatedTypeFactory}.
   *
   * @param checker the SQL tainting checker
   */
  public SqlQueryTaintingAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    this.SQLEVENQUOTES = AnnotationBuilder.fromClass(getElementUtils(), SqlEvenQuotes.class);
    this.SQLODDQUOTES = AnnotationBuilder.fromClass(getElementUtils(), SqlOddQuotes.class);
    this.SQLQUERYUNKNOWN = AnnotationBuilder.fromClass(getElementUtils(), SqlQueryUnknown.class);
    this.setOfSqlEvenQuotes = AnnotationMirrorSet.singleton(SQLEVENQUOTES);
    postInit();
  }

  @Override
  protected Set<AnnotationMirror> getEnumConstructorQualifiers() {
    return setOfSqlEvenQuotes;
  }

  private class SqlQueryTaintingTreeAnnotator extends TreeAnnotator {
    public SqlQueryTaintingTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
      super(atypeFactory);
    }

    @Override
    public Void visitBinary(BinaryTree tree, AnnotatedTypeMirror type) {
      if (!type.hasPrimaryAnnotationInHierarchy(SQLQUERYUNKNOWN)
          && TreeUtils.isStringConcatenation(tree)) {
        AnnotatedTypeMirror leftType = getAnnotatedType(tree.getLeftOperand());
        AnnotatedTypeMirror rightType = getAnnotatedType(tree.getRightOperand());

        if (leftType.hasPrimaryAnnotation(SQLQUERYUNKNOWN)
            || rightType.hasPrimaryAnnotation(SQLQUERYUNKNOWN)) {
          type.addAnnotation(SQLQUERYUNKNOWN);
          return null;
        }

        int leftParity = 0;
        if (leftType.hasPrimaryAnnotation(SQLODDQUOTES)) {
          leftParity = 1;
        }

        int rightParity = 0;
        if (rightType.hasPrimaryAnnotation(SQLODDQUOTES)) {
          rightParity = 1;
        }

        if (leftParity + rightParity % 2 == 0) {
          type.addAnnotation(SQLEVENQUOTES);
        } else {
          type.addAnnotation(SQLODDQUOTES);
        }
      }

      return null;
    }
  }
}
