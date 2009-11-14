package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public abstract class ClassLoader{
  protected ClassLoader() {}
  public java.lang.Class<?> loadClass(java.lang.String a1) throws java.lang.ClassNotFoundException { throw new RuntimeException("skeleton method"); }
  protected java.lang.Class<?> loadClass(java.lang.String a1, boolean a2) throws java.lang.ClassNotFoundException { throw new RuntimeException("skeleton method"); }
  public @Nullable java.net.URL getResource(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public java.util.Enumeration<java.net.URL> getResources(java.lang.String a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public static @Nullable java.net.URL getSystemResource(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public static java.util.Enumeration<java.net.URL> getSystemResources(java.lang.String a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public @Nullable java.io.InputStream getResourceAsStream(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public static @Nullable java.io.InputStream getSystemResourceAsStream(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public final @Nullable java.lang.ClassLoader getParent() { throw new RuntimeException("skeleton method"); }
  public static @Nullable java.lang.ClassLoader getSystemClassLoader() { throw new RuntimeException("skeleton method"); }
  public synchronized void setDefaultAssertionStatus(boolean a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void setPackageAssertionStatus(@Nullable java.lang.String a1, boolean a2) { throw new RuntimeException("skeleton method"); }
  public synchronized void setClassAssertionStatus(java.lang.String a1, boolean a2) { throw new RuntimeException("skeleton method"); }
  public synchronized void clearAssertionStatus() { throw new RuntimeException("skeleton method"); }
  protected Class<?> defineClass(byte[] b, int off, int len) throws ClassFormatError { throw new RuntimeException("skeleton method"); }
  protected Class<?> defineClass(@Nullable String name, byte[] b, int off, int len) throws ClassFormatError { throw new RuntimeException("skeleton method"); }
  protected Class<?> defineClass(@Nullable String name, byte[] b, int off, int len, @Nullable java.security.ProtectionDomain protectionDomain) throws ClassFormatError { throw new RuntimeException("skeleton method"); }
  protected Class<?> defineClass(@Nullable String name, java.nio.ByteBuffer b, @Nullable java.security.ProtectionDomain protectionDomain) throws ClassFormatError { throw new RuntimeException("skeleton method"); }
  protected void resolveClass(@NonNull Class<?> c) { throw new RuntimeException("skeleton method"); }
    protected final Class<?> findLoadedClass(String name) { throw new RuntimeException("skeleton method"); }
    
}
