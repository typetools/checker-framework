package java.lang;

import checkers.quals.*;

public abstract class ClassLoader{
  public java.lang.Class<?> loadClass(java.lang.String a1) throws java.lang.ClassNotFoundException { throw new RuntimeException("skeleton method"); }
  public java.net.URL getResource(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public java.util.Enumeration<java.net.URL> getResources(java.lang.String a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public static java.net.URL getSystemResource(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public static java.util.Enumeration<java.net.URL> getSystemResources(java.lang.String a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public java.io.InputStream getResourceAsStream(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public static java.io.InputStream getSystemResourceAsStream(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public final java.lang.ClassLoader getParent() { throw new RuntimeException("skeleton method"); }
  public static java.lang.ClassLoader getSystemClassLoader() { throw new RuntimeException("skeleton method"); }
  public synchronized void setDefaultAssertionStatus(boolean a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void setPackageAssertionStatus(java.lang.String a1, boolean a2) { throw new RuntimeException("skeleton method"); }
  public synchronized void setClassAssertionStatus(java.lang.String a1, boolean a2) { throw new RuntimeException("skeleton method"); }
  public synchronized void clearAssertionStatus() { throw new RuntimeException("skeleton method"); }
protected final @NonNull Class<?> defineClass(@NonNull byte[] b, int off, int len)
	throws ClassFormatError { throw new RuntimeException("skeleton method"); }
protected final @NonNull Class<?> defineClass(@NonNull String name, @NonNull byte[] b, int off, int len)
	throws ClassFormatError { throw new RuntimeException("skeleton method"); }
protected final void resolveClass(@NonNull Class<?> c) { throw new RuntimeException("skeleton method"); }

}
