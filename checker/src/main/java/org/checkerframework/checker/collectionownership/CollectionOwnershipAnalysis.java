package org.checkerframework.checker.collectionownership;

import javax.lang.model.type.TypeMirror;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.AnnotationMirrorSet;

/**
 * The analysis class for the collection ownership type system.
 *
 * <p>This class extends {@link CFAbstractAnalysis} so that {@link CollectionOwnershipStore} is used
 * rather than {@link CFStore}.
 */
public class CollectionOwnershipAnalysis
    extends CFAbstractAnalysis<CFValue, CollectionOwnershipStore, CollectionOwnershipTransfer> {

  /**
   * Creates a new {@link CollectionOwnershipAnalysis}.
   *
   * @param checker the checker
   * @param factory the factory
   */
  public CollectionOwnershipAnalysis(
      BaseTypeChecker checker, CollectionOwnershipAnnotatedTypeFactory factory) {
    super(checker, factory);
  }

  @Override
  public CollectionOwnershipTransfer createTransferFunction() {
    return new CollectionOwnershipTransfer(this, (CollectionOwnershipChecker) checker);
  }

  @Override
  public CollectionOwnershipStore createEmptyStore(boolean sequentialSemantics) {
    return new CollectionOwnershipStore(this, sequentialSemantics);
  }

  @Override
  public CollectionOwnershipStore createCopiedStore(CollectionOwnershipStore s) {
    return new CollectionOwnershipStore(this, s);
  }

  @Override
  public CFValue createAbstractValue(AnnotationMirrorSet annotations, TypeMirror underlyingType) {
    return getCfValue(this, annotations, underlyingType);
  }
}
