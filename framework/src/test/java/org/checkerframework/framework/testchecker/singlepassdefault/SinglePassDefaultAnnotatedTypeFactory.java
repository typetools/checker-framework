package org.checkerframework.framework.testchecker.singlepassdefault;

import java.lang.annotation.Annotation;
import java.util.Set;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.testchecker.util.SubQual;
import org.checkerframework.framework.testchecker.util.SuperQual;
import org.checkerframework.framework.util.defaults.QualifierDefaults;
import org.checkerframework.javacutil.AnnotationBuilder;

/**
 * Adds a checked code default of {@code @SuperQual} for {@link TypeUseLocation#RETURN}.
 *
 * <p>RETURN is one of the locations whose default annotates, from the top-level executable node,
 * some node other than the top-level node. A test file that also writes {@code @DefaultQualifier}
 * on a class therefore produces a precedence list in which a default that annotates nodes below the
 * top level precedes a default that annotates the return type from the top-level node, which is
 * exactly the case in which one traversal per default and one traversal for all defaults disagree.
 */
public class SinglePassDefaultAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /**
   * Creates a SinglePassDefaultAnnotatedTypeFactory.
   *
   * @param checker the checker
   */
  @SuppressWarnings("this-escape")
  public SinglePassDefaultAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    this.postInit();
  }

  @Override
  protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
    return Set.of(SuperQual.class, SubQual.class);
  }

  @Override
  protected void addCheckedCodeDefaults(QualifierDefaults defs) {
    super.addCheckedCodeDefaults(defs);
    defs.addCheckedCodeDefault(
        AnnotationBuilder.fromClass(getElementUtils(), SuperQual.class), TypeUseLocation.RETURN);
  }
}
