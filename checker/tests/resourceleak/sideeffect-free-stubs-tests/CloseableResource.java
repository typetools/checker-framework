// Class representing reources that implements Closeable and Autocloseable to be used by other tests
// in this directory.

import java.io.Closeable;

final class CloseableResource implements Closeable {
  @Override
  public void close() {}
}

final class AutoCloseableResource implements AutoCloseable {
  @Override
  public void close() {}
}
