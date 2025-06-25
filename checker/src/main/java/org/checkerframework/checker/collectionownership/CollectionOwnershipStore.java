package org.checkerframework.checker.collectionownership;

import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;

/**
 * The CollectionOwnership Store behaves like CFAbstractStore but keeps @OwningCollection fields in
 * the store. This is justified by the strict access rules of such fields. Keeping the field in the
 * store is required for verifying the postcondition annotation {@link CollectionFieldDestructor}.
 */
public class CollectionOwnershipStore extends CFAbstractStore<CFValue, CollectionOwnershipStore> {

  private final CollectionOwnershipAnnotatedTypeFactory atypeFactory;

  public CollectionOwnershipStore(
      CollectionOwnershipAnalysis analysis, boolean sequentialSemantics) {
    super(analysis, sequentialSemantics);
    this.atypeFactory = (CollectionOwnershipAnnotatedTypeFactory) analysis.getTypeFactory();
  }

  /** Copy constructor. */
  public CollectionOwnershipStore(
      CollectionOwnershipAnalysis analysis,
      CFAbstractStore<CFValue, CollectionOwnershipStore> other) {
    super(other);
    this.atypeFactory = ((CollectionOwnershipStore) other).atypeFactory;
  }

  /*
   * Keep {@code OwningCollection} fields in the store.
   */
  @Override
  protected CFValue newFieldValueAfterMethodCall(
      FieldAccess fieldAccess,
      GenericAnnotatedTypeFactory<CFValue, CollectionOwnershipStore, ?, ?> atf,
      CFValue value) {
    CFValue superResult = super.newFieldValueAfterMethodCall(fieldAccess, atf, value);
    if (superResult == null) {
      if (atypeFactory.isResourceCollection(fieldAccess.getField().asType())) {
        switch (atypeFactory.getCoType(value)) {
          case OwningCollection:
          case OwningCollectionWithoutObligation:
            return value;
          default:
            return superResult;
        }
      }
    }
    return superResult;
  }
}
