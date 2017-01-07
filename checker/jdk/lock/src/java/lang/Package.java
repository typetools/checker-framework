package java.lang;


import org.checkerframework.checker.lock.qual.*;


public class Package implements java.lang.reflect.AnnotatedElement{
  protected Package() {}
  public String getName() { throw new RuntimeException("skeleton method"); }
  public String getSpecificationTitle() { throw new RuntimeException("skeleton method"); }
  public String getSpecificationVersion() { throw new RuntimeException("skeleton method"); }
  public String getSpecificationVendor() { throw new RuntimeException("skeleton method"); }
  public String getImplementationTitle() { throw new RuntimeException("skeleton method"); }
  public String getImplementationVersion() { throw new RuntimeException("skeleton method"); }
  public String getImplementationVendor() { throw new RuntimeException("skeleton method"); }
   public boolean isSealed(@GuardSatisfied Package this) { throw new RuntimeException("skeleton method"); }
   public boolean isSealed(@GuardSatisfied Package this, java.net.@GuardSatisfied URL a1) { throw new RuntimeException("skeleton method"); }
   public boolean isCompatibleWith(@GuardSatisfied Package this,String a1) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
   public static Package getPackage(String a1) { throw new RuntimeException("skeleton method"); }
   public static Package[] getPackages() { throw new RuntimeException("skeleton method"); }
   public int hashCode(@GuardSatisfied Package this) { throw new RuntimeException("skeleton method"); }
   public String toString(@GuardSatisfied Package this) { throw new RuntimeException("skeleton method"); }
  public <A extends java.lang.annotation.Annotation> A getAnnotation(Class<A> a1) { throw new RuntimeException("skeleton method"); }
   public boolean isAnnotationPresent(@GuardSatisfied Package this,@GuardSatisfied Class<? extends java.lang.annotation.Annotation> a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.annotation.Annotation[] getAnnotations() { throw new RuntimeException("skeleton method"); }
  public java.lang.annotation.Annotation[] getDeclaredAnnotations() { throw new RuntimeException("skeleton method"); }
}
