package java.lang;
import checkers.javari.quals.*;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;

public final @ReadOnly class Character extends Object implements java.io.Serializable, Comparable<Character> {
    private static final long serialVersionUID = 0L;
    public static final int MIN_RADIX = 2;
    public static final int MAX_RADIX = 36;
    public static final char   MIN_VALUE = '\u0000';
    public static final char   MAX_VALUE = '\uffff';
    public static final  Class<Character> TYPE = null;
    public static final byte
        UNASSIGNED                  = 0;
    public static final byte
        UPPERCASE_LETTER            = 1;
    public static final byte
        LOWERCASE_LETTER            = 2;
    public static final byte
        TITLECASE_LETTER            = 3;
    public static final byte
        MODIFIER_LETTER             = 4;
    public static final byte
        OTHER_LETTER                = 5;
    public static final byte
        NON_SPACING_MARK            = 6;
    public static final byte
        ENCLOSING_MARK              = 7;
    public static final byte
        COMBINING_SPACING_MARK      = 8;
    public static final byte
        DECIMAL_DIGIT_NUMBER        = 9;
    public static final byte
        LETTER_NUMBER               = 10;
    public static final byte
        OTHER_NUMBER                = 11;
    public static final byte
        SPACE_SEPARATOR             = 12;
    public static final byte
        LINE_SEPARATOR              = 13;
    public static final byte
        PARAGRAPH_SEPARATOR         = 14;
    public static final byte
        CONTROL                     = 15;
    public static final byte
        FORMAT                      = 16;
    public static final byte
        PRIVATE_USE                 = 18;
    public static final byte
        SURROGATE                   = 19;
    public static final byte
        DASH_PUNCTUATION            = 20;
    public static final byte
        START_PUNCTUATION           = 21;
    public static final byte
        END_PUNCTUATION             = 22;
    public static final byte
        CONNECTOR_PUNCTUATION       = 23;
    public static final byte
        OTHER_PUNCTUATION           = 24;
    public static final byte
        MATH_SYMBOL                 = 25;
    public static final byte
        CURRENCY_SYMBOL             = 26;
    public static final byte
        MODIFIER_SYMBOL             = 27;
    public static final byte
        OTHER_SYMBOL                = 28;
    public static final byte
        INITIAL_QUOTE_PUNCTUATION   = 29;
    public static final byte
        FINAL_QUOTE_PUNCTUATION     = 30;
    static final int ERROR = 0xFFFFFFFF;
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
    public static final char MIN_HIGH_SURROGATE = '\uD800';
    public static final char MAX_HIGH_SURROGATE = '\uDBFF';
    public static final char MIN_LOW_SURROGATE  = '\uDC00';
    public static final char MAX_LOW_SURROGATE  = '\uDFFF';
    public static final char MIN_SURROGATE = MIN_HIGH_SURROGATE;
    public static final char MAX_SURROGATE = MAX_LOW_SURROGATE;
    public static final int MIN_SUPPLEMENTARY_CODE_POINT = 0x010000;
    public static final int MIN_CODE_POINT = 0x000000;
    public static final int MAX_CODE_POINT = 0x10ffff;

    public static @ReadOnly class Subset  {
        protected Subset() {}
        protected Subset(String name) {}
        public final boolean equals(@ReadOnly Object obj) { throw new RuntimeException("skeleton method"); }
        public final int hashCode() { throw new RuntimeException("skeleton method"); }
        public final String toString() { throw new RuntimeException("skeleton method"); }
    }

    public static final class UnicodeBlock extends Subset {
        private UnicodeBlock() {}
        public static final UnicodeBlock BASIC_LATIN = null;
        public static final UnicodeBlock LATIN_1_SUPPLEMENT = null;
        public static final UnicodeBlock LATIN_EXTENDED_A = null;
        public static final UnicodeBlock LATIN_EXTENDED_B = null;
        public static final UnicodeBlock IPA_EXTENSIONS = null;
        public static final UnicodeBlock SPACING_MODIFIER_LETTERS = null;
        public static final UnicodeBlock COMBINING_DIACRITICAL_MARKS = null;
        public static final UnicodeBlock GREEK = null;
        public static final UnicodeBlock CYRILLIC = null;
        public static final UnicodeBlock ARMENIAN = null;
        public static final UnicodeBlock HEBREW = null;
        public static final UnicodeBlock ARABIC = null;
        public static final UnicodeBlock DEVANAGARI = null;
        public static final UnicodeBlock BENGALI = null;
        public static final UnicodeBlock GURMUKHI = null;
        public static final UnicodeBlock GUJARATI = null;
        public static final UnicodeBlock ORIYA = null;
        public static final UnicodeBlock TAMIL = null;
        public static final UnicodeBlock TELUGU = null;
        public static final UnicodeBlock KANNADA = null;
        public static final UnicodeBlock MALAYALAM = null;
        public static final UnicodeBlock THAI = null;
        public static final UnicodeBlock LAO = null;
        public static final UnicodeBlock TIBETAN = null;
        public static final UnicodeBlock GEORGIAN = null;
        public static final UnicodeBlock HANGUL_JAMO = null;
        public static final UnicodeBlock LATIN_EXTENDED_ADDITIONAL = null;
        public static final UnicodeBlock GREEK_EXTENDED = null;
        public static final UnicodeBlock GENERAL_PUNCTUATION = null;
        public static final UnicodeBlock SUPERSCRIPTS_AND_SUBSCRIPTS = null;
        public static final UnicodeBlock CURRENCY_SYMBOLS = null;
        public static final UnicodeBlock COMBINING_MARKS_FOR_SYMBOLS = null;
        public static final UnicodeBlock LETTERLIKE_SYMBOLS = null;
        public static final UnicodeBlock NUMBER_FORMS = null;
        public static final UnicodeBlock ARROWS = null;
        public static final UnicodeBlock MATHEMATICAL_OPERATORS = null;
        public static final UnicodeBlock MISCELLANEOUS_TECHNICAL = null;
        public static final UnicodeBlock CONTROL_PICTURES = null;
        public static final UnicodeBlock OPTICAL_CHARACTER_RECOGNITION = null;
        public static final UnicodeBlock ENCLOSED_ALPHANUMERICS = null;
        public static final UnicodeBlock BOX_DRAWING = null;
        public static final UnicodeBlock BLOCK_ELEMENTS = null;
        public static final UnicodeBlock GEOMETRIC_SHAPES = null;
        public static final UnicodeBlock MISCELLANEOUS_SYMBOLS = null;
        public static final UnicodeBlock DINGBATS = null;
        public static final UnicodeBlock CJK_SYMBOLS_AND_PUNCTUATION = null;
        public static final UnicodeBlock HIRAGANA = null;
        public static final UnicodeBlock KATAKANA = null;
        public static final UnicodeBlock BOPOMOFO = null;
        public static final UnicodeBlock HANGUL_COMPATIBILITY_JAMO = null;
        public static final UnicodeBlock KANBUN = null;
        public static final UnicodeBlock ENCLOSED_CJK_LETTERS_AND_MONTHS = null;
        public static final UnicodeBlock CJK_COMPATIBILITY = null;
        public static final UnicodeBlock CJK_UNIFIED_IDEOGRAPHS = null;
        public static final UnicodeBlock HANGUL_SYLLABLES = null;
        public static final UnicodeBlock PRIVATE_USE_AREA = null;
        public static final UnicodeBlock CJK_COMPATIBILITY_IDEOGRAPHS = null;
        public static final UnicodeBlock ALPHABETIC_PRESENTATION_FORMS = null;
        public static final UnicodeBlock ARABIC_PRESENTATION_FORMS_A = null;
        public static final UnicodeBlock COMBINING_HALF_MARKS = null;
        public static final UnicodeBlock CJK_COMPATIBILITY_FORMS = null;
        public static final UnicodeBlock SMALL_FORM_VARIANTS = null;
        public static final UnicodeBlock ARABIC_PRESENTATION_FORMS_B = null;
        public static final UnicodeBlock HALFWIDTH_AND_FULLWIDTH_FORMS = null;
        public static final UnicodeBlock SPECIALS = null;
        @Deprecated
        public static final UnicodeBlock SURROGATES_AREA = null;
        public static final UnicodeBlock SYRIAC = null;
        public static final UnicodeBlock THAANA = null;
        public static final UnicodeBlock SINHALA = null;
        public static final UnicodeBlock MYANMAR = null;
        public static final UnicodeBlock ETHIOPIC = null;
        public static final UnicodeBlock CHEROKEE = null;
        public static final UnicodeBlock UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS = null;
        public static final UnicodeBlock OGHAM = null;
        public static final UnicodeBlock RUNIC = null;
        public static final UnicodeBlock KHMER = null;
        public static final UnicodeBlock MONGOLIAN = null;
        public static final UnicodeBlock BRAILLE_PATTERNS = null;
        public static final UnicodeBlock CJK_RADICALS_SUPPLEMENT = null;
        public static final UnicodeBlock KANGXI_RADICALS = null;
        public static final UnicodeBlock IDEOGRAPHIC_DESCRIPTION_CHARACTERS = null;
        public static final UnicodeBlock BOPOMOFO_EXTENDED = null;
        public static final UnicodeBlock CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A = null;
        public static final UnicodeBlock YI_SYLLABLES = null;
        public static final UnicodeBlock YI_RADICALS = null;
        public static final UnicodeBlock CYRILLIC_SUPPLEMENTARY = null;
        public static final UnicodeBlock TAGALOG = null;
        public static final UnicodeBlock HANUNOO = null;
        public static final UnicodeBlock BUHID = null;
        public static final UnicodeBlock TAGBANWA = null;
        public static final UnicodeBlock LIMBU = null;
        public static final UnicodeBlock TAI_LE = null;
        public static final UnicodeBlock KHMER_SYMBOLS = null;
        public static final UnicodeBlock PHONETIC_EXTENSIONS = null;
        public static final UnicodeBlock MISCELLANEOUS_MATHEMATICAL_SYMBOLS_A = null;
        public static final UnicodeBlock SUPPLEMENTAL_ARROWS_A = null;
        public static final UnicodeBlock SUPPLEMENTAL_ARROWS_B = null;
        public static final UnicodeBlock MISCELLANEOUS_MATHEMATICAL_SYMBOLS_B = null;
        public static final UnicodeBlock SUPPLEMENTAL_MATHEMATICAL_OPERATORS = null;
        public static final UnicodeBlock MISCELLANEOUS_SYMBOLS_AND_ARROWS = null;
        public static final UnicodeBlock KATAKANA_PHONETIC_EXTENSIONS = null;
        public static final UnicodeBlock YIJING_HEXAGRAM_SYMBOLS = null;
        public static final UnicodeBlock VARIATION_SELECTORS = null;
        public static final UnicodeBlock LINEAR_B_IDEOGRAMS = null;
        public static final UnicodeBlock LINEAR_B_SYLLABARY = null;
        public static final UnicodeBlock AEGEAN_NUMBERS = null;
        public static final UnicodeBlock OLD_ITALIC = null;
        public static final UnicodeBlock GOTHIC = null;
        public static final UnicodeBlock UGARITIC = null;
        public static final UnicodeBlock DESERET = null;
        public static final UnicodeBlock SHAVIAN = null;
        public static final UnicodeBlock OSMANYA = null;
        public static final UnicodeBlock CYPRIOT_SYLLABARY = null;
        public static final UnicodeBlock BYZANTINE_MUSICAL_SYMBOLS = null;
        public static final UnicodeBlock MUSICAL_SYMBOLS = null;
        public static final UnicodeBlock TAI_XUAN_JING_SYMBOLS = null;
        public static final UnicodeBlock MATHEMATICAL_ALPHANUMERIC_SYMBOLS = null;
        public static final UnicodeBlock CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B = null;
        public static final UnicodeBlock CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT = null;
        public static final UnicodeBlock TAGS = null;
        public static final UnicodeBlock VARIATION_SELECTORS_SUPPLEMENT = null;
        public static final UnicodeBlock SUPPLEMENTARY_PRIVATE_USE_AREA_A = null;
        public static final UnicodeBlock SUPPLEMENTARY_PRIVATE_USE_AREA_B = null;
        public static final UnicodeBlock HIGH_SURROGATES = null;
        public static final UnicodeBlock HIGH_PRIVATE_USE_SURROGATES = null;
        public static final UnicodeBlock LOW_SURROGATES = null;

        public static UnicodeBlock of(char c) {
            throw new RuntimeException("skeleton method");
        }

        public static UnicodeBlock of(int codePoint) {
            throw new RuntimeException("skeleton method");
        }

        public static final UnicodeBlock forName(String blockName) {
            throw new RuntimeException("skeleton method");
        }
    }

    public Character(char value) {
        throw new RuntimeException("skeleton method");
    }

    public static Character valueOf(char c) {
        throw new RuntimeException("skeleton method");
    }

    public char charValue() {
        throw new RuntimeException("skeleton method");
    }

    public int hashCode() {
        throw new RuntimeException("skeleton method");
    }

    public boolean equals(@ReadOnly Object obj) {
        throw new RuntimeException("skeleton method");
    }

    public String toString() {
        throw new RuntimeException("skeleton method");
    }

    public static String toString(char c) {
        throw new RuntimeException("skeleton method");
    }

    public static boolean isValidCodePoint(int codePoint) {
        throw new RuntimeException("skeleton method");
    }

    public static boolean isSupplementaryCodePoint(int codePoint) {
        throw new RuntimeException("skeleton method");
    }

    public static boolean isHighSurrogate(char ch) {
        throw new RuntimeException("skeleton method");
    }

    public static boolean isLowSurrogate(char ch) {
        throw new RuntimeException("skeleton method");
    }

    public static boolean isSurrogatePair(char high, char low) {
        throw new RuntimeException("skeleton method");
    }

    public static int charCount(int codePoint) {
        throw new RuntimeException("skeleton method");
    }

    public static int toCodePoint(char high, char low) {
        throw new RuntimeException("skeleton method");
    }

    public static int codePointAt(CharSequence seq, int index) {
        throw new RuntimeException("skeleton method");
    }

    public static int codePointAt(char[] a, int index) {
        throw new RuntimeException("skeleton method");
    }

    public static int codePointAt(char[] a, int index, int limit) {
        throw new RuntimeException("skeleton method");
    }

    static int codePointAtImpl(char[] a, int index, int limit) {
        throw new RuntimeException("skeleton method");
    }

    public static int codePointBefore(CharSequence seq, int index) {
        throw new RuntimeException("skeleton method");
    }

    public static int codePointBefore(char[] a, int index) {
        throw new RuntimeException("skeleton method");
    }

    public static int codePointBefore(char[] a, int index, int start) {
        throw new RuntimeException("skeleton method");
    }

    static int codePointBeforeImpl(char[] a, int index, int start) {
        throw new RuntimeException("skeleton method");
    }

    public static int toChars(int codePoint, char[] dst, int dstIndex) {
        throw new RuntimeException("skeleton method");
    }

    public static char[] toChars(int codePoint) {
        throw new RuntimeException("skeleton method");
    }

    static void toSurrogates(int codePoint, char[] dst, int index) {
        throw new RuntimeException("skeleton method");
    }

    public static int codePointCount(CharSequence seq, int beginIndex, int endIndex) {
        throw new RuntimeException("skeleton method");
    }

    public static int codePointCount(char[] a, int offset, int count) {
        throw new RuntimeException("skeleton method");
    }

    static int codePointCountImpl(char[] a, int offset, int count) {
        throw new RuntimeException("skeleton method");
    }

    public static int offsetByCodePoints(CharSequence seq, int index,
                     int codePointOffset) {
        throw new RuntimeException("skeleton method");
    }

    public static int offsetByCodePoints(char[] a, int start, int count,
                     int index, int codePointOffset) {
        throw new RuntimeException("skeleton method");
    }

    static int offsetByCodePointsImpl(char[] a, int start, int count,
                      int index, int codePointOffset) {
        throw new RuntimeException("skeleton method");
    }

    public static boolean isLowerCase(char ch) {
        throw new RuntimeException("skeleton method");
    }

    public static boolean isLowerCase(int codePoint) {
        throw new RuntimeException("skeleton method");
    }

    public static boolean isUpperCase(char ch) {
        throw new RuntimeException("skeleton method");
    }

    public static boolean isUpperCase(int codePoint) {
        throw new RuntimeException("skeleton method");
    }

    public static boolean isTitleCase(char ch) {
        throw new RuntimeException("skeleton method");
    }

    public static boolean isTitleCase(int codePoint) {
        throw new RuntimeException("skeleton method");
    }

    public static boolean isDigit(char ch) {
        throw new RuntimeException("skeleton method");
    }

    public static boolean isDigit(int codePoint) {
        throw new RuntimeException("skeleton method");
    }

    public static boolean isDefined(char ch) {
        throw new RuntimeException("skeleton method");
    }

    public static boolean isDefined(int codePoint) {
        throw new RuntimeException("skeleton method");
    }

    public static boolean isLetter(char ch) {
        throw new RuntimeException("skeleton method");
    }

    public static boolean isLetter(int codePoint) {
        throw new RuntimeException("skeleton method");
    }

    public static boolean isLetterOrDigit(char ch) {
        throw new RuntimeException("skeleton method");
    }

    public static boolean isLetterOrDigit(int codePoint) {
        throw new RuntimeException("skeleton method");
    }

    @Deprecated
    public static boolean isJavaLetter(char ch) {
        throw new RuntimeException("skeleton method");
    }

    @Deprecated
    public static boolean isJavaLetterOrDigit(char ch) {
        throw new RuntimeException("skeleton method");
    }

    public static boolean isJavaIdentifierStart(char ch) {
        throw new RuntimeException("skeleton method");
    }

    public static boolean isJavaIdentifierStart(int codePoint) {
        throw new RuntimeException("skeleton method");
    }

    public static boolean isJavaIdentifierPart(char ch) {
        throw new RuntimeException("skeleton method");
    }

    public static boolean isJavaIdentifierPart(int codePoint) {
        throw new RuntimeException("skeleton method");
    }

    public static boolean isUnicodeIdentifierStart(char ch) {
        throw new RuntimeException("skeleton method");
    }

    public static boolean isUnicodeIdentifierStart(int codePoint) {
        throw new RuntimeException("skeleton method");
    }

    public static boolean isUnicodeIdentifierPart(char ch) {
        throw new RuntimeException("skeleton method");
    }

    public static boolean isUnicodeIdentifierPart(int codePoint) {
        throw new RuntimeException("skeleton method");
    }

    public static boolean isIdentifierIgnorable(char ch) {
        throw new RuntimeException("skeleton method");
    }

    public static boolean isIdentifierIgnorable(int codePoint) {
        throw new RuntimeException("skeleton method");
    }

    public static char toLowerCase(char ch) {
        throw new RuntimeException("skeleton method");
    }

    public static int toLowerCase(int codePoint) {
        throw new RuntimeException("skeleton method");
    }

    public static char toUpperCase(char ch) {
        throw new RuntimeException("skeleton method");
    }

    public static int toUpperCase(int codePoint) {
        throw new RuntimeException("skeleton method");
    }

    public static char toTitleCase(char ch) {
        throw new RuntimeException("skeleton method");
    }

    public static int toTitleCase(int codePoint) {
        throw new RuntimeException("skeleton method");
    }

    public static int digit(char ch, int radix) {
        throw new RuntimeException("skeleton method");
    }

    public static int digit(int codePoint, int radix) {
        throw new RuntimeException("skeleton method");
    }

    public static int getNumericValue(char ch) {
        throw new RuntimeException("skeleton method");
    }

    public static int getNumericValue(int codePoint) {
        throw new RuntimeException("skeleton method");
    }

    @Deprecated
    public static boolean isSpace(char ch) {
        throw new RuntimeException("skeleton method");
    }

    public static boolean isSpaceChar(char ch) {
        throw new RuntimeException("skeleton method");
    }

    public static boolean isSpaceChar(int codePoint) {
        throw new RuntimeException("skeleton method");
    }

    public static boolean isWhitespace(char ch) {
        throw new RuntimeException("skeleton method");
    }

    public static boolean isWhitespace(int codePoint) {
        throw new RuntimeException("skeleton method");
    }

    public static boolean isISOControl(char ch) {
        throw new RuntimeException("skeleton method");
    }

    public static boolean isISOControl(int codePoint) {
        throw new RuntimeException("skeleton method");
    }

    public static int getType(char ch) {
        throw new RuntimeException("skeleton method");
    }

    public static int getType(int codePoint) {
        throw new RuntimeException("skeleton method");
    }

    public static char forDigit(int digit, int radix) {
        throw new RuntimeException("skeleton method");
    }

    public static byte getDirectionality(char ch) {
        throw new RuntimeException("skeleton method");
    }

    public static byte getDirectionality(int codePoint) {
        throw new RuntimeException("skeleton method");
    }

    public static boolean isMirrored(char ch) {
        throw new RuntimeException("skeleton method");
    }

    public static boolean isMirrored(int codePoint) {
        throw new RuntimeException("skeleton method");
    }

    public int compareTo( Character anotherCharacter) {
        throw new RuntimeException("skeleton method");
    }

    static int toUpperCaseEx(int codePoint) {
        throw new RuntimeException("skeleton method");
    }

    static char[] toUpperCaseCharArray(int codePoint) {
        throw new RuntimeException("skeleton method");
    }

    public static final int SIZE = 16;

    public static char reverseBytes(char ch) {
        throw new RuntimeException("skeleton method");
    }
}
