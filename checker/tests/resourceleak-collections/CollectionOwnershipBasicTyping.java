import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

/*
 * Test whether the defaults of resource collection fields, parameters, return types and new
allocations
 * are as expected.
 */
class CollectionOwnershipBasicTyping {

  int n = 10;

  /*
   * Check that this return is allowed. The local list defaults to @OwningCollection, which
   * the return type does too. Thus, return value is consistent with the return type.
   */
  Collection<Socket> checkReturn() {
    List<Socket> list = new ArrayList<>();
    return list;
  }

  /*
   * Check that this return is disallowed. The parameter list defaults to @NotOwningCollection,
   * which is a supertype of the @OwningCollection return type.
   * Thus, return value is inconsistent with the return type.
   */
  Collection<Socket> checkReturn(List<Socket> list) {
    // :: error: return
    return list;
  }

  void testAssignmentTransfersOwnership() {
    Collection<Socket> col = new ArrayList<>();
    Collection<Socket> col2 = new ArrayList<>();
    // col : @OwningCollection, col2 : @OwningCollection
    col = col2;
    // col : @OwningCollection, col2 : @NotOwningCollection

    // col2 is NotOwningCollection, so the second call should fail
    checkArgIsOwning(col);
    // :: error: argument
    checkArgIsOwning(col2);

    closeElements(col);
  }

  /*
   * Check that a resource collection constructed without explicit type argument also is of type @OwningCollection.
   */
  void testDiamond() {
    Collection<Socket> col = new ArrayList<>();
    // :: error: arg
    checkArgIsOCwoO(col);
    closeElements(col);
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
      @OwningCollection Collection<? extends @MustCallUnknown Object> collection) {}

  void checkArgIsOCwoO(
      @OwningCollectionWithoutObligation
          Collection<? extends @MustCallUnknown Object> collection) {}
}
