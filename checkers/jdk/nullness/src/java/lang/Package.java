package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class Package implements java.lang.reflect.AnnotatedElement{
  protected Package() {}
  public java.lang.String getName() { throw new RuntimeException("skeleton method"); }
  public @Nullable java.lang.String getSpecificationTitle() { throw new RuntimeException("skeleton method"); }
  public @Nullable java.lang.String getSpecificationVersion() { throw new RuntimeException("skeleton method"); }
  public @Nullable java.lang.String getSpecificationVendor() { throw new RuntimeException("skeleton method"); }
  public @Nullable java.lang.String getImplementationTitle() { throw new RuntimeException("skeleton method"); }
  public @Nullable java.lang.String getImplementationVersion() { throw new RuntimeException("skeleton method"); }
  public java.lang.String getImplementationVendor() { throw new RuntimeException("skeleton method"); }
  public boolean isSealed() { throw new RuntimeException("skeleton method"); }
  public boolean isSealed(java.net.URL a1) { throw new RuntimeException("skeleton method"); }
  public boolean isCompatibleWith(java.lang.String a1) throws java.lang.NumberFormatException { throw new RuntimeException("skeleton method"); }
  public static @Nullable java.lang.Package getPackage(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public static java.lang.Package[] getPackages() { throw new RuntimeException("skeleton method"); }
  public int hashCode() { throw new RuntimeException("skeleton method"); }
  public java.lang.String toString() { throw new RuntimeException("skeleton method"); }
  public <A extends java.lang.annotation.Annotation> @Nullable A getAnnotation(java.lang.Class<A> a1) { throw new RuntimeException("skeleton method"); }
  public boolean isAnnotationPresent(java.lang.Class<? extends java.lang.annotation.Annotation> a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.annotation.Annotation[] getAnnotations() { throw new RuntimeException("skeleton method"); }
  public java.lang.annotation.Annotation[] getDeclaredAnnotations() { throw new RuntimeException("skeleton method"); }
}
