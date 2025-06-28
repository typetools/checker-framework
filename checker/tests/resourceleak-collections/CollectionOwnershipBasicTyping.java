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

  // this return looks harmless. However, it returns a collection, but not its ownership.
  // Since no obligation will be created at call-site (it expects a non-owning collection),
  // this method must ensure the returned collection has no obligations, which it fails to
  // do in this case.
  @NotOwningCollection
  Collection<Socket> checkIllegalNotOwningReturn() {
    List<Socket> l = new ArrayList<>();
    // :: error: unfulfilled.collection.obligations
    l.add(new Socket());
    return l;
  }

  // this is the correct version of above. It passes ownership to another method first,
  // and then returns the non-owning reference.
  @NotOwningCollection
  Collection<Socket> checkLegalNotOwningReturn() {
    List<Socket> list = new ArrayList<>();
    closeElements(list);
    return list;
  }

  // check that unrefinement in assignments is allowed.
  void checkUnrefinement() {
    List<Socket> list = new ArrayList<>();
    List<Socket> newOwner = list;
    // newOwner : @OwningCollection, list: @NotOwningCollection
    list = newOwner;
    // newOwner = @NotOwningCollection, list: @OwningCollection
    closeElements(list);
  }

  void testAssignmentTransfersOwnership() {
    // col is overwritten and its obligation never fulfilled or passed on
    Collection<Socket> col = new ArrayList<>();
    // :: error: unfulfilled.collection.obligations
    col.add(new Socket());
    Collection<Socket> col2 = new ArrayList<>();
    // col : @OwningCollection, col2 : @OwningCollection
    col = col2;
    // col : @OwningCollection, col2 : @NotOwningCollection

    // col2 is NotOwningCollection, so the second call should fail
    checkArgIsOwning(col);
    // :: error: argument
    checkArgIsOwning(col2);
  }

  /*
   * Check that a resource collection constructed without explicit type argument is of type @OwningCollection
   * as well.
   */
  void testDiamond() {
    Collection<Socket> col = new ArrayList<>();
    col.add(new Socket());
    // :: error: argument
    checkArgIsOCwoO(col);
    closeElements(col);
  }

  void checkAdvancedDiamond() {
    // check that this doesn't create an obligation and defaults to @OwningCollectionBottom
    Collection<@MustCall Socket> c = new ArrayList<>();
    checkArgIsBottom(c);

    // these all create obligations

    Collection<Socket> c1 = new ArrayList<>();
    Collection<@MustCall("close") Object> c2 = new ArrayList<>();
    Collection<@MustCallUnknown Object> c3 = new ArrayList<>();
    c.add(new Socket());
    // :: error: unfulfilled.collection.obligations
    c1.add(new Socket());
    // :: error: unfulfilled.collection.obligations
    c2.add(new Socket());
    // :: error: unfulfilled.collection.obligations
    c3.add(new Socket());
  }

  void closeElements(@OwningCollection Collection<Socket> socketCollection) {
    for (Socket s : socketCollection) {
      try {
        s.close();
      } catch (Exception e) {
      }
    }
  }

  void checkArgIsBottom(Collection<? extends Object> collection) {}

  // these two methods take on unfulfillable obligations and thus throw an error

  void checkArgIsOwning(
      // :: error: unfulfilled.collection.obligations
      @OwningCollection Collection<? extends @MustCallUnknown Object> collection) {}

  void checkArgIsOCwoO(
      // :: error: illegal.type.annotation
      @OwningCollectionWithoutObligation
          Collection<? extends @MustCallUnknown Object> collection) {}
}
