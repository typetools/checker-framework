package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public final class Character implements java.io.Serializable, Comparable<Character> {
  private static final long serialVersionUID = 0;
  public static class Subset{
    protected Subset() {}
    public final boolean equals(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
    public final int hashCode() { throw new RuntimeException("skeleton method"); }
    public final String toString() { throw new RuntimeException("skeleton method"); }
  }
  public final static class UnicodeBlock extends Subset{
    public final static Character.UnicodeBlock BASIC_LATIN = new UnicodeBlock();
    public final static Character.UnicodeBlock LATIN_1_SUPPLEMENT = new UnicodeBlock();
    public final static Character.UnicodeBlock LATIN_EXTENDED_A = new UnicodeBlock();
    public final static Character.UnicodeBlock LATIN_EXTENDED_B = new UnicodeBlock();
    public final static Character.UnicodeBlock IPA_EXTENSIONS = new UnicodeBlock();
    public final static Character.UnicodeBlock SPACING_MODIFIER_LETTERS = new UnicodeBlock();
    public final static Character.UnicodeBlock COMBINING_DIACRITICAL_MARKS = new UnicodeBlock();
    public final static Character.UnicodeBlock GREEK = new UnicodeBlock();
    public final static Character.UnicodeBlock CYRILLIC = new UnicodeBlock();
    public final static Character.UnicodeBlock ARMENIAN = new UnicodeBlock();
    public final static Character.UnicodeBlock HEBREW = new UnicodeBlock();
    public final static Character.UnicodeBlock ARABIC = new UnicodeBlock();
    public final static Character.UnicodeBlock DEVANAGARI = new UnicodeBlock();
    public final static Character.UnicodeBlock BENGALI = new UnicodeBlock();
    public final static Character.UnicodeBlock GURMUKHI = new UnicodeBlock();
    public final static Character.UnicodeBlock GUJARATI = new UnicodeBlock();
    public final static Character.UnicodeBlock ORIYA = new UnicodeBlock();
    public final static Character.UnicodeBlock TAMIL = new UnicodeBlock();
    public final static Character.UnicodeBlock TELUGU = new UnicodeBlock();
    public final static Character.UnicodeBlock KANNADA = new UnicodeBlock();
    public final static Character.UnicodeBlock MALAYALAM = new UnicodeBlock();
    public final static Character.UnicodeBlock THAI = new UnicodeBlock();
    public final static Character.UnicodeBlock LAO = new UnicodeBlock();
    public final static Character.UnicodeBlock TIBETAN = new UnicodeBlock();
    public final static Character.UnicodeBlock GEORGIAN = new UnicodeBlock();
    public final static Character.UnicodeBlock HANGUL_JAMO = new UnicodeBlock();
    public final static Character.UnicodeBlock LATIN_EXTENDED_ADDITIONAL = new UnicodeBlock();
    public final static Character.UnicodeBlock GREEK_EXTENDED = new UnicodeBlock();
    public final static Character.UnicodeBlock GENERAL_PUNCTUATION = new UnicodeBlock();
    public final static Character.UnicodeBlock SUPERSCRIPTS_AND_SUBSCRIPTS = new UnicodeBlock();
    public final static Character.UnicodeBlock CURRENCY_SYMBOLS = new UnicodeBlock();
    public final static Character.UnicodeBlock COMBINING_MARKS_FOR_SYMBOLS = new UnicodeBlock();
    public final static Character.UnicodeBlock LETTERLIKE_SYMBOLS = new UnicodeBlock();
    public final static Character.UnicodeBlock NUMBER_FORMS = new UnicodeBlock();
    public final static Character.UnicodeBlock ARROWS = new UnicodeBlock();
    public final static Character.UnicodeBlock MATHEMATICAL_OPERATORS = new UnicodeBlock();
    public final static Character.UnicodeBlock MISCELLANEOUS_TECHNICAL = new UnicodeBlock();
    public final static Character.UnicodeBlock CONTROL_PICTURES = new UnicodeBlock();
    public final static Character.UnicodeBlock OPTICAL_CHARACTER_RECOGNITION = new UnicodeBlock();
    public final static Character.UnicodeBlock ENCLOSED_ALPHANUMERICS = new UnicodeBlock();
    public final static Character.UnicodeBlock BOX_DRAWING = new UnicodeBlock();
    public final static Character.UnicodeBlock BLOCK_ELEMENTS = new UnicodeBlock();
    public final static Character.UnicodeBlock GEOMETRIC_SHAPES = new UnicodeBlock();
    public final static Character.UnicodeBlock MISCELLANEOUS_SYMBOLS = new UnicodeBlock();
    public final static Character.UnicodeBlock DINGBATS = new UnicodeBlock();
    public final static Character.UnicodeBlock CJK_SYMBOLS_AND_PUNCTUATION = new UnicodeBlock();
    public final static Character.UnicodeBlock HIRAGANA = new UnicodeBlock();
    public final static Character.UnicodeBlock KATAKANA = new UnicodeBlock();
    public final static Character.UnicodeBlock BOPOMOFO = new UnicodeBlock();
    public final static Character.UnicodeBlock HANGUL_COMPATIBILITY_JAMO = new UnicodeBlock();
    public final static Character.UnicodeBlock KANBUN = new UnicodeBlock();
    public final static Character.UnicodeBlock ENCLOSED_CJK_LETTERS_AND_MONTHS = new UnicodeBlock();
    public final static Character.UnicodeBlock CJK_COMPATIBILITY = new UnicodeBlock();
    public final static Character.UnicodeBlock CJK_UNIFIED_IDEOGRAPHS = new UnicodeBlock();
    public final static Character.UnicodeBlock HANGUL_SYLLABLES = new UnicodeBlock();
    public final static Character.UnicodeBlock PRIVATE_USE_AREA = new UnicodeBlock();
    public final static Character.UnicodeBlock CJK_COMPATIBILITY_IDEOGRAPHS = new UnicodeBlock();
    public final static Character.UnicodeBlock ALPHABETIC_PRESENTATION_FORMS = new UnicodeBlock();
    public final static Character.UnicodeBlock ARABIC_PRESENTATION_FORMS_A = new UnicodeBlock();
    public final static Character.UnicodeBlock COMBINING_HALF_MARKS = new UnicodeBlock();
    public final static Character.UnicodeBlock CJK_COMPATIBILITY_FORMS = new UnicodeBlock();
    public final static Character.UnicodeBlock SMALL_FORM_VARIANTS = new UnicodeBlock();
    public final static Character.UnicodeBlock ARABIC_PRESENTATION_FORMS_B = new UnicodeBlock();
    public final static Character.UnicodeBlock HALFWIDTH_AND_FULLWIDTH_FORMS = new UnicodeBlock();
    public final static Character.UnicodeBlock SPECIALS = new UnicodeBlock();
    public final static Character.UnicodeBlock SURROGATES_AREA = new UnicodeBlock();
    public final static Character.UnicodeBlock SYRIAC = new UnicodeBlock();
    public final static Character.UnicodeBlock THAANA = new UnicodeBlock();
    public final static Character.UnicodeBlock SINHALA = new UnicodeBlock();
    public final static Character.UnicodeBlock MYANMAR = new UnicodeBlock();
    public final static Character.UnicodeBlock ETHIOPIC = new UnicodeBlock();
    public final static Character.UnicodeBlock CHEROKEE = new UnicodeBlock();
    public final static Character.UnicodeBlock UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS = new UnicodeBlock();
    public final static Character.UnicodeBlock OGHAM = new UnicodeBlock();
    public final static Character.UnicodeBlock RUNIC = new UnicodeBlock();
    public final static Character.UnicodeBlock KHMER = new UnicodeBlock();
    public final static Character.UnicodeBlock MONGOLIAN = new UnicodeBlock();
    public final static Character.UnicodeBlock BRAILLE_PATTERNS = new UnicodeBlock();
    public final static Character.UnicodeBlock CJK_RADICALS_SUPPLEMENT = new UnicodeBlock();
    public final static Character.UnicodeBlock KANGXI_RADICALS = new UnicodeBlock();
    public final static Character.UnicodeBlock IDEOGRAPHIC_DESCRIPTION_CHARACTERS = new UnicodeBlock();
    public final static Character.UnicodeBlock BOPOMOFO_EXTENDED = new UnicodeBlock();
    public final static Character.UnicodeBlock CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A = new UnicodeBlock();
    public final static Character.UnicodeBlock YI_SYLLABLES = new UnicodeBlock();
    public final static Character.UnicodeBlock YI_RADICALS = new UnicodeBlock();
    public final static Character.UnicodeBlock CYRILLIC_SUPPLEMENTARY = new UnicodeBlock();
    public final static Character.UnicodeBlock TAGALOG = new UnicodeBlock();
    public final static Character.UnicodeBlock HANUNOO = new UnicodeBlock();
    public final static Character.UnicodeBlock BUHID = new UnicodeBlock();
    public final static Character.UnicodeBlock TAGBANWA = new UnicodeBlock();
    public final static Character.UnicodeBlock LIMBU = new UnicodeBlock();
    public final static Character.UnicodeBlock TAI_LE = new UnicodeBlock();
    public final static Character.UnicodeBlock KHMER_SYMBOLS = new UnicodeBlock();
    public final static Character.UnicodeBlock PHONETIC_EXTENSIONS = new UnicodeBlock();
    public final static Character.UnicodeBlock MISCELLANEOUS_MATHEMATICAL_SYMBOLS_A = new UnicodeBlock();
    public final static Character.UnicodeBlock SUPPLEMENTAL_ARROWS_A = new UnicodeBlock();
    public final static Character.UnicodeBlock SUPPLEMENTAL_ARROWS_B = new UnicodeBlock();
    public final static Character.UnicodeBlock MISCELLANEOUS_MATHEMATICAL_SYMBOLS_B = new UnicodeBlock();
    public final static Character.UnicodeBlock SUPPLEMENTAL_MATHEMATICAL_OPERATORS = new UnicodeBlock();
    public final static Character.UnicodeBlock MISCELLANEOUS_SYMBOLS_AND_ARROWS = new UnicodeBlock();
    public final static Character.UnicodeBlock KATAKANA_PHONETIC_EXTENSIONS = new UnicodeBlock();
    public final static Character.UnicodeBlock YIJING_HEXAGRAM_SYMBOLS = new UnicodeBlock();
    public final static Character.UnicodeBlock VARIATION_SELECTORS = new UnicodeBlock();
    public final static Character.UnicodeBlock LINEAR_B_SYLLABARY = new UnicodeBlock();
    public final static Character.UnicodeBlock LINEAR_B_IDEOGRAMS = new UnicodeBlock();
    public final static Character.UnicodeBlock AEGEAN_NUMBERS = new UnicodeBlock();
    public final static Character.UnicodeBlock OLD_ITALIC = new UnicodeBlock();
    public final static Character.UnicodeBlock GOTHIC = new UnicodeBlock();
    public final static Character.UnicodeBlock UGARITIC = new UnicodeBlock();
    public final static Character.UnicodeBlock DESERET = new UnicodeBlock();
    public final static Character.UnicodeBlock SHAVIAN = new UnicodeBlock();
    public final static Character.UnicodeBlock OSMANYA = new UnicodeBlock();
    public final static Character.UnicodeBlock CYPRIOT_SYLLABARY = new UnicodeBlock();
    public final static Character.UnicodeBlock BYZANTINE_MUSICAL_SYMBOLS = new UnicodeBlock();
    public final static Character.UnicodeBlock MUSICAL_SYMBOLS = new UnicodeBlock();
    public final static Character.UnicodeBlock TAI_XUAN_JING_SYMBOLS = new UnicodeBlock();
    public final static Character.UnicodeBlock MATHEMATICAL_ALPHANUMERIC_SYMBOLS = new UnicodeBlock();
    public final static Character.UnicodeBlock CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B = new UnicodeBlock();
    public final static Character.UnicodeBlock CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT = new UnicodeBlock();
    public final static Character.UnicodeBlock TAGS = new UnicodeBlock();
    public final static Character.UnicodeBlock VARIATION_SELECTORS_SUPPLEMENT = new UnicodeBlock();
    public final static Character.UnicodeBlock SUPPLEMENTARY_PRIVATE_USE_AREA_A = new UnicodeBlock();
    public final static Character.UnicodeBlock SUPPLEMENTARY_PRIVATE_USE_AREA_B = new UnicodeBlock();
    public final static Character.UnicodeBlock HIGH_SURROGATES = new UnicodeBlock();
    public final static Character.UnicodeBlock HIGH_PRIVATE_USE_SURROGATES = new UnicodeBlock();
    public final static Character.UnicodeBlock LOW_SURROGATES = new UnicodeBlock();
    public static @Nullable Character.UnicodeBlock of(char a1) { throw new RuntimeException("skeleton method"); }
    public static @Nullable Character.UnicodeBlock of(int a1) { throw new RuntimeException("skeleton method"); }
    public final static Character.UnicodeBlock forName(String a1) { throw new RuntimeException("skeleton method"); }
    
    protected UnicodeBlock() {}
  }
  public final static int MIN_RADIX = 2;
  public final static int MAX_RADIX = 36;
  public final static char MIN_VALUE = 0;
  public final static char MAX_VALUE = 65535;
  public final static Class<Character> TYPE;
  public final static byte UNASSIGNED = 0;
  public final static byte UPPERCASE_LETTER = 1;
  public final static byte LOWERCASE_LETTER = 2;
  public final static byte TITLECASE_LETTER = 3;
  public final static byte MODIFIER_LETTER = 4;
  public final static byte OTHER_LETTER = 5;
  public final static byte NON_SPACING_MARK = 6;
  public final static byte ENCLOSING_MARK = 7;
  public final static byte COMBINING_SPACING_MARK = 8;
  public final static byte DECIMAL_DIGIT_NUMBER = 9;
  public final static byte LETTER_NUMBER = 10;
  public final static byte OTHER_NUMBER = 11;
  public final static byte SPACE_SEPARATOR = 12;
  public final static byte LINE_SEPARATOR = 13;
  public final static byte PARAGRAPH_SEPARATOR = 14;
  public final static byte CONTROL = 15;
  public final static byte FORMAT = 16;
  public final static byte PRIVATE_USE = 18;
  public final static byte SURROGATE = 19;
  public final static byte DASH_PUNCTUATION = 20;
  public final static byte START_PUNCTUATION = 21;
  public final static byte END_PUNCTUATION = 22;
  public final static byte CONNECTOR_PUNCTUATION = 23;
  public final static byte OTHER_PUNCTUATION = 24;
  public final static byte MATH_SYMBOL = 25;
  public final static byte CURRENCY_SYMBOL = 26;
  public final static byte MODIFIER_SYMBOL = 27;
  public final static byte OTHER_SYMBOL = 28;
  public final static byte INITIAL_QUOTE_PUNCTUATION = 29;
  public final static byte FINAL_QUOTE_PUNCTUATION = 30;
  public final static byte DIRECTIONALITY_UNDEFINED = -1;
  public final static byte DIRECTIONALITY_LEFT_TO_RIGHT = 0;
  public final static byte DIRECTIONALITY_RIGHT_TO_LEFT = 1;
  public final static byte DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC = 2;
  public final static byte DIRECTIONALITY_EUROPEAN_NUMBER = 3;
  public final static byte DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR = 4;
  public final static byte DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR = 5;
  public final static byte DIRECTIONALITY_ARABIC_NUMBER = 6;
  public final static byte DIRECTIONALITY_COMMON_NUMBER_SEPARATOR = 7;
  public final static byte DIRECTIONALITY_NONSPACING_MARK = 8;
  public final static byte DIRECTIONALITY_BOUNDARY_NEUTRAL = 9;
  public final static byte DIRECTIONALITY_PARAGRAPH_SEPARATOR = 10;
  public final static byte DIRECTIONALITY_SEGMENT_SEPARATOR = 11;
  public final static byte DIRECTIONALITY_WHITESPACE = 12;
  public final static byte DIRECTIONALITY_OTHER_NEUTRALS = 13;
  public final static byte DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING = 14;
  public final static byte DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE = 15;
  public final static byte DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING = 16;
  public final static byte DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE = 17;
  public final static byte DIRECTIONALITY_POP_DIRECTIONAL_FORMAT = 18;
  public final static char MIN_HIGH_SURROGATE = 55296;
  public final static char MAX_HIGH_SURROGATE = 56319;
  public final static char MIN_LOW_SURROGATE = 56320;
  public final static char MAX_LOW_SURROGATE = 57343;
  public final static char MIN_SURROGATE = 55296;
  public final static char MAX_SURROGATE = 57343;
  public final static int MIN_SUPPLEMENTARY_CODE_POINT = 65536;
  public final static int MIN_CODE_POINT = 0;
  public final static int MAX_CODE_POINT = 1114111;
  public final static int SIZE = 16;
  public Character(char a1) { throw new RuntimeException("skeleton method"); }
  public static Character valueOf(char a1) { throw new RuntimeException("skeleton method"); }
  public char charValue() { throw new RuntimeException("skeleton method"); }
  public int hashCode() { throw new RuntimeException("skeleton method"); }
  public boolean equals(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public String toString() { throw new RuntimeException("skeleton method"); }
  public static String toString(char a1) { throw new RuntimeException("skeleton method"); }
  public static boolean isValidCodePoint(int a1) { throw new RuntimeException("skeleton method"); }
  public static boolean isSupplementaryCodePoint(int a1) { throw new RuntimeException("skeleton method"); }
  public static boolean isHighSurrogate(char a1) { throw new RuntimeException("skeleton method"); }
  public static boolean isLowSurrogate(char a1) { throw new RuntimeException("skeleton method"); }
  public static boolean isSurrogatePair(char a1, char a2) { throw new RuntimeException("skeleton method"); }
  public static int charCount(int a1) { throw new RuntimeException("skeleton method"); }
  public static int toCodePoint(char a1, char a2) { throw new RuntimeException("skeleton method"); }
  public static int codePointAt(CharSequence a1, int a2) { throw new RuntimeException("skeleton method"); }
  public static int codePointAt(char[] a1, int a2) { throw new RuntimeException("skeleton method"); }
  public static int codePointAt(char[] a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public static int codePointBefore(CharSequence a1, int a2) { throw new RuntimeException("skeleton method"); }
  public static int codePointBefore(char[] a1, int a2) { throw new RuntimeException("skeleton method"); }
  public static int codePointBefore(char[] a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public static int toChars(int a1, char[] a2, int a3) { throw new RuntimeException("skeleton method"); }
  public static char[] toChars(int a1) { throw new RuntimeException("skeleton method"); }
  public static int codePointCount(CharSequence a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public static int codePointCount(char[] a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public static int offsetByCodePoints(CharSequence a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public static int offsetByCodePoints(char[] a1, int a2, int a3, int a4, int a5) { throw new RuntimeException("skeleton method"); }
  public static boolean isLowerCase(char a1) { throw new RuntimeException("skeleton method"); }
  public static boolean isLowerCase(int a1) { throw new RuntimeException("skeleton method"); }
  public static boolean isUpperCase(char a1) { throw new RuntimeException("skeleton method"); }
  public static boolean isUpperCase(int a1) { throw new RuntimeException("skeleton method"); }
  public static boolean isTitleCase(char a1) { throw new RuntimeException("skeleton method"); }
  public static boolean isTitleCase(int a1) { throw new RuntimeException("skeleton method"); }
  public static boolean isDigit(char a1) { throw new RuntimeException("skeleton method"); }
  public static boolean isDigit(int a1) { throw new RuntimeException("skeleton method"); }
  public static boolean isDefined(char a1) { throw new RuntimeException("skeleton method"); }
  public static boolean isDefined(int a1) { throw new RuntimeException("skeleton method"); }
  public static boolean isLetter(char a1) { throw new RuntimeException("skeleton method"); }
  public static boolean isLetter(int a1) { throw new RuntimeException("skeleton method"); }
  public static boolean isLetterOrDigit(char a1) { throw new RuntimeException("skeleton method"); }
  public static boolean isLetterOrDigit(int a1) { throw new RuntimeException("skeleton method"); }
  public static boolean isJavaLetter(char a1) { throw new RuntimeException("skeleton method"); }
  public static boolean isJavaLetterOrDigit(char a1) { throw new RuntimeException("skeleton method"); }
  public static boolean isJavaIdentifierStart(char a1) { throw new RuntimeException("skeleton method"); }
  public static boolean isJavaIdentifierStart(int a1) { throw new RuntimeException("skeleton method"); }
  public static boolean isJavaIdentifierPart(char a1) { throw new RuntimeException("skeleton method"); }
  public static boolean isJavaIdentifierPart(int a1) { throw new RuntimeException("skeleton method"); }
  public static boolean isUnicodeIdentifierStart(char a1) { throw new RuntimeException("skeleton method"); }
  public static boolean isUnicodeIdentifierStart(int a1) { throw new RuntimeException("skeleton method"); }
  public static boolean isUnicodeIdentifierPart(char a1) { throw new RuntimeException("skeleton method"); }
  public static boolean isUnicodeIdentifierPart(int a1) { throw new RuntimeException("skeleton method"); }
  public static boolean isIdentifierIgnorable(char a1) { throw new RuntimeException("skeleton method"); }
  public static boolean isIdentifierIgnorable(int a1) { throw new RuntimeException("skeleton method"); }
  public static char toLowerCase(char a1) { throw new RuntimeException("skeleton method"); }
  public static int toLowerCase(int a1) { throw new RuntimeException("skeleton method"); }
  public static char toUpperCase(char a1) { throw new RuntimeException("skeleton method"); }
  public static int toUpperCase(int a1) { throw new RuntimeException("skeleton method"); }
  public static char toTitleCase(char a1) { throw new RuntimeException("skeleton method"); }
  public static int toTitleCase(int a1) { throw new RuntimeException("skeleton method"); }
  public static int digit(char a1, int a2) { throw new RuntimeException("skeleton method"); }
  public static int digit(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public static int getNumericValue(char a1) { throw new RuntimeException("skeleton method"); }
  public static int getNumericValue(int a1) { throw new RuntimeException("skeleton method"); }
  public static boolean isSpace(char a1) { throw new RuntimeException("skeleton method"); }
  public static boolean isSpaceChar(char a1) { throw new RuntimeException("skeleton method"); }
  public static boolean isSpaceChar(int a1) { throw new RuntimeException("skeleton method"); }
  public static boolean isWhitespace(char a1) { throw new RuntimeException("skeleton method"); }
  public static boolean isWhitespace(int a1) { throw new RuntimeException("skeleton method"); }
  public static boolean isISOControl(char a1) { throw new RuntimeException("skeleton method"); }
  public static boolean isISOControl(int a1) { throw new RuntimeException("skeleton method"); }
  public static int getType(char a1) { throw new RuntimeException("skeleton method"); }
  public static int getType(int a1) { throw new RuntimeException("skeleton method"); }
  public static char forDigit(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public static byte getDirectionality(char a1) { throw new RuntimeException("skeleton method"); }
  public static byte getDirectionality(int a1) { throw new RuntimeException("skeleton method"); }
  public static boolean isMirrored(char a1) { throw new RuntimeException("skeleton method"); }
  public static boolean isMirrored(int a1) { throw new RuntimeException("skeleton method"); }
  public int compareTo(Character a1) { throw new RuntimeException("skeleton method"); }
  public static char reverseBytes(char a1) { throw new RuntimeException("skeleton method"); }
}
