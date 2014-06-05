package java.util;
import org.checkerframework.checker.javari.qual.*;

import java.io.*;

public final class Locale implements Cloneable, Serializable {
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

    public Locale(String language, String country, String variant) {
        throw new RuntimeException("skeleton method");
    }

    public Locale(String language, String country) {
        throw new RuntimeException("skeleton method");
    }

    public Locale(String language) {
        throw new RuntimeException("skeleton method");
    }

    static Locale getInstance(String language, String country, String variant) {
        throw new RuntimeException("skeleton method");
    }

    public static Locale getDefault() {
        throw new RuntimeException("skeleton method");
    }

    public static synchronized void setDefault(Locale newLocale) {
        throw new RuntimeException("skeleton method");
    }

    public static Locale[] getAvailableLocales() {
        throw new RuntimeException("skeleton method");
    }

    public static String[] getISOCountries() {
        throw new RuntimeException("skeleton method");
    }

    public static String[] getISOLanguages() {
        throw new RuntimeException("skeleton method");
    }

    public String getLanguage(@ReadOnly Locale this) {
        throw new RuntimeException("skeleton method");
    }

    public String getCountry(@ReadOnly Locale this) {
        throw new RuntimeException("skeleton method");
    }

    public String getVariant(@ReadOnly Locale this) {
        throw new RuntimeException("skeleton method");
    }

    public final String toString(@ReadOnly Locale this) {
        throw new RuntimeException("skeleton method");
    }

    public String getISO3Language(@ReadOnly Locale this) throws MissingResourceException {
        throw new RuntimeException("skeleton method");
    }

    public String getISO3Country(@ReadOnly Locale this) throws MissingResourceException {
        throw new RuntimeException("skeleton method");
    }

    public final String getDisplayLanguage() {
        throw new RuntimeException("skeleton method");
    }

    public String getDisplayLanguage(@ReadOnly Locale this, Locale inLocale) {
        throw new RuntimeException("skeleton method");
    }

    public final String getDisplayCountry() {
        throw new RuntimeException("skeleton method");
    }

    public String getDisplayCountry(@ReadOnly Locale this, Locale inLocale) {
        throw new RuntimeException("skeleton method");
    }

    public final String getDisplayVariant() {
        throw new RuntimeException("skeleton method");
    }

    public String getDisplayVariant(@ReadOnly Locale this, Locale inLocale) {
        throw new RuntimeException("skeleton method");
    }

    public final String getDisplayName() {
        return getDisplayName(getDefault());
    }

    public String getDisplayName(@ReadOnly Locale this, Locale inLocale) {
        throw new RuntimeException("skeleton method");
    }

    public Object clone() {
        throw new RuntimeException("skeleton method");
    }

    public int hashCode() {
        throw new RuntimeException("skeleton method");
    }

    public boolean equals(@ReadOnly Locale this, @ReadOnly Object obj) {
        throw new RuntimeException("skeleton method");
    }
}
