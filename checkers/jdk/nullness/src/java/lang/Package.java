package java.lang;

import dataflow.quals.Pure;

import checkers.nullness.quals.Nullable;


public class Package implements java.lang.reflect.AnnotatedElement{
  protected Package() {}
  public String getName() { throw new RuntimeException("skeleton method"); }
  public @Nullable String getSpecificationTitle() { throw new RuntimeException("skeleton method"); }
  public @Nullable String getSpecificationVersion() { throw new RuntimeException("skeleton method"); }
  public @Nullable String getSpecificationVendor() { throw new RuntimeException("skeleton method"); }
  public @Nullable String getImplementationTitle() { throw new RuntimeException("skeleton method"); }
  public @Nullable String getImplementationVersion() { throw new RuntimeException("skeleton method"); }
  public String getImplementationVendor() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean isSealed() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean isSealed(java.net.URL a1) { throw new RuntimeException("skeleton method"); }
  @Pure public boolean isCompatibleWith(String a1) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  public static @Pure @Nullable Package getPackage(String a1) { throw new RuntimeException("skeleton method"); }
  public static @Pure Package[] getPackages() { throw new RuntimeException("skeleton method"); }
  @Pure public int hashCode() { throw new RuntimeException("skeleton method"); }
  @Pure public String toString() { throw new RuntimeException("skeleton method"); }
  public <A extends java.lang.annotation.Annotation> @Nullable A getAnnotation(Class<A> a1) { throw new RuntimeException("skeleton method"); }
  @Pure public boolean isAnnotationPresent(Class<? extends java.lang.annotation.Annotation> a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.annotation.Annotation[] getAnnotations() { throw new RuntimeException("skeleton method"); }
  public java.lang.annotation.Annotation[] getDeclaredAnnotations() { throw new RuntimeException("skeleton method"); }
}
