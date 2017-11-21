package java.util;

import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.dataflow.qual.*;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class ResourceBundle{
  public static class Control{
    protected Control() {}
    public final static List<String> FORMAT_DEFAULT = new LinkedList<String>();
    public final static List<String> FORMAT_CLASS = new LinkedList<String>();
    public final static List<String> FORMAT_PROPERTIES = new LinkedList<String>();
    public final static long TTL_DONT_CACHE = -1;
    public final static long TTL_NO_EXPIRATION_CONTROL = -2;
    public final static ResourceBundle.Control getControl(List<String> a1) { throw new RuntimeException("skeleton method"); }
    public final static ResourceBundle.Control getNoFallbackControl(List<String> a1) { throw new RuntimeException("skeleton method"); }
    public List<String> getFormats(String a1) { throw new RuntimeException("skeleton method"); }
    public List<Locale> getCandidateLocales(String a1, Locale a2) { throw new RuntimeException("skeleton method"); }
    public Locale getFallbackLocale(String a1, Locale a2) { throw new RuntimeException("skeleton method"); }
    public ResourceBundle newBundle(String a1, Locale a2, String a3, ClassLoader a4, boolean a5) throws IllegalAccessException, InstantiationException, java.io.IOException { throw new RuntimeException("skeleton method"); }
    public long getTimeToLive(String a1, Locale a2) { throw new RuntimeException("skeleton method"); }
    public boolean needsReload(String a1, Locale a2, String a3, ClassLoader a4, ResourceBundle a5, long a6) { throw new RuntimeException("skeleton method"); }
    public String toBundleName(String a1, Locale a2) { throw new RuntimeException("skeleton method"); }
    public final String toResourceName(String a1, String a2) { throw new RuntimeException("skeleton method"); }
  }
  public ResourceBundle() { throw new RuntimeException("skeleton method"); }
  public final String getString(String a1) { throw new RuntimeException("skeleton method"); }
  public final String[] getStringArray(String a1) { throw new RuntimeException("skeleton method"); }
  public final Object getObject(String a1) { throw new RuntimeException("skeleton method"); }
  public Locale getLocale() { throw new RuntimeException("skeleton method"); }
  public final static ResourceBundle getBundle(String a1) { throw new RuntimeException("skeleton method"); }
  public final static ResourceBundle getBundle(String a1, ResourceBundle.Control a2) { throw new RuntimeException("skeleton method"); }
  public final static ResourceBundle getBundle(String a1, Locale a2) { throw new RuntimeException("skeleton method"); }
  public final static ResourceBundle getBundle(String a1, Locale a2, ResourceBundle.Control a3) { throw new RuntimeException("skeleton method"); }
  public static ResourceBundle getBundle(String a1, Locale a2, ClassLoader a3) { throw new RuntimeException("skeleton method"); }
  public static ResourceBundle getBundle(String a1, Locale a2, ClassLoader a3, ResourceBundle.Control a4) { throw new RuntimeException("skeleton method"); }
  public final static void clearCache() { throw new RuntimeException("skeleton method"); }
  public final static void clearCache(ClassLoader a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public abstract Enumeration<String> getKeys();
  @Pure public boolean containsKey(String a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public Set<@KeyFor("this") String> keySet() { throw new RuntimeException("skeleton method"); }
}
