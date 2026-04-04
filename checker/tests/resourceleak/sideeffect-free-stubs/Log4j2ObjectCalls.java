// This test covers the Log4j 2.x API in package org.apache.logging.log4j.
// The local Logger and LogManager declarations below are tiny stand-ins for the
// real library API. The RLC-specific stub marks Logger.debug/warn/error(Object)
// as @SideEffectFree, so logging after a resource is closed should not wipe out
// the close fact.

package org.apache.logging.log4j;

import java.io.Closeable;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.qual.Owning;

interface Logger {
  void debug(Object message);

  void warn(Object message);

  void error(Object message);
}

final class SimpleLogger implements Logger {
  @Override
  public void debug(Object message) {}

  @Override
  public void warn(Object message) {}

  @Override
  public void error(Object message) {}
}

final class LogManager {
  private LogManager() {}

  static Logger getLogger() {
    return new SimpleLogger();
  }
}

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
