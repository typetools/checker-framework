package org.checkerframework.checker.collectionownership;

import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;

/**
 * The CollectionOwnership Store behaves like CFAbstractStore but keeps @OwningCollection fields in
 * the store. This is justified by the strict access rules of such fields. Keeping the field in the
 * store is required for verifying the postcondition annotation {@code @CollectionFieldDestructor}.
 */
public class CollectionOwnershipStore extends CFAbstractStore<CFValue, CollectionOwnershipStore> {

  /** the annotated type factory */
  private final CollectionOwnershipAnnotatedTypeFactory atypeFactory;

  /**
   * Constructs a collection ownership store.
   *
   * @param analysis the collection ownership analysis
   * @param sequentialSemantics if true, use sequential semantics
   */
  public CollectionOwnershipStore(
      CollectionOwnershipAnalysis analysis, boolean sequentialSemantics) {
    super(analysis, sequentialSemantics);
    this.atypeFactory = (CollectionOwnershipAnnotatedTypeFactory) analysis.getTypeFactory();
  }

  /**
   * Copy constructor.
   *
   * @param analysis the collection ownership analysis
   * @param other the store to construct from
   */
  public CollectionOwnershipStore(
      CollectionOwnershipAnalysis analysis,
      CFAbstractStore<CFValue, CollectionOwnershipStore> other) {
    super(other);
    this.atypeFactory = ((CollectionOwnershipStore) other).atypeFactory;
  }

  /*
   * Keep OwningCollection fields in the store.
   */
  @Override
  protected CFValue newFieldValueAfterMethodCall(
      FieldAccess fieldAccess,
      GenericAnnotatedTypeFactory<CFValue, CollectionOwnershipStore, ?, ?> atf,
      CFValue value) {
    CFValue superResult = super.newFieldValueAfterMethodCall(fieldAccess, atf, value);
    if (superResult == null) {
      if (atypeFactory.isResourceCollectionField(fieldAccess.getField())) {
        return value;
      }
    }
    return superResult;
  }
}
