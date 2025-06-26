import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class ConstructorTakesOwnership implements Closeable {
  List<Resource> resList = new ArrayList<>();
  @NotOwningCollection List<Resource> notOwningField = new ArrayList<>();

  public ConstructorTakesOwnership(@OwningCollection List<Resource> list) {
    resList = list;
  }

  // :: error: unfulfilled.collection.obligations
  public ConstructorTakesOwnership(@OwningCollection List<Resource> list, int a) {
    if (a > 5) {
      // this doesn't remove the obligation of list
      notOwningField = list;
    }
  }

  public ConstructorTakesOwnership(@OwningCollection List<Resource> list, float a) {
    resList = list;
    if (a > 5) {
      // illegal reassignment
      // :: error: unfulfilled.collection.obligations
      resList = new ArrayList<>();
    }
  }

  // no justification for reassignment. Not allowed.
  public void reassignCollectionFieldIllegal() {
    // :: error: unfulfilled.collection.obligations
    resList = new ArrayList<>();
  }

  // allowed since resList has @OwningCollectionWithoutObligation at reassignment time.
  // No CreatesMustCallFor("this") required, since the rhs is @OwningCollectionWithoutObligation.
  public void reassignCollectionFieldLegal() {
    for (Resource r : resList) {
      r.close();
      r.flush();
    }
    resList = new ArrayList<>();
  }

  // assignment allowed since resList has @OwningCollectionWithoutObligation at reassignment time,
  // but method
  // is missing an @CreatesMustCallFor("this")
  // :: error: missing.creates.mustcall.for
  public void reassignCollectionFieldMissingCMCF() {
    for (Resource r : resList) {
      r.close();
      r.flush();
    }
    List<Resource> newList = new ArrayList<>();
    newList.add(new Resource());
    resList = newList;
  }

  // allowed since resList has @OwningCollectionWithoutObligation at reassignment time
  public void reassignCollectionFieldAfterDestructor() {
    close();
    resList = new ArrayList<>();
  }

  // allowed since assignment cannot overwrite anything
  public void reassignCollectionFieldIfNull() {
    if (resList != null) {
      resList = new ArrayList<>();
    }
  }

  // assignment allowed since field not owned by class.
  // treated as normal variable.
  public void reassignCollectionNotOwningField() {
    notOwningField = new ArrayList<>();
    // :: error: unfulfilled.collection.obligations
    notOwningField.add(new Resource());
  }

  @CollectionFieldDestructor("resList")
  @Override
  public void close() {
    for (Resource r : resList) {
      r.close();
      r.flush();
    }
  }
}

class OwningCollectionFieldTyping {
  // :: error: unfulfilled.collection.obligations
  List<Resource> ocField = new ArrayList<>();

  void tryTransferringFieldOwnershipAssignment() {
    // try to steal ownership
    List<Resource> ownershipStealer = ocField;
    // :: error: method.invocation
    ownershipStealer.add(new Resource());
  }

  void tryTransferringFieldOwnershipArgumentPassing() {
    // try to give away ownership of field to parameter
    // :: error: transfer.owningcollection.field.ownership
    takeArgumentOwnership(ocField);
  }

  void takeArgumentOwnership(@OwningCollection List<Resource> param) {
    // do scary things like adding new elements with calling obligations, or pass
    // it on into a class that stores it as an owning field.
    param.add(new Resource());
    Aggregator agg = new Aggregator(param);
    agg.close();
  }

  List<Resource> tryTransferringFieldOwnershipReturn() {
    // :: error: transfer.owningcollection.field.ownership
    return ocField;
  }
}
