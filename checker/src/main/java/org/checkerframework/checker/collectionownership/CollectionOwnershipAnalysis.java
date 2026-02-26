package org.checkerframework.checker.collectionownership;

import com.google.common.collect.ImmutableSet;
import java.io.UnsupportedEncodingException;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.resourceleak.ResourceLeakChecker;
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
   * The set of exceptions to ignore, cached from {@link
   * ResourceLeakChecker#getIgnoredExceptions()}.
   */
  private static final SetOfTypes DEFAULT_IGNORED_EXCEPTIONS =
      SetOfTypes.anyOfTheseNames(
          ImmutableSet.of(
              // Any method call has a CFG edge for Throwable/RuntimeException/Error
              // to represent run-time misbehavior. Ignore it.
              Throwable.class.getCanonicalName(),
              Error.class.getCanonicalName(),
              RuntimeException.class.getCanonicalName(),
              // Use the Nullness Checker to prove this won't happen.
              NullPointerException.class.getCanonicalName(),
              // These errors can't be predicted statically, so ignore them and assume
              // they won't happen.
              ClassCircularityError.class.getCanonicalName(),
              ClassFormatError.class.getCanonicalName(),
              NoClassDefFoundError.class.getCanonicalName(),
              OutOfMemoryError.class.getCanonicalName(),
              // It's not our problem if the Java type system is wrong.
              ClassCastException.class.getCanonicalName(),
              // It's not our problem if the code is going to divide by zero.
              ArithmeticException.class.getCanonicalName(),
              // Use the Index Checker to prevent these errors.
              ArrayIndexOutOfBoundsException.class.getCanonicalName(),
              NegativeArraySizeException.class.getCanonicalName(),
              // Most of the time, this exception is infeasible, as the charset used
              // is guaranteed to be present by the Java spec (e.g., "UTF-8").
              // Eventually, this exclusion could be refined by looking at the charset
              // being requested.
              UnsupportedEncodingException.class.getCanonicalName()));

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

  @Override
  public boolean isIgnoredExceptionType(TypeMirror exceptionType) {
    return DEFAULT_IGNORED_EXCEPTIONS.contains(getTypes(), exceptionType);
  }
}
