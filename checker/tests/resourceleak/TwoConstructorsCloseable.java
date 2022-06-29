// A test case for false positives that I encountered in Zookeeper.

import java.io.Closeable;

public class TwoConstructorsCloseable implements Closeable {
  public TwoConstructorsCloseable(Object obj) {}

  public TwoConstructorsCloseable() {
    this(null);
  }

  public void close() {}

  class Derivative extends TwoConstructorsCloseable {
    Derivative() {
      super(null);
    }
  }
}
