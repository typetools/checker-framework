// A `@CreatesMustCallFor("this")` method called on `super` can insert into the subclass's owning
// collection field through an overriding method, so afterward the field has an obligation.

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class SuperCallFieldState {

  static class Base implements Closeable {
    @CreatesMustCallFor("this")
    void add(@Owning Resource r) {
      store(r);
    }

    @CreatesMustCallFor("this")
    void store(@Owning Resource r) {
      r.close();
      r.flush();
    }

    @Override
    public void close() {}
  }

  static class Sub extends Base {
    List<Resource> resList = new ArrayList<>();

    @Override
    @CreatesMustCallFor("this")
    void store(@Owning Resource r) {
      resList.add(r);
    }

    @CreatesMustCallFor("this")
    void resetAfterSuperAdd(@Owning Resource r) {
      close();
      // :: error: reset.not.owning
      super.add(r);
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
}
