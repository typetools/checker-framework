// Class representing reources that implements Closable and Autoclosable to be used by other tests
// in this directory.

import java.io.Closeable;

final class CloseableResource implements Closeable {
  @Override
  public void close() {}
}

final class AutoClosableResource implements AutoCloseable {
  @Override
  public void close() {}
}
