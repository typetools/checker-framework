// Test case for https://github.com/typetools/checker-framework/issues/6179
// (and other rules regarding @Owning and exceptions)

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.dataflow.qual.SideEffectFree;

abstract class OwnershipWithExceptions {

  static class ManualExample1 {
    void example(String myHost, int myPort) throws IOException {
      Socket s = new Socket(myHost, myPort);
      closeSocket(s);
    }

    void closeSocket(@Owning @MustCall("close") Socket t) {
      try {
        t.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  static class ManualExample2 {
    void example(String myHost, int myPort) throws Exception {
      // Error: `s` is not closed on all paths
      // ::error: (required.method.not.called)
      Socket s = new Socket(myHost, myPort);

      // `closeSocket` does not have to close `s` when it throws IOException.
      // Instead, this method has to catch the exception and close `s`.
      closeSocket(s);
    }

    void closeSocket(@Owning Socket t) throws IOException {
      throw new IOException();
    }
  }

  abstract @Owning Closeable alloc();

  @SideEffectFree
  abstract boolean arbitraryChoice();

  abstract void transfer(@Owning Closeable resource) throws IOException;

  void transferAndPropagateException(@Owning Closeable resource) throws IOException {
    transfer(resource);
  }

  void transferHasNoObligationsOnException(@Owning Closeable resource) throws IOException {
    throw new IOException();
  }

  // :: error: (required.method.not.called)
  void transferAndIgnoreExceptionWithoutClosing(@Owning Closeable zzz) {
    try {
      transfer(zzz);
    } catch (IOException ignored) {
    }
  }

  boolean transferAndIgnoreExceptionCorrectly(@Owning Closeable resource) {
    try {
      transfer(resource);
      return true;
    } catch (Exception e) {
      try {
        resource.close();
      } catch (Exception other) {
      }
      return false;
    }
  }

  // Passing an argument as an @Owning parameter does not transfer ownership if
  // the called method throws.  So, this is not correct: if transfer(resource)
  // throws an exception, it leaks the resource.
  void noExceptionHandling() throws IOException {
    // ::error: (required.method.not.called)
    Closeable resource = alloc();
    // ::error: (assignment)
    @CalledMethods("close") Closeable a = resource;
    transfer(resource);
    // ::error: (assignment)
    @CalledMethods("close") Closeable b = resource;
  }

  class FinalOwnedField implements Closeable {

    final @Owning Closeable resource;

    FinalOwnedField() throws IOException {
      resource = alloc();
    }

    FinalOwnedField(int ignored) throws IOException {
      // Same as the 0-argument constructor, but handled correctly.
      resource = alloc();
      try {
        if (arbitraryChoice()) {
          throw new IOException();
        }
      } catch (Exception e) {
        resource.close();
        throw e;
      }
    }

    FinalOwnedField(@Owning Closeable resource) throws IOException {
      // Although, when the resource was passed by a caller, then we can be
      // more relaxed.  On exception, ownership remains with the caller.
      this.resource = resource;
      if (arbitraryChoice()) {
        throw new IOException();
      }
    }

    // Not allowed: destructors have to close @Owning fields even on exception.
    @Override
    @EnsuresCalledMethods(
        value = "this.resource",
        methods = {"close"})
    // ::error: (destructor.exceptional.postcondition)
    public void close() throws IOException {
      throw new IOException();
    }
  }

  class MutableOwnedField implements Closeable {

    @Owning Closeable resource;

    @RequiresCalledMethods(
        value = "this.resource",
        methods = {"close"})
    @CreatesMustCallFor("this")
    void realloc() throws IOException {
      // Unlike in a constructor, field assignments in normal methods are not
      // leaked when the method exits with an exception, since the reciever
      // is still accessible to the caller.
      resource = alloc();
      if (arbitraryChoice()) {
        throw new IOException();
      }
    }

    @Override
    @EnsuresCalledMethods(
        value = "this.resource",
        methods = {"close"})
    public void close() throws IOException {
      resource.close();
    }
  }
}
