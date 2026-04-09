// This test covers the Log4j 1.x API in package org.apache.log4j.
// The local Logger class below is just a tiny stand-in for the real library API.
// The RLC-specific stub marks logging methods as @SideEffectFree, so
// logging after a resource is closed should not wipe out the close fact.

package org.apache.log4j;

import java.io.Closeable;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.qual.Owning;

class Logger {
  public void debug(Object message) {}

  public void debug(Object message, Throwable t) {}

  public void info(Object message) {}

  public void warn(Object message) {}

  public void error(Object message) {}
}

final class CloseableResource implements Closeable {
  @Override
  public void close() {}
}

class Log4j1DebugObject implements Closeable {
  private final Logger logger = new Logger();
  private @Owning CloseableResource resource = new CloseableResource();

  @Override
  @EnsuresCalledMethods(value = "this.resource", methods = "close")
  public void close() {
    resource.close();
    logger.debug("after close");
  }
}

class Log4j1DebugWithThrowable implements Closeable {
  private final Logger logger = new Logger();
  private @Owning CloseableResource resource = new CloseableResource();

  @Override
  @EnsuresCalledMethods(value = "this.resource", methods = "close")
  public void close() {
    resource.close();
    logger.debug("after close", new RuntimeException());
  }
}

class Log4j1InfoObject implements Closeable {
  private final Logger logger = new Logger();
  private @Owning CloseableResource resource = new CloseableResource();

  @Override
  @EnsuresCalledMethods(value = "this.resource", methods = "close")
  public void close() {
    resource.close();
    logger.info("after close");
  }
}

class Log4j1WarnObject implements Closeable {
  private final Logger logger = new Logger();
  private @Owning CloseableResource resource = new CloseableResource();

  @Override
  @EnsuresCalledMethods(value = "this.resource", methods = "close")
  public void close() {
    resource.close();
    logger.warn("after close");
  }
}

class Log4j1ErrorObject implements Closeable {
  private final Logger logger = new Logger();
  private @Owning CloseableResource resource = new CloseableResource();

  @Override
  @EnsuresCalledMethods(value = "this.resource", methods = "close")
  public void close() {
    resource.close();
    logger.error("after close");
  }
}
