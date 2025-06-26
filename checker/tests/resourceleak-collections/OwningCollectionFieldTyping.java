import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class ConstructorTakesOwnership implements Closeable {
  // :: error: unfulfilled.collection.obligations
  List<Resource> resList = new ArrayList<>();
  @NotOwningCollection List<Resource> notOwningList = new ArrayList<>();

  public ConstructorTakesOwnership(@OwningCollection List<Resource> list) {
    resList = list;
  }

  // :: error: unfulfilled.collection.obligations
  public ConstructorTakesOwnership(@OwningCollection List<Resource> list, int a) {
    if (a > 5) {
      // this doesn't remove the obligation of list
      notOwningList = list;
    }
  }

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
