package java.lang;
import checkers.javari.quals.*;

import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Formatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final @ReadOnly class String
    implements java.io.Serializable, Comparable<String>, CharSequence
{
    private final char value[];
    private final int offset;
    private final int count;
    private @Assignable int hash;
    private static final long serialVersionUID = -6849794470754667710L;

    private static final ObjectStreamField[] serialPersistentFields =
        new ObjectStreamField[0];

    public String() {
        throw new RuntimeException("skeleton method");
    }

    public String(String original) {
        throw new RuntimeException("skeleton method");
    }

    public String(char @ReadOnly [] value) {
        throw new RuntimeException("skeleton method");
    }

    public String(char @ReadOnly [] value, int offset, int count) {
        throw new RuntimeException("skeleton method");
    }

    public String(int @ReadOnly [] codePoints, int offset, int count) {
        throw new RuntimeException("skeleton method");
    }

    @Deprecated
    public String(byte @ReadOnly [] ascii, int hibyte, int offset, int count) {
        throw new RuntimeException("skeleton method");
    }

    @Deprecated
    public String(byte @ReadOnly [] ascii, int hibyte) {
        throw new RuntimeException("skeleton method");
    }

    private static void checkBounds(byte @ReadOnly [] bytes, int offset, int length) {
        throw new RuntimeException("skeleton method");
    }

    public String(byte @ReadOnly [] bytes, int offset, int length, String charsetName)
    throws UnsupportedEncodingException {
        throw new RuntimeException("skeleton method");
    }

    public String(byte @ReadOnly [] bytes, int offset, int length, Charset charset) {
        throw new RuntimeException("skeleton method");
    }

    public String(byte @ReadOnly [] bytes, String charsetName)
    throws UnsupportedEncodingException {
        throw new RuntimeException("skeleton method");
    }

    public String(byte @ReadOnly [] bytes,  Charset charset) {
        throw new RuntimeException("skeleton method");
    }

    public String(byte @ReadOnly [] bytes, int offset, int length) {
        throw new RuntimeException("skeleton method");
    }

    public String(byte @ReadOnly [] bytes) {
        throw new RuntimeException("skeleton method");
    }

    public String(@ReadOnly StringBuffer buffer) {
        throw new RuntimeException("skeleton method");
    }

    public String(@ReadOnly StringBuilder builder) {
        throw new RuntimeException("skeleton method");
    }

    String(int offset, int count, char @ReadOnly [] value) {
        throw new RuntimeException("skeleton method");
    }

    public int length() {
        throw new RuntimeException("skeleton method");
    }

    public boolean isEmpty() {
        throw new RuntimeException("skeleton method");
    }

    public char charAt(int index) {
        throw new RuntimeException("skeleton method");
    }

    public int codePointAt(int index) {
        throw new RuntimeException("skeleton method");
    }

    public int codePointBefore(int index) {
        throw new RuntimeException("skeleton method");
    }

    public int codePointCount(int beginIndex, int endIndex) {
        throw new RuntimeException("skeleton method");
    }

    public int offsetByCodePoints(int index, int codePointOffset) {
        throw new RuntimeException("skeleton method");
    }

    void getChars(char dst[], int dstBegin) {
        throw new RuntimeException("skeleton method");
    }

    public void getChars(int srcBegin, int srcEnd, char dst[], int dstBegin) {
        throw new RuntimeException("skeleton method");
    }

    @Deprecated
    public void getBytes(int srcBegin, int srcEnd, byte dst[], int dstBegin) {
        throw new RuntimeException("skeleton method");
    }

    public byte[] getBytes(@ReadOnly String this,  String charsetName)
    throws UnsupportedEncodingException {
        throw new RuntimeException("skeleton method");
    }

    public byte[] getBytes(Charset charset) {
        throw new RuntimeException("skeleton method");
    }

    public byte[] getBytes() {
        throw new RuntimeException("skeleton method");
    }

    public boolean equals(@ReadOnly Object anObject) {
        throw new RuntimeException("skeleton method");
    }

    public boolean contentEquals(@ReadOnly StringBuffer sb) {
        throw new RuntimeException("skeleton method");
    }

    public boolean contentEquals(@ReadOnly CharSequence cs) {
        throw new RuntimeException("skeleton method");
    }

    public boolean equalsIgnoreCase(String anotherString) {
        throw new RuntimeException("skeleton method");
    }

    public int compareTo(String anotherString) {
        throw new RuntimeException("skeleton method");
    }

    public static final Comparator<String> CASE_INSENSITIVE_ORDER
                                         = new CaseInsensitiveComparator();
    private static @ReadOnly class CaseInsensitiveComparator
                         implements Comparator<String>, java.io.Serializable {
        private static final long serialVersionUID = 8575799808933029326L;
        public int compare(String s1,  String s2) {
        throw new RuntimeException("skeleton method");
        }
    }

    public int compareToIgnoreCase(String str) {
        throw new RuntimeException("skeleton method");
    }

    public boolean regionMatches(int toffset, String other, int ooffset, int len) {
        throw new RuntimeException("skeleton method");
    }

    public boolean regionMatches(boolean ignoreCase, int toffset,
                                 String other, int ooffset, int len) {
        throw new RuntimeException("skeleton method");
    }

    public boolean startsWith(String prefix, int toffset) {
        throw new RuntimeException("skeleton method");
    }

    public boolean startsWith(String prefix) {
        throw new RuntimeException("skeleton method");
    }

    public boolean endsWith(String suffix) {
        throw new RuntimeException("skeleton method");
    }

    public int hashCode() {
        throw new RuntimeException("skeleton method");
    }

    public int indexOf(int ch) {
        throw new RuntimeException("skeleton method");
    }

    public int indexOf(int ch, int fromIndex) {
        throw new RuntimeException("skeleton method");
    }

    public int lastIndexOf(int ch) {
        throw new RuntimeException("skeleton method");
    }

    public int lastIndexOf(int ch, int fromIndex) {
        throw new RuntimeException("skeleton method");
    }

    public int indexOf(String str) {
        throw new RuntimeException("skeleton method");
    }

    public int indexOf(String str, int fromIndex) {
        throw new RuntimeException("skeleton method");
    }

    static int indexOf(char @ReadOnly [] source, int sourceOffset, int sourceCount,
                       char @ReadOnly [] target, int targetOffset, int targetCount,
                       int fromIndex) {
        throw new RuntimeException("skeleton method");
    }

    public int lastIndexOf(String str) {
        throw new RuntimeException("skeleton method");
    }

    public int lastIndexOf(String str, int fromIndex) {
        throw new RuntimeException("skeleton method");
    }

    static int lastIndexOf(char @ReadOnly [] source, int sourceOffset, int sourceCount,
                           char @ReadOnly [] target, int targetOffset, int targetCount,
                           int fromIndex) {
        throw new RuntimeException("skeleton method");
    }

    public String substring(int beginIndex) {
        throw new RuntimeException("skeleton method");
    }

    public String substring(int beginIndex, int endIndex) {
        throw new RuntimeException("skeleton method");
    }

    public CharSequence subSequence(int beginIndex, int endIndex) {
        throw new RuntimeException("skeleton method");
    }

    public String concat(String str) {
        throw new RuntimeException("skeleton method");
    }

    public String replace(char oldChar, char newChar) {
        throw new RuntimeException("skeleton method");
    }

    public boolean matches(String regex) {
        throw new RuntimeException("skeleton method");
    }

    public boolean contains(@ReadOnly CharSequence s) {
        throw new RuntimeException("skeleton method");
    }

    public String replaceFirst(String regex, String replacement) {
        throw new RuntimeException("skeleton method");
    }

    public String replaceAll(String regex, String replacement) {
        throw new RuntimeException("skeleton method");
    }

    public String replace(CharSequence target, CharSequence replacement) {
        throw new RuntimeException("skeleton method");
    }

    public String[] split(String regex, int limit) {
        throw new RuntimeException("skeleton method");
    }

    public String[] split(String regex) {
        throw new RuntimeException("skeleton method");
    }

    public String toLowerCase(Locale locale) {
        throw new RuntimeException("skeleton method");
    }

    public String toLowerCase() {
        throw new RuntimeException("skeleton method");
    }

    public String toUpperCase(Locale locale) {
        throw new RuntimeException("skeleton method");
    }

    public String toUpperCase() {
        throw new RuntimeException("skeleton method");
    }

    public String trim() {
        throw new RuntimeException("skeleton method");
    }

    public String toString() {
        throw new RuntimeException("skeleton method");
    }

    public char[] toCharArray() {
        throw new RuntimeException("skeleton method");
    }

    public static String format(String format, @ReadOnly Object ... args) {
        throw new RuntimeException("skeleton method");
    }

    public static String format( Locale l, String format, @ReadOnly Object ... args) {
        throw new RuntimeException("skeleton method");
    }

    public static String valueOf(@ReadOnly Object obj) {
        throw new RuntimeException("skeleton method");
    }

    public static String valueOf(char @ReadOnly [] data) {
        throw new RuntimeException("skeleton method");
    }

    public static String valueOf(char @ReadOnly [] data, int offset, int count) {
        throw new RuntimeException("skeleton method");
    }

    public static String copyValueOf(char @ReadOnly [] data, int offset, int count) {
        throw new RuntimeException("skeleton method");
    }

    public static String copyValueOf(char @ReadOnly [] data) {
        throw new RuntimeException("skeleton method");
    }

    public static String valueOf(boolean b) {
        throw new RuntimeException("skeleton method");
    }

    public static String valueOf(char c) {
        throw new RuntimeException("skeleton method");
    }

    public static String valueOf(int i) {
        throw new RuntimeException("skeleton method");
    }

    public static String valueOf(long l) {
        throw new RuntimeException("skeleton method");
    }

    public static String valueOf(float f) {
        throw new RuntimeException("skeleton method");
    }

    public static String valueOf(double d) {
        throw new RuntimeException("skeleton method");
    }

    public native String intern();
}
