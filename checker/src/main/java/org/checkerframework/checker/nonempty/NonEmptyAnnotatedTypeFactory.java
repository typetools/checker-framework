package org.checkerframework.checker.nonempty;

import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.nonempty.qual.NonEmpty;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.AnnotationBuilder;

public class NonEmptyAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /** The @{@link NonEmpty} annotation. */
  public final AnnotationMirror NON_EMPTY = AnnotationBuilder.fromClass(elements, NonEmpty.class);

  /**
   * Creates a new {@link NonEmptyAnnotatedTypeFactory} that operates on a particular AST.
   *
   * @param checker the checker to use
   */
  public NonEmptyAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    this.sideEffectsUnrefineAliases = true;
    this.postInit();
  }

  @Override
  public CFTransfer createFlowTransferFunction(
      CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
    return new NonEmptyTransfer(analysis);
  }
}
