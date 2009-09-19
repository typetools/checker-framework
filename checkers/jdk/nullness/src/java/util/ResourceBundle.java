package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public abstract class ResourceBundle{
  public static class Control{
      public final static java.util.List<java.lang.String> FORMAT_DEFAULT = new java.util.LinkedList<java.lang.String>();
      public final static java.util.List<java.lang.String> FORMAT_CLASS = new java.util.LinkedList<java.lang.String>();
      public final static java.util.List<java.lang.String> FORMAT_PROPERTIES = new java.util.LinkedList<java.lang.String>();
    public final static long TTL_DONT_CACHE = -1;
    public final static long TTL_NO_EXPIRATION_CONTROL = -2;
    public final static java.util.ResourceBundle.Control getControl(java.util.List<java.lang.String> a1) { throw new RuntimeException("skeleton method"); }
    public final static java.util.ResourceBundle.Control getNoFallbackControl(java.util.List<java.lang.String> a1) { throw new RuntimeException("skeleton method"); }
    public java.util.List<java.lang.String> getFormats(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
    public java.util.List<java.util.Locale> getCandidateLocales(java.lang.String a1, java.util.Locale a2) { throw new RuntimeException("skeleton method"); }
    public java.util.Locale getFallbackLocale(java.lang.String a1, java.util.Locale a2) { throw new RuntimeException("skeleton method"); }
    public java.util.ResourceBundle newBundle(java.lang.String a1, java.util.Locale a2, java.lang.String a3, java.lang.ClassLoader a4, boolean a5)throws java.lang.IllegalAccessException, java.lang.InstantiationException, java.io.IOException { throw new RuntimeException("skeleton method"); }
    public long getTimeToLive(java.lang.String a1, java.util.Locale a2) { throw new RuntimeException("skeleton method"); }
    public boolean needsReload(java.lang.String a1, java.util.Locale a2, java.lang.String a3, java.lang.ClassLoader a4, java.util.ResourceBundle a5, long a6) { throw new RuntimeException("skeleton method"); }
    public java.lang.String toBundleName(java.lang.String a1, java.util.Locale a2) { throw new RuntimeException("skeleton method"); }
    public final java.lang.String toResourceName(java.lang.String a1, java.lang.String a2) { throw new RuntimeException("skeleton method"); }
  }
  public ResourceBundle() { throw new RuntimeException("skeleton method"); }
  public final java.lang.String getString(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public final java.lang.String[] getStringArray(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public final java.lang.Object getObject(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public java.util.Locale getLocale() { throw new RuntimeException("skeleton method"); }
  public final static java.util.ResourceBundle getBundle(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public final static java.util.ResourceBundle getBundle(java.lang.String a1, java.util.ResourceBundle.Control a2) { throw new RuntimeException("skeleton method"); }
  public final static java.util.ResourceBundle getBundle(java.lang.String a1, java.util.Locale a2) { throw new RuntimeException("skeleton method"); }
  public final static java.util.ResourceBundle getBundle(java.lang.String a1, java.util.Locale a2, java.util.ResourceBundle.Control a3) { throw new RuntimeException("skeleton method"); }
  public static java.util.ResourceBundle getBundle(java.lang.String a1, java.util.Locale a2, java.lang.ClassLoader a3) { throw new RuntimeException("skeleton method"); }
  public static java.util.ResourceBundle getBundle(java.lang.String a1, java.util.Locale a2, java.lang.ClassLoader a3, java.util.ResourceBundle.Control a4) { throw new RuntimeException("skeleton method"); }
  public final static void clearCache() { throw new RuntimeException("skeleton method"); }
  public final static void clearCache(java.lang.ClassLoader a1) { throw new RuntimeException("skeleton method"); }
  public abstract java.util.Enumeration<java.lang.String> getKeys();
  public boolean containsKey(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public java.util.Set<java.lang.String> keySet() { throw new RuntimeException("skeleton method"); }
}
