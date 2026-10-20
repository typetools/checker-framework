// More tests for the problem described at
// https://github.com/typetools/checker-framework/issues/5722
// Test that an owning return cannot override a non-owning return

import java.io.*;
import java.net.*;
import org.checkerframework.checker.mustcall.qual.*;

public class OwningOverride2 {
  abstract static class A {
    public abstract @NotOwning Socket get();
  }

  static class B extends A {
    @Override
    // :: error: owning.override.return
    public Socket get() {
      return null;
    }
  }
}
