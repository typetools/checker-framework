package org.checkerframework.checker.mustcall;

import com.sun.source.tree.Tree;
import javax.lang.model.element.Element;
import org.checkerframework.checker.mustcall.qual.MustCallAlias;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeValidator;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * This type validator is identical to BaseTypeValidator, except that it always permits the use of
 * {@link MustCallAlias} annotations on type uses, because these will be validated by the Object
 * Construction Checker's -AcheckMustCall algorithm.
 */
public class MustCallTypeValidator extends BaseTypeValidator {
  /**
   * Create a MustCallTypeValidator by calling the super constructor.
   *
   * @param checker the checker
   * @param visitor the visitor
   * @param atypeFactory the type factory
   */
  public MustCallTypeValidator(
      BaseTypeChecker checker, BaseTypeVisitor<?> visitor, AnnotatedTypeFactory atypeFactory) {
    super(checker, visitor, atypeFactory);
  }

  @Override
  protected void reportInvalidAnnotationsOnUse(AnnotatedTypeMirror type, Tree p) {
    Element elt = TreeUtils.elementFromTree(p);
    if (AnnotationUtils.containsSameByClass(elt.getAnnotationMirrors(), MustCallAlias.class)) {
      return;
    }
    super.reportInvalidAnnotationsOnUse(type, p);
  }
}
