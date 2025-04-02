// Tests for cases where a finally-block throws an exception.
// try { return } finally { ... } creates complex control flow since the finally block executes
// AFTER the return.  Normally a method doesn't do anything after it returns.

import java.io.*;
import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.nullness.qual.*;

abstract class FinallyThrowsException {

  public abstract boolean choice();

  public abstract @Owning @Nullable Closeable alloc() throws IOException;

  public @Owning Closeable test1() throws IOException {
    // Resource leak: the allocated resource is lost when the exception is thrown
    try {
      // :: error: (required.method.not.called)
      return alloc();
    } finally {
      throw new IOException();
    }
  }

  public @Owning Closeable test2(@Owning Closeable r1) throws IOException {
    // OK: we are not obligated to close @Owning parameters if we throw an exception
    try {
      return r1;
    } finally {
      throw new IOException();
    }
  }

  public @Owning Closeable test3() throws IOException {
    // Resource leak: the allocated resource is lost if x.close() throws
    try (Closeable x = alloc()) {
      // :: error: (required.method.not.called)
      return alloc();
    }
  }

  public @Owning Closeable test4(@Owning Closeable r1) throws IOException {
    // OK: we are not obligated to close @Owning parameters if we throw an exception
    try (Closeable x = alloc()) {
      return r1;
    }
  }

  // Demonstration of a particular pattern that should work; there are some variants
  // involving finally-blocks below.
  public @Owning @Nullable Closeable test5(@Owning Closeable r1) throws IOException {
    if (choice()) {
      return r1;
    } else {
      r1.close();
      return null;
    }
  }

  // Identical to test5, but wrapped in try-finally-throw.
  // OK: we are not obligated to close @Owning parameters if we throw an exception.
  public @Owning @Nullable Closeable test6(@Owning Closeable r1) throws IOException {
    try {
      if (choice()) {
        return r1;
      } else {
        r1.close();
        return null;
      }
    } finally {
      throw new IOException();
    }
  }

  // Identical to test5, but wrapped in try-with-resources.
  // This is a false positive: we are not obligated to close @Owning parameters if we throw an
  // exception.  The cause of the false positive is the same control-flow merge described in
  // IfBranch.test1(), although the merge is more subtle: both returns have an edge to the
  // implicit `finally { x.close(); }` of the try-with-resources block.
  // :: error: (required.method.not.called)
  public @Owning @Nullable Closeable test7(@Owning Closeable r1) throws IOException {
    try (Closeable x = alloc()) {
      if (choice()) {
        return r1;
      } else {
        r1.close();
        return null;
      }
    }
  }

  // -------------------------------------------------------------------------
  // Larger examples demonstrating the principles above.

  // A complicated method that either (1) returns its r1 argument unchanged or (2) allocates
  // a new resource r2, closes its r1 argument, and returns r2.
  public @Owning Closeable test8(@Owning Closeable r1) throws IOException {
    Closeable r2 = alloc();

    // If allocation failed, return r1 unchanged.
    if (r2 == null) {
      return r1;
    }

    // If allocation succeeded, close r1 and return r2.
    try {
      r1.close();
      return r2;
    } catch (Exception e) {
      // If we fail to close r1, we want to raise that exception.
      // But, we have to close r2 before throwing.
      try {
        r2.close();
      } catch (Exception onClose) {
        e.addSuppressed(onClose);
      }
      throw e;
    }
  }

  // Variant of test8 wrapped in try-with-resources.
  // This exhibits one false positive on r1 (see test7) and one true positive on r2 (see test3).
  // The true positive might be very surprising because the same code is resource-leak-free when
  // it does not appear under try-with-resources (see test8).
  // :: error: (required.method.not.called)
  public @Owning Closeable test9(@Owning Closeable r1) throws IOException {
    try (Closeable x = alloc()) {
      // :: error: (required.method.not.called)
      Closeable r2 = alloc();
      if (r2 == null) {
        return r1;
      }
      try {
        r1.close();
        return r2; // possible leak if x.close() throws
      } catch (Exception e) {
        try {
          r2.close();
        } catch (Exception onClose) {
          e.addSuppressed(onClose);
        }
        throw e;
      }
    }
  }

  // Rewrite of test9 with no resource leaks or false positives.  The key idea is to move the
  // `return` outside the try-with-resources.  We need the complicated catch-block in case the
  // implicit call to `x.close()` throws.
  public @Owning Closeable test10(@Owning Closeable r1) throws IOException {
    Closeable result = null;
    try (Closeable x = alloc()) {
      result = test8(r1);
    } catch (Exception e) {
      try {
        if (result != null) {
          result.close();
        }
      } catch (Exception onClose) {
        e.addSuppressed(onClose);
      }
      throw e;
    }
    return result;
  }
}
