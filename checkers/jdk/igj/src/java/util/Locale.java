package java.util;
import checkers.igj.quals.*;

@Immutable
public final class Locale implements @Immutable Cloneable, @Immutable java.io.Serializable {
    private static final long serialVersionUID = 0L;
  public final static Locale ENGLISH;
  public final static Locale FRENCH;
  public final static Locale GERMAN;
  public final static Locale ITALIAN;
  public final static Locale JAPANESE;
  public final static Locale KOREAN;
  public final static Locale CHINESE;
  public final static Locale SIMPLIFIED_CHINESE;
  public final static Locale TRADITIONAL_CHINESE;
  public final static Locale FRANCE;
  public final static Locale GERMANY;
  public final static Locale ITALY;
  public final static Locale JAPAN;
  public final static Locale KOREA;
  public final static Locale CHINA;
  public final static Locale PRC;
  public final static Locale TAIWAN;
  public final static Locale UK;
  public final static Locale US;
  public final static Locale CANADA;
  public final static Locale CANADA_FRENCH;
  public final static Locale ROOT;
  public Locale(@AssignsFields Locale this, String a1, String a2, String a3) { throw new RuntimeException("skeleton method"); }
  public Locale(@AssignsFields Locale this, String a1, String a2) { throw new RuntimeException("skeleton method"); }
  public Locale(@AssignsFields Locale this, String a1) { throw new RuntimeException("skeleton method"); }
  public static Locale getDefault() { throw new RuntimeException("skeleton method"); }
  public static synchronized void setDefault(Locale a1) { throw new RuntimeException("skeleton method"); }
  public static Locale @ReadOnly [] getAvailableLocales() { throw new RuntimeException("skeleton method"); }
  public static String @ReadOnly [] getISOCountries() { throw new RuntimeException("skeleton method"); }
  public static String @ReadOnly [] getISOLanguages() { throw new RuntimeException("skeleton method"); }
  public String getLanguage() { throw new RuntimeException("skeleton method"); }
  public String getCountry() { throw new RuntimeException("skeleton method"); }
  public String getVariant() { throw new RuntimeException("skeleton method"); }
  public final String toString() { throw new RuntimeException("skeleton method"); }
  public String getISO3Language()throws MissingResourceException { throw new RuntimeException("skeleton method"); }
  public String getISO3Country()throws MissingResourceException { throw new RuntimeException("skeleton method"); }
  public final String getDisplayLanguage() { throw new RuntimeException("skeleton method"); }
  public String getDisplayLanguage(Locale a1) { throw new RuntimeException("skeleton method"); }
  public final String getDisplayCountry() { throw new RuntimeException("skeleton method"); }
  public String getDisplayCountry(Locale a1) { throw new RuntimeException("skeleton method"); }
  public final String getDisplayVariant() { throw new RuntimeException("skeleton method"); }
  public String getDisplayVariant(Locale a1) { throw new RuntimeException("skeleton method"); }
  public final String getDisplayName() { throw new RuntimeException("skeleton method"); }
  public String getDisplayName(Locale a1) { throw new RuntimeException("skeleton method"); }
  public int hashCode() { throw new RuntimeException("skeleton method"); }
  public boolean equals(@ReadOnly Object a1) { throw new RuntimeException("skeleton method"); }
  public @Immutable Object clone() { throw new RuntimeException("skeleton method"); }
}
