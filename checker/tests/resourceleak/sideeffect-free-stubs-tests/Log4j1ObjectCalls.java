// This test covers the Log4j 1.x API in package org.apache.log4j.
// The RLC-specific stub marks logging methods as @SideEffectFree, so logging after a resource is
// closed should not wipe out the close fact.

import java.io.Closeable;
import org.apache.log4j.Logger;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.qual.Owning;

class Log4j1DebugObject implements Closeable {
  private final Logger logger = Logger.getLogger("Log4j1DebugObject");
  private @Owning CloseableResource resource = new CloseableResource();

  @Override
  @EnsuresCalledMethods(value = "this.resource", methods = "close")
  public void close() {
    resource.close();
    logger.debug("after close");
  }
}

class Log4j1DebugWithThrowable implements Closeable {
  private final Logger logger = Logger.getLogger("Log4j1DebugWithThrowable");
  private @Owning CloseableResource resource = new CloseableResource();

  @Override
  @EnsuresCalledMethods(value = "this.resource", methods = "close")
  public void close() {
    resource.close();
    logger.debug("after close", new RuntimeException());
  }
}

class Log4j1InfoObject implements Closeable {
  private final Logger logger = Logger.getLogger("Log4j1InfoObject");
  private @Owning CloseableResource resource = new CloseableResource();

  @Override
  @EnsuresCalledMethods(value = "this.resource", methods = "close")
  public void close() {
    resource.close();
    logger.info("after close");
  }
}

class Log4j1WarnObject implements Closeable {
  private final Logger logger = Logger.getLogger("Log4j1WarnObject");
  private @Owning CloseableResource resource = new CloseableResource();

  @Override
  @EnsuresCalledMethods(value = "this.resource", methods = "close")
  public void close() {
    resource.close();
    logger.warn("after close");
  }
}

class Log4j1ErrorObject implements Closeable {
  private final Logger logger = Logger.getLogger("Log4j1ErrorObject");
  private @Owning CloseableResource resource = new CloseableResource();

  @Override
  @EnsuresCalledMethods(value = "this.resource", methods = "close")
  public void close() {
    resource.close();
    logger.error("after close");
  }
}
