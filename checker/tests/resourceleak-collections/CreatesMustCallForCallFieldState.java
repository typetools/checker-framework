// A call to a `@CreatesMustCallFor("this")` method can insert into an owning collection field, so
// afterward the field has an obligation and cannot be reassigned.

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class CreatesMustCallForCallFieldState implements Closeable {

  List<Resource> resList = new ArrayList<>();

  CreatesMustCallForCallFieldState(@Owning Resource r) {
    add(r);
    // :: error: unfulfilled.collection.obligations
    resList = new ArrayList<>();
  }

  // The `@CreatesMustCallFor` target "#1" of `fill` resolves to `this` at the call site.
  CreatesMustCallForCallFieldState(@Owning Resource r, int i) {
    fill(this, r);
    // :: error: unfulfilled.collection.obligations
    resList = new ArrayList<>();
  }

  @CreatesMustCallFor("this")
  void add(@Owning Resource r) {
    resList.add(r);
  }

  @CreatesMustCallFor("#1")
  static void fill(CreatesMustCallForCallFieldState h, @Owning Resource r) {
    h.add(r);
  }

  @CreatesMustCallFor("this")
  void resetAfterAdd(@Owning Resource r) {
    close();
    add(r);
    // :: error: unfulfilled.collection.obligations
    resList = new ArrayList<>();
  }

  @Override
  @CollectionFieldDestructor("resList")
  public void close() {
    for (Resource r : resList) {
      r.close();
      r.flush();
    }
  }
}
