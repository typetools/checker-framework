import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
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

  /* This should default to @OwningCollection */
  Collection<Socket> resourceCollectionField;

  /*
   * Check that resource collection field defaults to @OwningCollection.
   */
  void checkResourceCollectionFieldDefault() {
    checkArgIsOwning(resourceCollectionField);
    // :: error: argument
    checkArgIsOCwoO(resourceCollectionField);
  }

  /*
  * This method checks that its parameter defaults to @NotOwningCollection.

  * Parameter should default to @NotOwningCollection.
  * Return type should default to @OwningCollection.
  */
  List<Socket> identity(List<Socket> list) {
    // list : @NotOwningCollection. Thus, next line should throw an error.

    // :: error: argument
    checkArgIsOwning(list);

    return list;
  }

  /*
   * Checks that an @OwningCollection can be passed to a resource collection parameter (because it should default
   * to @OwningCollection). I.e. check that the resource collection parameter default is visible at call-site as well.
   */
  void checkResourceCollectionParameterDefault() {
    @OwningCollection List<Socket> list = new ArrayList<Socket>();
    identity(list);
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

  void checkArgIsOwning(@OwningCollection Collection<Socket> collection) {}

  void checkArgIsOwning(@OwningCollection Socket[] collection) {}

  void checkArgIsOCwoO(@OwningCollectionWithoutObligation Collection<Socket> collection) {}

  void checkArgIsOCwoO(@OwningCollectionWithoutObligation Socket[] collection) {}
}
