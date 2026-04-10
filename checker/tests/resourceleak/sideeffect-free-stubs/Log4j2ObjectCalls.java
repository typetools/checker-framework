// This test covers the Log4j 2.x API in package org.apache.logging.log4j.
// real library API. The RLC-specific stub marks logging methods as @SideEffectFree, so logging
// after a resource is closed should not wipe out the close fact.

import java.io.Closeable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.qual.Owning;

final class CloseableResource implements Closeable {
  @Override
  public void close() {}
}

class Log4j2DebugObject implements Closeable {
  private static final Logger logger = LogManager.getLogger();
  private @Owning CloseableResource resource = new CloseableResource();

  @Override
  @EnsuresCalledMethods(value = "this.resource", methods = "close")
  public void close() {
    resource.close();
    logger.debug("after close");
  }
}

class Log4j2DebugVarargs implements Closeable {
  private static final Logger logger = LogManager.getLogger();
  private @Owning CloseableResource resource = new CloseableResource();

  @Override
  @EnsuresCalledMethods(value = "this.resource", methods = "close")
  public void close() {
    resource.close();
    logger.debug("closed resource {}", "name");
  }
}

class Log4j2InfoObject implements Closeable {
  private static final Logger logger = LogManager.getLogger();
  private @Owning CloseableResource resource = new CloseableResource();

  @Override
  @EnsuresCalledMethods(value = "this.resource", methods = "close")
  public void close() {
    resource.close();
    logger.info("after close");
  }
}

class Log4j2WarnObject implements Closeable {
  private static final Logger logger = LogManager.getLogger();
  private @Owning CloseableResource resource = new CloseableResource();

  @Override
  @EnsuresCalledMethods(value = "this.resource", methods = "close")
  public void close() {
    resource.close();
    logger.warn("after close");
  }
}

class Log4j2ErrorObject implements Closeable {
  private static final Logger logger = LogManager.getLogger();
  private @Owning CloseableResource resource = new CloseableResource();

  @Override
  @EnsuresCalledMethods(value = "this.resource", methods = "close")
  public void close() {
    resource.close();
    logger.error("after close");
  }
}

class Log4j2ErrorWithThrowable implements Closeable {
  private static final Logger logger = LogManager.getLogger();
  private @Owning CloseableResource resource = new CloseableResource();

  @Override
  @EnsuresCalledMethods(value = "this.resource", methods = "close")
  public void close() {
    resource.close();
    logger.error("after close", new RuntimeException());
  }
}
