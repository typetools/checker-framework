import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

/*
 * Test whether the defaults of resource collection fields, parameters, return types and new allocations
 * are as expected.
 */
class CollectionOwnershipDefaults {

  int n = 10;

  /*
   * Check that manual MustCall annotations are correctly considered when deciding whether
   * something is a resoure collection.
   */
  public void detectResourceCollection(
      List<@MustCall({}) Socket> l1,
      Collection<@MustCall({"close"}) Socket> l2,
      LinkedList<Socket> l3,
      ArrayList<String> l4,
      ArrayList<@MustCall({"a"}) String> l5) {
    // not a resource collection, this call succeeds.
    checkArgIsOwning(l1);
    // a resource collection, this call fails.
    // :: error: argument
    checkArgIsOwning(l2);
    // a resource collection, this call fails.
    // :: error: argument
    checkArgIsOwning(l3);
    // not a resource collection, this call succeeds.
    checkArgIsOwning(l4);
    // a resource collection, this call fails.
    // :: error: argument
    checkArgIsOwning(l5);
  }

  /*
   * This method checks that its parameter defaults to @NotOwningCollection.
   */
  void checkParamIsNotOwningCollection(List<Socket> list) {
    // list : @NotOwningCollection. Thus, next line should throw an error.

    // :: error: argument
    checkArgIsOwning(list);
  }

  /*
   * Return type should default to @OwningCollection. Thus this is perfectly okay.
   */
  List<Socket> identity(@OwningCollection List<Socket> list) {
    return list;
  }

  /*
   * Check whether manual annotations on the return type correctly override the default.
   */
  @NotOwningCollection
  List<Socket> overrideReturnType(List<Socket> list) {
    return list;
  }

  void overrideReturnTypeClient() {
    // the arraylist passed to the method is never closed. The ownership is not passed
    // to overrideReturnType(), since parameters are @NotOwningCollection by default
    List<Socket> notOwninglist = overrideReturnType(new ArrayList<Socket>());
    List<Socket> owninglist = identity(new ArrayList<Socket>());

    checkArgIsOwning(owninglist);
    // :: error: argument
    checkArgIsOwning(notOwninglist);
  }

  /*
   * Checks that an @OwningCollection can be passed to a resource collection parameter (because it should default
   * to @OwningCollection). I.e. check that the resource collection parameter default is visible at call-site as well.
   */
  void checkResourceCollectionParameterDefault() {
    List<Socket> list = new ArrayList<Socket>();
    closeElements(identity(list));
  }

  /*
   * Checks that return value of a resource collection correctly defaults to @OwningCollection
   */
  void checkResourceCollectionReturnValueDefault() {
    List<Socket> returnVal = identity(new ArrayList<Socket>());
    // returnVal supposed to be @OwningCollection. Thus, first call should succeed, second fail.

    checkArgIsOwning(returnVal);
    // :: error: argument
    checkArgIsOCwoO(returnVal);
  }

  /*
   * Checks that a newly allocated resource collection has type @OwningCollection.
   */
  void checkNewResourceCollectionDefault() {
    List<Socket> newResourceCollection = new ArrayList<Socket>();
    checkArgIsOwning(newResourceCollection);
    // :: error: argument
    checkArgIsOCwoO(newResourceCollection);

    Socket[] newResourceArray = new Socket[n];
    checkArgIsOwning(newResourceArray);
    // :: error: argument
    checkArgIsOCwoO(newResourceArray);
  }

  void closeElements(Socket @OwningCollection [] socketCollection) {
    for (Socket s : socketCollection) {
      try {
        s.close();
      } catch (Exception e) {
      }
    }
  }

  void closeElements(@OwningCollection Collection<Socket> socketCollection) {
    for (Socket s : socketCollection) {
      try {
        s.close();
      } catch (Exception e) {
      }
    }
  }

  void checkArgIsOwning(
      // :: error: unfulfilled.collection.obligations
      @OwningCollection Collection<? extends @MustCallUnknown Object> collection) {}

  // :: error: unfulfilled.collection.obligations
  void checkArgIsOwning(Socket @OwningCollection [] collection) {}

  void checkArgIsOCwoO(
      // :: error: illegal.type.annotation
      @OwningCollectionWithoutObligation
          Collection<? extends @MustCallUnknown Object> collection) {}

  // :: error: illegal.type.annotation
  void checkArgIsOCwoO(Socket @OwningCollectionWithoutObligation [] collection) {}
}
