package org.checkerframework.checker.lock;

import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;

/**
 * The analysis class for the lock type system.
 *
 * <p>This class extends {@link CFAbstractAnalysis} so that {@link LockStore} is used rather than
 * {@link CFStore}.
 */
public class LockAnalysis extends CFAbstractAnalysis<CFValue, LockStore, LockTransfer> {

  /**
   * Creates {@link LockAnalysis}
   *
   * @param checker the checker
   * @param factory the factory
   */
  public LockAnalysis(BaseTypeChecker checker, LockAnnotatedTypeFactory factory) {
    super(checker, factory);
  }

  @Override
  public LockTransfer createTransferFunction() {
    return new LockTransfer(this, (LockChecker) checker);
  }

  @Override
  public LockStore createEmptyStore(boolean sequentialSemantics) {
    return new LockStore(this, sequentialSemantics);
  }

  @Override
  public LockStore createCopiedStore(LockStore s) {
    return new LockStore(this, s);
  }

  @Override
  public CFValue createAbstractValue(Set<AnnotationMirror> annotations, TypeMirror underlyingType) {
    return defaultCreateAbstractValue(this, annotations, underlyingType);
  }
}
