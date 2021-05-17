// Based on a number of false positives in Zookeeper that all use this pattern to reflectively
// initialize a class. -AresolveReflection fixes this version, but most (4/5) of the failures in
// Zookeeper
// persist even with that flag, which also imposes about a 50% perf overhead. So these are now
// expected warnings.

import java.io.*;
import java.lang.reflect.Constructor;

class ClassForNameInit {

  public static InputStream inputStreamFactory() throws Exception {
    // FYI this code will always fail if you run it, so don't.
    // There's no ByteArrayInputStream constructor that takes no arguments.
    Class<?> baisClass = Class.forName("java.io.ByteArrayInputStream");
    Object bais = baisClass.getConstructor().newInstance();
    return (InputStream) bais;
  }

  public static Object objectFactory() throws Exception {
    Class<?> objClass = Class.forName("java.lang.Object");
    Object obj = objClass.getConstructor().newInstance();
    return (Object) obj;
  }

  private static Object getAuditLogger(String auditLoggerClass) {
    if (auditLoggerClass == null) {
      auditLoggerClass = Object.class.getName();
    }
    try {
      Constructor<?> clientCxnConstructor =
          Class.forName(auditLoggerClass).getDeclaredConstructor();
      Object auditLogger = (Object) clientCxnConstructor.newInstance();
      return auditLogger;
    } catch (Exception e) {
      throw new RuntimeException("Couldn't instantiate " + auditLoggerClass, e);
    }
  }
}
