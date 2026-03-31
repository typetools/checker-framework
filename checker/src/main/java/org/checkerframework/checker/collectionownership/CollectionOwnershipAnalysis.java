package org.checkerframework.checker.collectionownership;

import com.google.common.collect.ImmutableSet;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.resourceleak.ResourceLeakChecker;
import org.checkerframework.checker.resourceleak.ResourceLeakUtils;
import org.checkerframework.checker.resourceleak.SetOfTypes;
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
   * The resource-leak ignored-exception policy, except that RLCC does not ignore {@link Throwable}.
   * Broad {@code Throwable}-only exceptional paths affect collection-ownership flow in ways that
   * matter for RLCC, so this analysis treats them as real exceptional control flow.
   */
  private final SetOfTypes ignoredExceptions;

  /**
   * Creates a new {@link CollectionOwnershipAnalysis}.
   *
   * @param checker the checker
   * @param factory the factory
   */
  public CollectionOwnershipAnalysis(
      BaseTypeChecker checker, CollectionOwnershipAnnotatedTypeFactory factory) {
    super(checker, factory);
    ResourceLeakChecker resourceLeakChecker = ResourceLeakUtils.getResourceLeakChecker(checker);
    SetOfTypes baseIgnoredExceptions = resourceLeakChecker.getIgnoredExceptions();
    SetOfTypes exactThrowable =
        SetOfTypes.anyOfTheseNames(ImmutableSet.of(Throwable.class.getCanonicalName()));
    ignoredExceptions =
        (types, exceptionType) ->
            !exactThrowable.contains(types, exceptionType)
                && baseIgnoredExceptions.contains(types, exceptionType);
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

  @Override
  public boolean isIgnoredExceptionType(TypeMirror exceptionType) {
    return ignoredExceptions.contains(getTypes(), exceptionType);
  }
}
