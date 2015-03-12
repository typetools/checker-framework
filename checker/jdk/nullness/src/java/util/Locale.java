package java.util;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class Locale implements Cloneable, java.io.Serializable {
  private static final long serialVersionUID = 0L;
  public final static Locale ENGLISH = null;
  public final static Locale FRENCH = null;
  public final static Locale GERMAN = null;
  public final static Locale ITALIAN = null;
  public final static Locale JAPANESE = null;
  public final static Locale KOREAN = null;
  public final static Locale CHINESE = null;
  public final static Locale SIMPLIFIED_CHINESE = null;
  public final static Locale TRADITIONAL_CHINESE = null;
  public final static Locale FRANCE = null;
  public final static Locale GERMANY = null;
  public final static Locale ITALY = null;
  public final static Locale JAPAN = null;
  public final static Locale KOREA = null;
  public final static Locale CHINA = null;
  public final static Locale PRC = null;
  public final static Locale TAIWAN = null;
  public final static Locale UK = null;
  public final static Locale US = null;
  public final static Locale CANADA = null;
  public final static Locale CANADA_FRENCH = null;
  public final static Locale ROOT = null;
  public Locale(String a1, String a2, String a3) { throw new RuntimeException("skeleton method"); }
  public Locale(String a1, String a2) { throw new RuntimeException("skeleton method"); }
  public Locale(String a1) { throw new RuntimeException("skeleton method"); }
  public static Locale getDefault() { throw new RuntimeException("skeleton method"); }
  public static synchronized void setDefault(Locale a1) { throw new RuntimeException("skeleton method"); }
  public static Locale[] getAvailableLocales() { throw new RuntimeException("skeleton method"); }
  public static String[] getISOCountries() { throw new RuntimeException("skeleton method"); }
  public static String[] getISOLanguages() { throw new RuntimeException("skeleton method"); }
  public String getLanguage() { throw new RuntimeException("skeleton method"); }
  public String getCountry() { throw new RuntimeException("skeleton method"); }
  public String getVariant() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public final String toString() { throw new RuntimeException("skeleton method"); }
  public String getISO3Language() throws MissingResourceException { throw new RuntimeException("skeleton method"); }
  public String getISO3Country() throws MissingResourceException { throw new RuntimeException("skeleton method"); }
  public final String getDisplayLanguage() { throw new RuntimeException("skeleton method"); }
  public String getDisplayLanguage(Locale a1) { throw new RuntimeException("skeleton method"); }
  public final String getDisplayCountry() { throw new RuntimeException("skeleton method"); }
  public String getDisplayCountry(Locale a1) { throw new RuntimeException("skeleton method"); }
  public final String getDisplayVariant() { throw new RuntimeException("skeleton method"); }
  public String getDisplayVariant(Locale a1) { throw new RuntimeException("skeleton method"); }
  public final String getDisplayName() { throw new RuntimeException("skeleton method"); }
  public String getDisplayName(Locale a1) { throw new RuntimeException("skeleton method"); }
  @Pure public int hashCode() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean equals(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public Object clone() { throw new RuntimeException("skeleton method"); }
}
