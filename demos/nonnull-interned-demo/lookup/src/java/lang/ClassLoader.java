package java.lang;

import checkers.quals.*;

public abstract class ClassLoader{
  public Class<?> loadClass(String a1) throws ClassNotFoundException { throw new RuntimeException("skeleton method"); }
  public java.net.URL getResource(String a1) { throw new RuntimeException("skeleton method"); }
  public java.util.Enumeration<java.net.URL> getResources(String a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public static java.net.URL getSystemResource(String a1) { throw new RuntimeException("skeleton method"); }
  public static java.util.Enumeration<java.net.URL> getSystemResources(String a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public java.io.InputStream getResourceAsStream(String a1) { throw new RuntimeException("skeleton method"); }
  public static java.io.InputStream getSystemResourceAsStream(String a1) { throw new RuntimeException("skeleton method"); }
  public final ClassLoader getParent() { throw new RuntimeException("skeleton method"); }
  public static ClassLoader getSystemClassLoader() { throw new RuntimeException("skeleton method"); }
  public synchronized void setDefaultAssertionStatus(boolean a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void setPackageAssertionStatus(String a1, boolean a2) { throw new RuntimeException("skeleton method"); }
  public synchronized void setClassAssertionStatus(String a1, boolean a2) { throw new RuntimeException("skeleton method"); }
  public synchronized void clearAssertionStatus() { throw new RuntimeException("skeleton method"); }
protected final @NonNull Class<?> defineClass(@NonNull byte[] b, int off, int len)
	throws ClassFormatError { throw new RuntimeException("skeleton method"); }
protected final @NonNull Class<?> defineClass(@NonNull String name, @NonNull byte[] b, int off, int len)
	throws ClassFormatError { throw new RuntimeException("skeleton method"); }
protected final void resolveClass(@NonNull Class<?> c) { throw new RuntimeException("skeleton method"); }

}
