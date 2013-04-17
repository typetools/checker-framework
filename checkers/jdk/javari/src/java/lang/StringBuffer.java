package java.lang;
import checkers.javari.quals.*;

public final class StringBuffer
    extends AbstractStringBuilder
    implements java.io.Serializable, CharSequence {

    static final long serialVersionUID = 3388685877147921107L;

    public StringBuffer() {
        throw new RuntimeException("skeleton method");
    }

    public StringBuffer(int capacity) {
        throw new RuntimeException("skeleton method");
    }

    public StringBuffer(String str) {
        throw new RuntimeException("skeleton method");
    }

    public StringBuffer(@ReadOnly CharSequence seq) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized int length(@ReadOnly StringBuffer this) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized int capacity(@ReadOnly StringBuffer this) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized void ensureCapacity(int minimumCapacity) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized void trimToSize() {
        throw new RuntimeException("skeleton method");
    }

    public synchronized void setLength(int newLength) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized char charAt(@ReadOnly StringBuffer this, int index) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized int codePointAt(@ReadOnly StringBuffer this, int index) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized int codePointBefore(@ReadOnly StringBuffer this, int index) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized int codePointCount(@ReadOnly StringBuffer this, int beginIndex, int endIndex) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized int offsetByCodePoints(@ReadOnly StringBuffer this, int index, int codePointOffset) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized void getChars(@ReadOnly StringBuffer this, int srcBegin, int srcEnd, char dst[],
                                      int dstBegin) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized void setCharAt(int index, char ch) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized StringBuffer append(@ReadOnly Object obj) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized StringBuffer append(String str) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized StringBuffer append(@ReadOnly StringBuffer sb) {
        throw new RuntimeException("skeleton method");
    }

    public StringBuffer append(@ReadOnly CharSequence s) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized StringBuffer append(@ReadOnly CharSequence s, int start, int end) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized StringBuffer append(char @ReadOnly [] str) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized StringBuffer append(char @ReadOnly [] str, int offset, int len) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized StringBuffer append(boolean b) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized StringBuffer append(char c) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized StringBuffer append(int i) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized StringBuffer appendCodePoint(int codePoint) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized StringBuffer append(long lng) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized StringBuffer append(float f) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized StringBuffer append(double d) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized StringBuffer delete(int start, int end) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized StringBuffer deleteCharAt(int index) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized StringBuffer replace(int start, int end,  String str) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized String substring(@ReadOnly StringBuffer this, int start) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized @ReadOnly CharSequence subSequence(@ReadOnly StringBuffer this, int start, int end) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized String substring(@ReadOnly StringBuffer this, int start, int end) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized StringBuffer insert(int index, char @ReadOnly [] str, int offset, int len) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized StringBuffer insert(int offset, @ReadOnly Object obj) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized StringBuffer insert(int offset, String str) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized  StringBuffer insert(int offset, char @ReadOnly [] str) {
        throw new RuntimeException("skeleton method");
    }

    public StringBuffer insert(int dstOffset, @ReadOnly CharSequence s) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized StringBuffer insert(int dstOffset, @ReadOnly CharSequence s, int start, int end) {
        throw new RuntimeException("skeleton method");
    }

    public StringBuffer insert(int offset, boolean b) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized StringBuffer insert(int offset, char c) {
        throw new RuntimeException("skeleton method");
    }

    public StringBuffer insert(int offset, int i) {
        throw new RuntimeException("skeleton method");
    }

    public StringBuffer insert(int offset, long l) {
        throw new RuntimeException("skeleton method");
    }

    public StringBuffer insert(int offset, float f) {
        throw new RuntimeException("skeleton method");
    }

    public StringBuffer insert(int offset, double d) {
        throw new RuntimeException("skeleton method");
    }

    public int indexOf(@ReadOnly StringBuffer this, String str) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized int indexOf(@ReadOnly StringBuffer this, String str, int fromIndex) {
        throw new RuntimeException("skeleton method");
    }

    public int lastIndexOf(@ReadOnly StringBuffer this,  String str) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized int lastIndexOf(@ReadOnly StringBuffer this, String str, int fromIndex) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized StringBuffer reverse() {
        throw new RuntimeException("skeleton method");
    }

    public synchronized String toString(@ReadOnly StringBuffer this) {
        throw new RuntimeException("skeleton method");
    }

    private static final java.io.ObjectStreamField[] serialPersistentFields =
    {
        new java.io.ObjectStreamField("value", char[].class),
        new java.io.ObjectStreamField("count", Integer.TYPE),
        new java.io.ObjectStreamField("shared", Boolean.TYPE),
    };

    private synchronized void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException {
        throw new RuntimeException("skeleton method");
    }

    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        throw new RuntimeException("skeleton method");
    }
}
