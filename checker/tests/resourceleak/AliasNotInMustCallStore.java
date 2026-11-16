// A test of a resource alias that is unassigned on some path to a method exit, and through which
// the must-call method is called.  At the check on the edge from a conditional block to the exit
// block, the Must Call store is the exit block's input, which merges every path to the exit, so it
// has no value for the alias.  The Called Methods store is the edge's own store, which records
// that close() was called on the alias.  The checker should issue no error.

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;
import org.checkerframework.checker.mustcall.qual.MustCallAlias;
import org.checkerframework.checker.mustcall.qual.Owning;

class AliasNotInMustCallStore {

  void whileLoop(boolean b, boolean c) throws IOException {
    FileInputStream s = new FileInputStream("x");
    if (b) {
      s.close();
      return;
    }
    FileInputStream t = s;
    t.close();
    while (c) {}
  }

  void ifReturn(boolean b, boolean c) throws IOException {
    FileInputStream s = new FileInputStream("x");
    if (b) {
      s.close();
      return;
    }
    FileInputStream t = s;
    t.close();
    if (c) {
      return;
    }
  }

  void assertStatement(boolean b, boolean c) throws IOException {
    FileInputStream s = new FileInputStream("x");
    if (b) {
      s.close();
      return;
    }
    FileInputStream t = s;
    t.close();
    assert c;
  }

  @InheritableMustCall("dispose")
  static class Wrapper {
    private final @Owning InputStream stream;

    @MustCallAlias Wrapper(@MustCallAlias InputStream stream) {
      this.stream = stream;
    }

    @EnsuresCalledMethods(value = "this.stream", methods = "close")
    public void dispose() throws IOException {
      this.stream.close();
    }
  }

  // The alias that has no Must Call store value is a wrapper whose must-call method, dispose(),
  // differs from that of the wrapped resource, close().
  void whileLoopWrapper(boolean b, boolean c) throws IOException {
    FileInputStream s = new FileInputStream("x");
    if (b) {
      s.close();
      return;
    }
    Wrapper t = new Wrapper(s);
    t.dispose();
    while (c) {}
  }

  void ifAtEnd(boolean b, boolean c) throws IOException {
    FileInputStream s = new FileInputStream("x");
    if (b) {
      s.close();
      return;
    }
    FileInputStream t = s;
    t.close();
    if (c) {
      System.out.println();
    }
  }
}
