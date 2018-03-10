package java.lang;

import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.nullness.qual.Nullable;


public final class Character implements java.io.Serializable, Comparable<Character> {
  private static final long serialVersionUID = 0;
  public static class Subset{
    protected Subset() {}
    @Pure protected Subset(String name) { throw new RuntimeException("skeleton method"); }
    @Pure public final boolean equals(@Nullable Object obj) { throw new RuntimeException("skeleton method"); }
    @Pure public final int hashCode() { throw new RuntimeException("skeleton method"); }
    @SideEffectFree public final String toString() { throw new RuntimeException("skeleton method"); }
  }
  public static final class UnicodeBlock extends Subset{
    public static final Character.UnicodeBlock BASIC_LATIN = new UnicodeBlock();
    public static final Character.UnicodeBlock LATIN_1_SUPPLEMENT = new UnicodeBlock();
    public static final Character.UnicodeBlock LATIN_EXTENDED_A = new UnicodeBlock();
    public static final Character.UnicodeBlock LATIN_EXTENDED_B = new UnicodeBlock();
    public static final Character.UnicodeBlock IPA_EXTENSIONS = new UnicodeBlock();
    public static final Character.UnicodeBlock SPACING_MODIFIER_LETTERS = new UnicodeBlock();
    public static final Character.UnicodeBlock COMBINING_DIACRITICAL_MARKS = new UnicodeBlock();
    public static final Character.UnicodeBlock GREEK = new UnicodeBlock();
    public static final Character.UnicodeBlock CYRILLIC = new UnicodeBlock();
    public static final Character.UnicodeBlock ARMENIAN = new UnicodeBlock();
    public static final Character.UnicodeBlock HEBREW = new UnicodeBlock();
    public static final Character.UnicodeBlock ARABIC = new UnicodeBlock();
    public static final Character.UnicodeBlock DEVANAGARI = new UnicodeBlock();
    public static final Character.UnicodeBlock BENGALI = new UnicodeBlock();
    public static final Character.UnicodeBlock GURMUKHI = new UnicodeBlock();
    public static final Character.UnicodeBlock GUJARATI = new UnicodeBlock();
    public static final Character.UnicodeBlock ORIYA = new UnicodeBlock();
    public static final Character.UnicodeBlock TAMIL = new UnicodeBlock();
    public static final Character.UnicodeBlock TELUGU = new UnicodeBlock();
    public static final Character.UnicodeBlock KANNADA = new UnicodeBlock();
    public static final Character.UnicodeBlock MALAYALAM = new UnicodeBlock();
    public static final Character.UnicodeBlock THAI = new UnicodeBlock();
    public static final Character.UnicodeBlock LAO = new UnicodeBlock();
    public static final Character.UnicodeBlock TIBETAN = new UnicodeBlock();
    public static final Character.UnicodeBlock GEORGIAN = new UnicodeBlock();
    public static final Character.UnicodeBlock HANGUL_JAMO = new UnicodeBlock();
    public static final Character.UnicodeBlock LATIN_EXTENDED_ADDITIONAL = new UnicodeBlock();
    public static final Character.UnicodeBlock GREEK_EXTENDED = new UnicodeBlock();
    public static final Character.UnicodeBlock GENERAL_PUNCTUATION = new UnicodeBlock();
    public static final Character.UnicodeBlock SUPERSCRIPTS_AND_SUBSCRIPTS = new UnicodeBlock();
    public static final Character.UnicodeBlock CURRENCY_SYMBOLS = new UnicodeBlock();
    public static final Character.UnicodeBlock COMBINING_MARKS_FOR_SYMBOLS = new UnicodeBlock();
    public static final Character.UnicodeBlock LETTERLIKE_SYMBOLS = new UnicodeBlock();
    public static final Character.UnicodeBlock NUMBER_FORMS = new UnicodeBlock();
    public static final Character.UnicodeBlock ARROWS = new UnicodeBlock();
    public static final Character.UnicodeBlock MATHEMATICAL_OPERATORS = new UnicodeBlock();
    public static final Character.UnicodeBlock MISCELLANEOUS_TECHNICAL = new UnicodeBlock();
    public static final Character.UnicodeBlock CONTROL_PICTURES = new UnicodeBlock();
    public static final Character.UnicodeBlock OPTICAL_CHARACTER_RECOGNITION = new UnicodeBlock();
    public static final Character.UnicodeBlock ENCLOSED_ALPHANUMERICS = new UnicodeBlock();
    public static final Character.UnicodeBlock BOX_DRAWING = new UnicodeBlock();
    public static final Character.UnicodeBlock BLOCK_ELEMENTS = new UnicodeBlock();
    public static final Character.UnicodeBlock GEOMETRIC_SHAPES = new UnicodeBlock();
    public static final Character.UnicodeBlock MISCELLANEOUS_SYMBOLS = new UnicodeBlock();
    public static final Character.UnicodeBlock DINGBATS = new UnicodeBlock();
    public static final Character.UnicodeBlock CJK_SYMBOLS_AND_PUNCTUATION = new UnicodeBlock();
    public static final Character.UnicodeBlock HIRAGANA = new UnicodeBlock();
    public static final Character.UnicodeBlock KATAKANA = new UnicodeBlock();
    public static final Character.UnicodeBlock BOPOMOFO = new UnicodeBlock();
    public static final Character.UnicodeBlock HANGUL_COMPATIBILITY_JAMO = new UnicodeBlock();
    public static final Character.UnicodeBlock KANBUN = new UnicodeBlock();
    public static final Character.UnicodeBlock ENCLOSED_CJK_LETTERS_AND_MONTHS = new UnicodeBlock();
    public static final Character.UnicodeBlock CJK_COMPATIBILITY = new UnicodeBlock();
    public static final Character.UnicodeBlock CJK_UNIFIED_IDEOGRAPHS = new UnicodeBlock();
    public static final Character.UnicodeBlock HANGUL_SYLLABLES = new UnicodeBlock();
    public static final Character.UnicodeBlock PRIVATE_USE_AREA = new UnicodeBlock();
    public static final Character.UnicodeBlock CJK_COMPATIBILITY_IDEOGRAPHS = new UnicodeBlock();
    public static final Character.UnicodeBlock ALPHABETIC_PRESENTATION_FORMS = new UnicodeBlock();
    public static final Character.UnicodeBlock ARABIC_PRESENTATION_FORMS_A = new UnicodeBlock();
    public static final Character.UnicodeBlock COMBINING_HALF_MARKS = new UnicodeBlock();
    public static final Character.UnicodeBlock CJK_COMPATIBILITY_FORMS = new UnicodeBlock();
    public static final Character.UnicodeBlock SMALL_FORM_VARIANTS = new UnicodeBlock();
    public static final Character.UnicodeBlock ARABIC_PRESENTATION_FORMS_B = new UnicodeBlock();
    public static final Character.UnicodeBlock HALFWIDTH_AND_FULLWIDTH_FORMS = new UnicodeBlock();
    public static final Character.UnicodeBlock SPECIALS = new UnicodeBlock();
    public static final Character.UnicodeBlock SURROGATES_AREA = new UnicodeBlock();
    public static final Character.UnicodeBlock SYRIAC = new UnicodeBlock();
    public static final Character.UnicodeBlock THAANA = new UnicodeBlock();
    public static final Character.UnicodeBlock SINHALA = new UnicodeBlock();
    public static final Character.UnicodeBlock MYANMAR = new UnicodeBlock();
    public static final Character.UnicodeBlock ETHIOPIC = new UnicodeBlock();
    public static final Character.UnicodeBlock CHEROKEE = new UnicodeBlock();
    public static final Character.UnicodeBlock UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS = new UnicodeBlock();
    public static final Character.UnicodeBlock OGHAM = new UnicodeBlock();
    public static final Character.UnicodeBlock RUNIC = new UnicodeBlock();
    public static final Character.UnicodeBlock KHMER = new UnicodeBlock();
    public static final Character.UnicodeBlock MONGOLIAN = new UnicodeBlock();
    public static final Character.UnicodeBlock BRAILLE_PATTERNS = new UnicodeBlock();
    public static final Character.UnicodeBlock CJK_RADICALS_SUPPLEMENT = new UnicodeBlock();
    public static final Character.UnicodeBlock KANGXI_RADICALS = new UnicodeBlock();
    public static final Character.UnicodeBlock IDEOGRAPHIC_DESCRIPTION_CHARACTERS = new UnicodeBlock();
    public static final Character.UnicodeBlock BOPOMOFO_EXTENDED = new UnicodeBlock();
    public static final Character.UnicodeBlock CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A = new UnicodeBlock();
    public static final Character.UnicodeBlock YI_SYLLABLES = new UnicodeBlock();
    public static final Character.UnicodeBlock YI_RADICALS = new UnicodeBlock();
    public static final Character.UnicodeBlock CYRILLIC_SUPPLEMENTARY = new UnicodeBlock();
    public static final Character.UnicodeBlock TAGALOG = new UnicodeBlock();
    public static final Character.UnicodeBlock HANUNOO = new UnicodeBlock();
    public static final Character.UnicodeBlock BUHID = new UnicodeBlock();
    public static final Character.UnicodeBlock TAGBANWA = new UnicodeBlock();
    public static final Character.UnicodeBlock LIMBU = new UnicodeBlock();
    public static final Character.UnicodeBlock TAI_LE = new UnicodeBlock();
    public static final Character.UnicodeBlock KHMER_SYMBOLS = new UnicodeBlock();
    public static final Character.UnicodeBlock PHONETIC_EXTENSIONS = new UnicodeBlock();
    public static final Character.UnicodeBlock MISCELLANEOUS_MATHEMATICAL_SYMBOLS_A = new UnicodeBlock();
    public static final Character.UnicodeBlock SUPPLEMENTAL_ARROWS_A = new UnicodeBlock();
    public static final Character.UnicodeBlock SUPPLEMENTAL_ARROWS_B = new UnicodeBlock();
    public static final Character.UnicodeBlock MISCELLANEOUS_MATHEMATICAL_SYMBOLS_B = new UnicodeBlock();
    public static final Character.UnicodeBlock SUPPLEMENTAL_MATHEMATICAL_OPERATORS = new UnicodeBlock();
    public static final Character.UnicodeBlock MISCELLANEOUS_SYMBOLS_AND_ARROWS = new UnicodeBlock();
    public static final Character.UnicodeBlock KATAKANA_PHONETIC_EXTENSIONS = new UnicodeBlock();
    public static final Character.UnicodeBlock YIJING_HEXAGRAM_SYMBOLS = new UnicodeBlock();
    public static final Character.UnicodeBlock VARIATION_SELECTORS = new UnicodeBlock();
    public static final Character.UnicodeBlock LINEAR_B_SYLLABARY = new UnicodeBlock();
    public static final Character.UnicodeBlock LINEAR_B_IDEOGRAMS = new UnicodeBlock();
    public static final Character.UnicodeBlock AEGEAN_NUMBERS = new UnicodeBlock();
    public static final Character.UnicodeBlock OLD_ITALIC = new UnicodeBlock();
    public static final Character.UnicodeBlock GOTHIC = new UnicodeBlock();
    public static final Character.UnicodeBlock UGARITIC = new UnicodeBlock();
    public static final Character.UnicodeBlock DESERET = new UnicodeBlock();
    public static final Character.UnicodeBlock SHAVIAN = new UnicodeBlock();
    public static final Character.UnicodeBlock OSMANYA = new UnicodeBlock();
    public static final Character.UnicodeBlock CYPRIOT_SYLLABARY = new UnicodeBlock();
    public static final Character.UnicodeBlock BYZANTINE_MUSICAL_SYMBOLS = new UnicodeBlock();
    public static final Character.UnicodeBlock MUSICAL_SYMBOLS = new UnicodeBlock();
    public static final Character.UnicodeBlock TAI_XUAN_JING_SYMBOLS = new UnicodeBlock();
    public static final Character.UnicodeBlock MATHEMATICAL_ALPHANUMERIC_SYMBOLS = new UnicodeBlock();
    public static final Character.UnicodeBlock CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B = new UnicodeBlock();
    public static final Character.UnicodeBlock CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT = new UnicodeBlock();
    public static final Character.UnicodeBlock TAGS = new UnicodeBlock();
    public static final Character.UnicodeBlock VARIATION_SELECTORS_SUPPLEMENT = new UnicodeBlock();
    public static final Character.UnicodeBlock SUPPLEMENTARY_PRIVATE_USE_AREA_A = new UnicodeBlock();
    public static final Character.UnicodeBlock SUPPLEMENTARY_PRIVATE_USE_AREA_B = new UnicodeBlock();
    public static final Character.UnicodeBlock HIGH_SURROGATES = new UnicodeBlock();
    public static final Character.UnicodeBlock HIGH_PRIVATE_USE_SURROGATES = new UnicodeBlock();
    public static final Character.UnicodeBlock LOW_SURROGATES = new UnicodeBlock();
    @Pure public static @Nullable UnicodeBlock of(char c) { throw new RuntimeException("skeleton method"); }
    @Pure public static @Nullable UnicodeBlock of(int codePoint) { throw new RuntimeException("skeleton method"); }
    @Pure public static final UnicodeBlock forName(String blockName) { throw new RuntimeException("skeleton method"); }

    protected UnicodeBlock() {}
  }
  public static final int MIN_RADIX = 2;
  public static final int MAX_RADIX = 36;
  public static final char MIN_VALUE = 0;
  public static final char MAX_VALUE = 65535;
  public static final Class<Character> TYPE = null;
  public static final byte UNASSIGNED = 0;
  public static final byte UPPERCASE_LETTER = 1;
  public static final byte LOWERCASE_LETTER = 2;
  public static final byte TITLECASE_LETTER = 3;
  public static final byte MODIFIER_LETTER = 4;
  public static final byte OTHER_LETTER = 5;
  public static final byte NON_SPACING_MARK = 6;
  public static final byte ENCLOSING_MARK = 7;
  public static final byte COMBINING_SPACING_MARK = 8;
  public static final byte DECIMAL_DIGIT_NUMBER = 9;
  public static final byte LETTER_NUMBER = 10;
  public static final byte OTHER_NUMBER = 11;
  public static final byte SPACE_SEPARATOR = 12;
  public static final byte LINE_SEPARATOR = 13;
  public static final byte PARAGRAPH_SEPARATOR = 14;
  public static final byte CONTROL = 15;
  public static final byte FORMAT = 16;
  public static final byte PRIVATE_USE = 18;
  public static final byte SURROGATE = 19;
  public static final byte DASH_PUNCTUATION = 20;
  public static final byte START_PUNCTUATION = 21;
  public static final byte END_PUNCTUATION = 22;
  public static final byte CONNECTOR_PUNCTUATION = 23;
  public static final byte OTHER_PUNCTUATION = 24;
  public static final byte MATH_SYMBOL = 25;
  public static final byte CURRENCY_SYMBOL = 26;
  public static final byte MODIFIER_SYMBOL = 27;
  public static final byte OTHER_SYMBOL = 28;
  public static final byte INITIAL_QUOTE_PUNCTUATION = 29;
  public static final byte FINAL_QUOTE_PUNCTUATION = 30;
  public static final byte DIRECTIONALITY_UNDEFINED = -1;
  public static final byte DIRECTIONALITY_LEFT_TO_RIGHT = 0;
  public static final byte DIRECTIONALITY_RIGHT_TO_LEFT = 1;
  public static final byte DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC = 2;
  public static final byte DIRECTIONALITY_EUROPEAN_NUMBER = 3;
  public static final byte DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR = 4;
  public static final byte DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR = 5;
  public static final byte DIRECTIONALITY_ARABIC_NUMBER = 6;
  public static final byte DIRECTIONALITY_COMMON_NUMBER_SEPARATOR = 7;
  public static final byte DIRECTIONALITY_NONSPACING_MARK = 8;
  public static final byte DIRECTIONALITY_BOUNDARY_NEUTRAL = 9;
  public static final byte DIRECTIONALITY_PARAGRAPH_SEPARATOR = 10;
  public static final byte DIRECTIONALITY_SEGMENT_SEPARATOR = 11;
  public static final byte DIRECTIONALITY_WHITESPACE = 12;
  public static final byte DIRECTIONALITY_OTHER_NEUTRALS = 13;
  public static final byte DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING = 14;
  public static final byte DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE = 15;
  public static final byte DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING = 16;
  public static final byte DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE = 17;
  public static final byte DIRECTIONALITY_POP_DIRECTIONAL_FORMAT = 18;
  public static final char MIN_HIGH_SURROGATE = 55296;
  public static final char MAX_HIGH_SURROGATE = 56319;
  public static final char MIN_LOW_SURROGATE = 56320;
  public static final char MAX_LOW_SURROGATE = 57343;
  public static final char MIN_SURROGATE = 55296;
  public static final char MAX_SURROGATE = 57343;
  public static final int MIN_SUPPLEMENTARY_CODE_POINT = 65536;
  public static final int MIN_CODE_POINT = 0;
  public static final int MAX_CODE_POINT = 1114111;
  public static final int SIZE = 16;
  @Pure public Character(char ch) { throw new RuntimeException("skeleton method"); }
  // Not @Pure: might return new value that is not == (but is equals() to previous results.
  public static Character valueOf(char ch) { throw new RuntimeException("skeleton method"); }
  @Pure public char charValue() { throw new RuntimeException("skeleton method"); }
  @Pure public int hashCode() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean equals(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String toString() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static String toString(char ch) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isValidCodePoint(int codePoint) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isBmpCodePoint(int codePoint) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isSupplementaryCodePoint(int codePoint) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isHighSurrogate(char ch) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isLowSurrogate(char ch) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isSurrogate(char ch) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isSurrogatePair(char high, char low) { throw new RuntimeException("skeleton method"); }
  @Pure public static int charCount(int codePoint) { throw new RuntimeException("skeleton method"); }
  @Pure public static int toCodePoint(char high, char low) { throw new RuntimeException("skeleton method"); }
  @Pure public static int codePointAt(CharSequence seq, int index) { throw new RuntimeException("skeleton method"); }
  @Pure public static int codePointAt(char[] a, int index) { throw new RuntimeException("skeleton method"); }
  @Pure public static int codePointAt(char[] a, int index, int limit) { throw new RuntimeException("skeleton method"); }
  @Pure public static int codePointBefore(CharSequence seq, int index) { throw new RuntimeException("skeleton method"); }
  @Pure public static int codePointBefore(char[] a, int index) { throw new RuntimeException("skeleton method"); }
  @Pure public static int codePointBefore(char[] a, int index, int start) { throw new RuntimeException("skeleton method"); }
  @Pure public static char highSurrogate(int codePoint) { throw new RuntimeException("skeleton method"); }
  @Pure public static char lowSurrogate(int codePoint) { throw new RuntimeException("skeleton method"); }
  @Pure public static int toChars(int codePoint, char[] dst, int dstIndex) { throw new RuntimeException("skeleton method"); }
  @Pure public static char[] toChars(int codePoint) { throw new RuntimeException("skeleton method"); }
  @Pure public static int codePointCount(CharSequence seq, int beginIndex, int endIndex) { throw new RuntimeException("skeleton method"); }
  @Pure public static int codePointCount(char[] a, int offset, int count) { throw new RuntimeException("skeleton method"); }
  @Pure public static int offsetByCodePoints(CharSequence seq, int index, int codePointOffset) { throw new RuntimeException("skeleton method"); }
  @Pure public static int offsetByCodePoints(char[] a, int start, int count, int index, int codePointOffset) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isLowerCase(char ch) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isLowerCase(int codePoint) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isUpperCase(char ch) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isUpperCase(int codePoint) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isTitleCase(char ch) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isTitleCase(int codePoint) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isDigit(char ch) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isDigit(int codePoint) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isDefined(char ch) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isDefined(int codePoint) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isLetter(char ch) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isLetter(int codePoint) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isLetterOrDigit(char ch) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isLetterOrDigit(int codePoint) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isJavaLetter(char ch) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isJavaLetterOrDigit(char ch) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isAlphabetic(int codePoint) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isIdeographic(int codePoint) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isJavaIdentifierStart(char ch) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isJavaIdentifierStart(int codePoint) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isJavaIdentifierPart(char ch) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isJavaIdentifierPart(int codePoint) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isUnicodeIdentifierStart(char ch) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isUnicodeIdentifierStart(int codePoint) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isUnicodeIdentifierPart(char ch) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isUnicodeIdentifierPart(int codePoint) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isIdentifierIgnorable(char ch) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isIdentifierIgnorable(int codePoint) { throw new RuntimeException("skeleton method"); }
  @Pure public static char toLowerCase(char ch) { throw new RuntimeException("skeleton method"); }
  @Pure public static int toLowerCase(int codePoint) { throw new RuntimeException("skeleton method"); }
  @Pure public static char toUpperCase(char ch) { throw new RuntimeException("skeleton method"); }
  @Pure public static int toUpperCase(int codePoint) { throw new RuntimeException("skeleton method"); }
  @Pure public static char toTitleCase(char ch) { throw new RuntimeException("skeleton method"); }
  @Pure public static int toTitleCase(int codePoint) { throw new RuntimeException("skeleton method"); }
  @Pure public static int digit(char ch, int radix) { throw new RuntimeException("skeleton method"); }
  @Pure public static int digit(int codePoint, int radix) { throw new RuntimeException("skeleton method"); }
  @Pure public static int getNumericValue(char ch) { throw new RuntimeException("skeleton method"); }
  @Pure public static int getNumericValue(int codePoint) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isSpace(char ch) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isSpaceChar(char ch) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isSpaceChar(int codePoint) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isWhitespace(char ch) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isWhitespace(int codePoint) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isISOControl(char ch) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isISOControl(int codePoint) { throw new RuntimeException("skeleton method"); }
  @Pure public static int getType(char ch) { throw new RuntimeException("skeleton method"); }
  @Pure public static int getType(int codePoint) { throw new RuntimeException("skeleton method"); }
  @Pure public static char forDigit(int digit, int radix) { throw new RuntimeException("skeleton method"); }
  @Pure public static byte getDirectionality(char ch) { throw new RuntimeException("skeleton method"); }
  @Pure public static byte getDirectionality(int codePoint) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isMirrored(char ch) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isMirrored(int codePoint) { throw new RuntimeException("skeleton method"); }
  @Pure public int compareTo(Character anotherCharacter) { throw new RuntimeException("skeleton method"); }
  @Pure public static int compare(char x, char y) { throw new RuntimeException("skeleton method"); }
  @Pure public static char reverseBytes(char ch) { throw new RuntimeException("skeleton method"); }
  @Pure public static String getName(int codePoint) { throw new RuntimeException("skeleton method"); }

  public static enum UnicodeScript {
    // At least one enum constant should be declared to avoid a syntax error.
    COMMON;
    @Pure public static UnicodeScript of(int codePoint) { throw new RuntimeException("skeleton method"); }
    @Pure public static final UnicodeScript forName(String scriptName) { throw new RuntimeException("skeleton method"); }
  }
}
