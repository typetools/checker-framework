import java.io.IOException;
import java.util.*;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class NotOwningCollectionMutationTest {

  @InheritableMustCall("close")
  static class R {
    void close() throws IOException {}
  }

  R owning() {
    return new R();
  }

  @NotOwning
  R notowning() {
    // :: error: required.method.not.called
    return new R();
  }

  // ------------------------------------------------------------
  // NotOwningCollection receiver + NotOwning element: OK
  // ------------------------------------------------------------
  void ok_noc_add_notowning_param(@NotOwningCollection List<R> l, @NotOwning R r) {
    l.add(r); // ok
  }

  void ok_noc_add_notowning_method(@NotOwningCollection List<R> l) {
    l.add(notowning()); // ok
  }

  // ------------------------------------------------------------
  // NotOwningCollection receiver + owning element: error (smuggling)
  // Close afterwards to avoid extra RLC "required.method.not.called".
  // ------------------------------------------------------------
  void err_noc_add_owning_local_then_close(@NotOwningCollection List<R> l) {
    R r = new R();
    // :: error: illegal.collection.mutator.owning.insert.into.notowning
    l.add(r);
    try {
      r.close();
    } catch (IOException e) {
      // ignore
    }
  }

  void err_noc_add_owning_param_then_close(@NotOwningCollection List<R> l, @Owning R r) {
    // :: error: illegal.collection.mutator.owning.insert.into.notowning
    l.add(r);
    try {
      r.close();
    } catch (IOException e) {
      // ignore
    }
  }

  void err_noc_add_alias_of_owning_then_close(@NotOwningCollection List<R> l) {
    R r = new R();
    R r2 = r;
    // :: error: illegal.collection.mutator.owning.insert.into.notowning
    l.add(r2);
    try {
      r.close();
    } catch (IOException e) {
      // ignore
    }
  }

  // ------------------------------------------------------------
  // NotOwningCollection receiver via move/alias: old name becomes NOC
  // (assuming your assignment transfer sets RHS (old owner) to NOC)
  // ------------------------------------------------------------
  void err_noc_receiver_after_move_then_close() {
    @OwningCollection List<R> owner = new ArrayList<>();
    List<R> newOwner = owner; // after transfer: "owner" becomes @NotOwningCollection
    R r = new R();
    // :: error: illegal.collection.mutator.owning.insert.into.notowning
    owner.add(r);
    try {
      r.close();
    } catch (IOException e) {
      // ignore
    }

    for (R r2 : newOwner) {
      try {
        r2.close();
      } catch (Exception e) {
      }
    }
  }

  // ------------------------------------------------------------
  // NotOwningCollection as a field: same rule (can add only non-owning)
  // ------------------------------------------------------------

  final @NotOwningCollection List<R> cache = new ArrayList<>();

  void ok_noc_field_add_notowning(@NotOwning R r) {
    cache.add(r); // ok
  }

  void ok_noc_field_add_notowning_2(R r) {
    cache.add(r); // also ok
  }

  void err_noc_field_add_owning_then_close() {
    R r = new R();
    // :: error: illegal.collection.mutator.owning.insert.into.notowning
    cache.add(r);
    try {
      r.close();
    } catch (IOException e) {
      // ignore
    }
  }

  // ------------------------------------------------------------
  // OwningCollection receiver + owning element: should create collection obligation.
  // If you don't discharge via a certified loop, expect unfulfilled.collection.obligations.
  // ------------------------------------------------------------

  void err_oc_add_owning_local_no_dispose() {
    @OwningCollection List<R> l = new ArrayList<>();
    R r = new R();
    // :: error: unfulfilled.collection.obligations
    l.add(r);
  }

  void err_oc_add_owning_call_no_dispose() {
    List<R> l = new ArrayList<>();
    // :: error: unfulfilled.collection.obligations
    l.add(owning());
  }

  void err_oc_add_notowning_call_no_dispose() {
    @OwningCollection List<R> l = new ArrayList<>();
    // :: error: illegal.collection.mutator.nonowning.insert.into.owning
    // :: error: unfulfilled.collection.obligations
    l.add(notowning());
  }

  void err_oc_add_owning_alias_no_dispose() {
    @OwningCollection List<R> l = new ArrayList<>();
    R r = new R();
    R r2 = r;
    // :: error: unfulfilled.collection.obligations
    l.add(r2);
  }

  void err_oc_add_owning_newexpr_no_dispose() {
    @OwningCollection List<R> l = new ArrayList<>();
    // :: error: unfulfilled.collection.obligations
    l.add(new R());
  }

  // Discharge via a certified loop (close is inside try/catch so no early-exit via exception).
  void ok_oc_add_owning_then_dispose_loop() {
    @OwningCollection List<R> l = new ArrayList<>();
    R r = new R();
    l.add(r);

    for (R x : l) {
      try {
        x.close();
      } catch (IOException e) {

      }
    }
  }

  void ok_oc_add_two_then_dispose_loop() {
    @OwningCollection List<R> l = new ArrayList<>();
    R r1 = new R();
    R r2 = new R();
    l.add(r1);
    l.add(r2);

    for (R x : l) {
      try {
        x.close();
      } catch (IOException e) {
        // swallow
      }
    }
  }

  // inserting @NotOwning elements into an owning collection:
  void err_oc_insert_notowning(@OwningCollection List<R> l, @NotOwning R r) {
    // :: error: illegal.collection.mutator.nonowning.insert.into.owning
    l.add(r);
    close_collection(l);
  }

  void close_collection(@OwningCollection List<R> l) {
    for (R r : l) {
      try {
        r.close();
      } catch (IOException e) {
      }
    }
  }

  // ------------------------------------------------------------
  // addAll: TODO: Take care of the type errors in addAll via JDK stubs
  // ------------------------------------------------------------

  //  void err_noc_receiver_addAll_noc_arg(
  //      @NotOwningCollection List<R> dst, @NotOwningCollection List<R> src) {
  //    // :: error: method.invocation
  //    dst.addAll(src);
  //  }
  //
  //  void err_noc_receiver_addAll_oc_arg(
  //      @NotOwningCollection List<R> dst, @OwningCollection List<R> src) {
  //    // :: error: method.invocation
  //    dst.addAll(src);
  //  }
}
