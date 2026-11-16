// Like AliasNotInMustCallStore, but the resource alias that is unassigned on some path is a
// @MustCallAlias wrapper whose class declares a different must-call method than the resource it
// wraps.  Calling the wrapper's own must-call method satisfies the obligation.

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;
import org.checkerframework.checker.mustcall.qual.MustCallAlias;
import org.checkerframework.checker.mustcall.qual.Owning;

class AliasNotInMustCallStoreWrapper {

  @InheritableMustCall("dispose")
  static class Disposer {
    private final @Owning Reader reader;

    @MustCallAlias Disposer(@MustCallAlias Reader reader) {
      this.reader = reader;
    }

    @EnsuresCalledMethods(value = "this.reader", methods = "close")
    void dispose() throws IOException {
      reader.close();
    }
  }

  void whileLoop(boolean b, boolean c) throws IOException {
    FileReader r = new FileReader("x");
    if (b) {
      r.close();
      return;
    }
    Disposer d = new Disposer(r);
    d.dispose();
    while (c) {}
  }

  void ifReturn(boolean b, boolean c) throws IOException {
    FileReader r = new FileReader("x");
    if (b) {
      r.close();
      return;
    }
    Disposer d = new Disposer(r);
    d.dispose();
    if (c) {
      return;
    }
  }
}
