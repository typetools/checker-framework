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

  // assignment to @NotOwningCollection field is legal, but
  // here the obligation is not fulfilled after reassigning.
  public void reassignNotOwningCollectionFieldLegal() {
    // :: error: unfulfilled.collection.obligations
    notOwningField = getList();
  }

  List<Resource> getList() {
    return new ArrayList<Resource>();
  }

  // no justification for reassignment. Not allowed.
  public void reassignCollectionFieldIllegal() {
    // :: error: unfulfilled.collection.obligations
    resList = new ArrayList<>();
  }

  // owning collection field reassignment with an owning parameter
  public void reassignCollectionFieldWithOwningCollection(@OwningCollection List<Resource> list) {
    // :: error: unfulfilled.collection.obligations
    resList = list;
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
  // but method is missing an @CreatesMustCallFor("this")
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

  // allowed since assignment happens only if the field is null
  public void reassignCollectionFieldIfNull() {
    if (resList == null) {
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
  // :: error: unfulfilled.field.obligations
  List<Resource> ocField = new ArrayList<>();

  void tryTransferringFieldOwnershipAssignment() {
    // try to steal ownership
    List<Resource> ownershipStealer = ocField;
    // :: error: illegal.collection.mutator.owning.insert.into.notowning
    // :: error: required.method.not.called
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

// The field starts at @OwningCollectionBottom, so the initializer is its first write. Each
// constructor starts from the state the initializers leave.
class OwningFieldWithOwningRHSInitializer implements Closeable {
  List<Resource> fieldList = getList();

  OwningFieldWithOwningRHSInitializer() {}

  OwningFieldWithOwningRHSInitializer(int i) {
    // :: error: unfulfilled.collection.obligations
    fieldList = new ArrayList<>();
  }

  List<Resource> getList() {
    return new ArrayList<Resource>();
  }

  @CollectionFieldDestructor("fieldList")
  @Override
  public void close() {
    for (Resource r : fieldList) {
      r.close();
      r.flush();
    }
  }
}

// here, the assignment to an @OwningCollection rhs is allowed
class OwningFinalFieldWithOwningRHSInitializer implements Closeable {
  final List<Resource> fieldList = getList();

  List<Resource> getList() {
    return new ArrayList<Resource>();
  }

  @CollectionFieldDestructor("fieldList")
  @Override
  public void close() {
    for (Resource r : fieldList) {
      r.close();
      r.flush();
    }
  }
}

// A field that holds null has no obligation, so writing to it is allowed.
class OwningFieldWithNullInitializer implements Closeable {
  List<Resource> fieldList = null;

  OwningFieldWithNullInitializer(@OwningCollection List<Resource> list) {
    fieldList = list;
  }

  @CreatesMustCallFor("this")
  void reset(@OwningCollection List<Resource> list) {
    close();
    fieldList = null;
    fieldList = list;
  }

  // :: error: missing.creates.mustcall.for
  void resetWithoutCreatesMustCallFor(@OwningCollection List<Resource> list) {
    close();
    fieldList = null;
    fieldList = list;
  }

  void clearReference() {
    // :: error: unfulfilled.collection.obligations
    fieldList = null;
  }

  @CollectionFieldDestructor("fieldList")
  @Override
  public void close() {
    for (Resource r : fieldList) {
      r.close();
      r.flush();
    }
  }
}

class OwningFinalField implements Closeable {
  final List<Resource> fieldList;

  public OwningFinalField(@OwningCollection List<Resource> list) {
    this.fieldList = list;
  }

  @CollectionFieldDestructor("fieldList")
  @Override
  public void close() {
    for (Resource r : fieldList) {
      r.close();
      r.flush();
    }
  }
}

class OwningListFactory {
  static List<Resource> newList() {
    List<Resource> list = new ArrayList<>();
    list.add(new Resource());
    return list;
  }
}

// An initializer block can give the field its first value.
class OwningFieldAssignedInInitializerBlock implements Closeable {
  List<Resource> fieldList;

  {
    fieldList = OwningListFactory.newList();
  }

  @CollectionFieldDestructor("fieldList")
  @Override
  public void close() {
    for (Resource r : fieldList) {
      r.close();
      r.flush();
    }
  }
}

// A write on one branch leaves the field @OwningCollection after the branches join.
class OwningFieldReassignedInInitializerBlock implements Closeable {
  static boolean flag;

  List<Resource> fieldList;

  {
    if (flag) {
      fieldList = OwningListFactory.newList();
    }
    // :: error: unfulfilled.collection.obligations
    fieldList = new ArrayList<>();
  }

  @CollectionFieldDestructor("fieldList")
  @Override
  public void close() {
    for (Resource r : fieldList) {
      r.close();
      r.flush();
    }
  }
}

class OwningFieldFilledInInitializerBlock implements Closeable {
  List<Resource> fieldList = new ArrayList<>();

  {
    fieldList.add(new Resource());
  }

  OwningFieldFilledInInitializerBlock() {
    // :: error: unfulfilled.collection.obligations
    fieldList = new ArrayList<>();
  }

  @CollectionFieldDestructor("fieldList")
  @Override
  public void close() {
    for (Resource r : fieldList) {
      r.close();
      r.flush();
    }
  }
}

class OwningFieldReassignedAfterBranch implements Closeable {
  List<Resource> fieldList;

  OwningFieldReassignedAfterBranch(boolean b) {
    if (b) {
      fieldList = OwningListFactory.newList();
    }
    // :: error: unfulfilled.collection.obligations
    fieldList = new ArrayList<>();
  }

  @CollectionFieldDestructor("fieldList")
  @Override
  public void close() {
    for (Resource r : fieldList) {
      r.close();
      r.flush();
    }
  }
}

// Inherited fields start at @OwningCollection: the superclass constructor may already have added
// resources.
class OwningFieldSuperclass implements Closeable {
  List<Resource> fieldList = new ArrayList<>();

  OwningFieldSuperclass() {
    fieldList.add(new Resource());
  }

  @CollectionFieldDestructor("fieldList")
  @Override
  public void close() {
    for (Resource r : fieldList) {
      r.close();
      r.flush();
    }
  }
}

class OwningFieldSubclass extends OwningFieldSuperclass {
  OwningFieldSubclass() {
    super();
    // :: error: unfulfilled.collection.obligations
    fieldList = new ArrayList<>();
  }

  // Disposing the inherited field first makes the write legal.
  OwningFieldSubclass(int i) {
    super();
    close();
    fieldList = new ArrayList<>();
  }

  void overwrite() {
    // :: error: unfulfilled.collection.obligations
    fieldList = new ArrayList<>();
  }

  @CreatesMustCallFor("this")
  void replace(@OwningCollection List<Resource> list) {
    close();
    fieldList = list;
  }
}
