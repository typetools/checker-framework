package org.checkerframework.framework.flow;

import javax.lang.model.type.TypeMirror;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.javacutil.AnnotationMirrorSet;

/** The default org.checkerframework.dataflow analysis used in the Checker Framework. */
public class CFAnalysis extends CFAbstractAnalysis<CFValue, CFStore, CFTransfer> {

  /**
   * Creates a new {@code CFAnalysis}.
   *
   * @param checker the checker
   * @param factory the factory
   */
  public CFAnalysis(
      BaseTypeChecker checker,
      GenericAnnotatedTypeFactory<CFValue, CFStore, CFTransfer, CFAnalysis> factory) {
    super(checker, factory);
  }

  @Override
  public CFStore createEmptyStore(boolean sequentialSemantics) {
    return new CFStore(this, sequentialSemantics);
  }

  @Override
  public CFStore createCopiedStore(CFStore s) {
    return new CFStore(s);
  }

  @Override
  public CFValue createAbstractValue(AnnotationMirrorSet annotations, TypeMirror underlyingType) {
    return getCfValue(this, annotations, underlyingType);
  }
}
