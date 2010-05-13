package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class Package implements java.lang.reflect.AnnotatedElement{
  protected Package() {}
  public String getName() { throw new RuntimeException("skeleton method"); }
  public @Nullable String getSpecificationTitle() { throw new RuntimeException("skeleton method"); }
  public @Nullable String getSpecificationVersion() { throw new RuntimeException("skeleton method"); }
  public @Nullable String getSpecificationVendor() { throw new RuntimeException("skeleton method"); }
  public @Nullable String getImplementationTitle() { throw new RuntimeException("skeleton method"); }
  public @Nullable String getImplementationVersion() { throw new RuntimeException("skeleton method"); }
  public String getImplementationVendor() { throw new RuntimeException("skeleton method"); }
  public boolean isSealed() { throw new RuntimeException("skeleton method"); }
  public boolean isSealed(java.net.URL a1) { throw new RuntimeException("skeleton method"); }
  public boolean isCompatibleWith(String a1) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  public static @Pure @Nullable Package getPackage(String a1) { throw new RuntimeException("skeleton method"); }
  public static @Pure Package[] getPackages() { throw new RuntimeException("skeleton method"); }
  public int hashCode() { throw new RuntimeException("skeleton method"); }
  public String toString() { throw new RuntimeException("skeleton method"); }
  public <A extends java.lang.annotation.Annotation> @Nullable A getAnnotation(Class<A> a1) { throw new RuntimeException("skeleton method"); }
  public boolean isAnnotationPresent(Class<? extends java.lang.annotation.Annotation> a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.annotation.Annotation[] getAnnotations() { throw new RuntimeException("skeleton method"); }
  public java.lang.annotation.Annotation[] getDeclaredAnnotations() { throw new RuntimeException("skeleton method"); }
}
