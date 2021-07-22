package org.checkerframework.framework.flow;

import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;

/** The default org.checkerframework.dataflow analysis used in the Checker Framework. */
public class CFAnalysis extends CFAbstractAnalysis<CFValue, CFStore, CFTransfer> {

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
  public CFValue createAbstractValue(Set<AnnotationMirror> annotations, TypeMirror underlyingType) {
    return defaultCreateAbstractValue(this, annotations, underlyingType);
  }
}
