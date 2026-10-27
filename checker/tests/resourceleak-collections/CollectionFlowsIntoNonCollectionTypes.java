import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

/*
 * Tests a resource collection flowing into a location whose type is not a resource collection,
 * such as Object or Collection<?>. Such a location holds no collection ownership, and the
 * collection's obligations stay with their current owner, so the flow is allowed. The exception is
 * a location that takes ownership: an @Owning parameter, an @Owning field, or a return. A
 * collection that may have obligations cannot flow into such a location.
 */
@InheritableMustCall("close")
class CollectionFlowsIntoNonCollectionTypes {
  Object objectField;

  @Owning Object owningObjectField;

  static void take(Object o) {}

  static void takeCollection(Collection<?> c) {}

  static void sink(@Owning Object o) {}

  static void dispose(@OwningCollection List<Resource> list) {
    for (Resource r : list) {
      r.close();
      r.flush();
    }
  }

  void close() {}

  void emptyCollectionToObject() {
    List<Resource> list = new ArrayList<>();
    take(list);
    takeCollection(list);
    System.out.println(list);
  }

  void owningCollectionToObjectThenDisposed() {
    List<Resource> list = new ArrayList<>();
    list.add(new Resource());
    take(list);
    takeCollection(list);
    System.out.println(list);
    dispose(list);
  }

  void notOwningParameterToObject(List<Resource> list) {
    take(list);
    takeCollection(list);
    System.out.println(list);
    String s = String.valueOf(list);
  }

  void objectMethodsOnCollections(List<Resource> param) {
    List<Resource> list = new ArrayList<>();
    list.toString();
    list.getClass();
    param.toString();
    param.hashCode();
  }

  // The diamond's element type is inferred from nothing, so its @MustCall type is unknown.
  void diamondWithObjectTarget() {
    Object o = new ArrayList<>();
    take(o);
    @SuppressWarnings("unchecked")
    List<Object> asList = (List<Object>) o;
    takeCollection(asList);
  }

  void varDiamondToObject() {
    var list = new ArrayList<>();
    take(list);
  }

  Object emptyCollectionReturnedAsObject() {
    List<Resource> list = new ArrayList<>();
    return list;
  }

  void emptyCollectionToOwningObjectParameter() {
    List<Resource> list = new ArrayList<>();
    sink(list);
  }

  // The caller remains responsible for the elements, so the leak is still reported.

  void owningCollectionToObjectNotDisposed() {
    List<Resource> list = new ArrayList<>();
    // :: error: unfulfilled.collection.obligations
    list.add(new Resource());
    take(list);
  }

  void objectAliasNotDisposed() {
    List<Resource> list = new ArrayList<>();
    // :: error: unfulfilled.collection.obligations
    list.add(new Resource());
    Object o = list;
  }

  void objectFieldNotDisposed() {
    List<Resource> list = new ArrayList<>();
    // :: error: unfulfilled.collection.obligations
    list.add(new Resource());
    objectField = list;
  }

  // Inserting through a view cast from Object does not transfer the element's obligation.
  void insertThroughCastFromObject(Object o) {
    @SuppressWarnings("unchecked")
    List<Resource> list = (List<Resource>) o;
    // :: error: required.method.not.called
    Resource r = new Resource();
    list.add(r);
  }

  // These locations take ownership but cannot be tracked as collections.

  Object owningCollectionReturnedAsObject() {
    List<Resource> list = new ArrayList<>();
    list.add(new Resource());
    // :: error: return
    return list;
  }

  void owningCollectionToOwningObjectParameter() {
    List<Resource> list = new ArrayList<>();
    list.add(new Resource());
    // :: error: argument
    sink(list);
  }

  @CreatesMustCallFor("this")
  void owningCollectionToOwningObjectField() {
    List<Resource> list = new ArrayList<>();
    list.add(new Resource());
    // :: error: assignment
    owningObjectField = list;
  }
}
