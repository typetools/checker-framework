// An override need not repeat @Owning or @NotOwning where its parameter or return type has no
// must-call obligations, because a value of that type has nothing to own.

import java.net.Socket;
import org.checkerframework.checker.mustcall.qual.*;

public class OwningOverrideNoMustCall {

  interface Source<T extends @MustCallUnknown Object> {
    @NotOwning
    T get();
  }

  static class StringSource implements Source<String> {
    @Override
    public String get() {
      return "";
    }
  }

  // The bound of T has no must-call obligations, so T is never a resource type.
  static class GenericSource<T> implements Source<T> {
    @Override
    public T get() {
      return null;
    }
  }

  static class SocketSource implements Source<Socket> {
    @Override
    // :: error: owning.override.return
    public Socket get() {
      return null;
    }
  }

  interface Sink<T extends @MustCallUnknown Object> {
    void put(@Owning T t);
  }

  static class StringSink implements Sink<String> {
    @Override
    public void put(String s) {}
  }

  static class SocketSink implements Sink<Socket> {
    @Override
    // :: error: owning.override.param
    public void put(Socket s) {}
  }
}
