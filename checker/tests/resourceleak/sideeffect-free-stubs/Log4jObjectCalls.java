// RLC-specific stubs mark these Log4j logging methods as SideEffectFree so that
// diagnostic logging after a resource is closed does not wipe out the close fact.
// This file checks the Object-taking debug/warn/error overloads that were added.

package org.apache.log4j;

import java.io.Closeable;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.qual.Owning;

class Category {
  public void debug(Object message) {}

  public void error(Object message) {}

  public void warn(Object message) {}
}

final class CloseableResource implements Closeable {
  @Override
  public void close() {}
}

class Log4jDebugObject implements Closeable {
  private final Category logger = new Category();
  private @Owning CloseableResource resource = new CloseableResource();

  @Override
  @EnsuresCalledMethods(value = "this.resource", methods = "close")
  public void close() {
    resource.close();
    logger.debug("after close");
  }
}

class Log4jWarnObject implements Closeable {
  private final Category logger = new Category();
  private @Owning CloseableResource resource = new CloseableResource();

  @Override
  @EnsuresCalledMethods(value = "this.resource", methods = "close")
  public void close() {
    resource.close();
    logger.warn("after close");
  }
}

class Log4jErrorObject implements Closeable {
  private final Category logger = new Category();
  private @Owning CloseableResource resource = new CloseableResource();

  @Override
  @EnsuresCalledMethods(value = "this.resource", methods = "close")
  public void close() {
    resource.close();
    logger.error("after close");
  }
}
