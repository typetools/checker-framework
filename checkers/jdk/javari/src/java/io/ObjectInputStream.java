package java.io;

import java.io.ObjectStreamClass.WeakClassKey;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import static java.io.ObjectStreamClass.processQueue;

import checkers.javari.quals.*;

@SuppressWarnings("rawtypes")
public class ObjectInputStream
    extends InputStream implements ObjectInput, ObjectStreamConstants
{
    private static final int NULL_HANDLE = -1;
    private static final Object unsharedMarker = new Object();
    private static final HashMap<String, Class<?>> primClasses
        = new HashMap<String, Class<?>>(8, 1.0F);
    private static class Caches {
        static final ConcurrentMap<WeakClassKey,Boolean> subclassAudits =
            new ConcurrentHashMap<WeakClassKey,Boolean>();
        static final ReferenceQueue<Class<?>> subclassAuditsQueue =
            new ReferenceQueue<Class<?>>();
    }
    private final BlockDataInputStream bin;
    private final ValidationList vlist;
    private int depth;
    private boolean closed;
    private final HandleTable handles;
    private int passHandle = NULL_HANDLE;
    private boolean defaultDataEnd = false;
    private byte[] primVals;
    private final boolean enableOverride;
    private boolean enableResolve;
    private CallbackContext curContext;
    public ObjectInputStream(InputStream in) throws IOException { throw new RuntimeException("skeleton method"); }
    protected ObjectInputStream() throws IOException, SecurityException { throw new RuntimeException("skeleton method"); }
    public final Object readObject() throws IOException, ClassNotFoundException { throw new RuntimeException("skeleton method"); }
    protected Object readObjectOverride() throws IOException, ClassNotFoundException { throw new RuntimeException("skeleton method"); }
    public Object readUnshared() throws IOException, ClassNotFoundException { throw new RuntimeException("skeleton method"); }
    public void defaultReadObject() throws IOException, ClassNotFoundException { throw new RuntimeException("skeleton method"); }
    // return value was @PolyRead in a previous annotation of the library
    public ObjectInputStream.GetField readFields() throws IOException, ClassNotFoundException { throw new RuntimeException("skeleton method"); }
    public void registerValidation(ObjectInputValidation obj, int prio) throws NotActiveException, InvalidObjectException { throw new RuntimeException("skeleton method"); }
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException { throw new RuntimeException("skeleton method"); }
    protected Class<?> resolveProxyClass(String @ReadOnly [] interfaces) throws IOException, ClassNotFoundException { throw new RuntimeException("skeleton method"); }
    protected Object resolveObject(@ReadOnly ObjectInputStream this, Object obj) throws IOException { throw new RuntimeException("skeleton method"); }
    protected boolean enableResolveObject(boolean enable) throws SecurityException { throw new RuntimeException("skeleton method"); }
    protected void readStreamHeader() throws IOException, StreamCorruptedException { throw new RuntimeException("skeleton method"); }
    protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException { throw new RuntimeException("skeleton method"); }
    public int read() throws IOException { throw new RuntimeException("skeleton method"); }
    public int read(byte @ReadOnly [] buf, int off, int len) throws IOException { throw new RuntimeException("skeleton method"); }
    public int available() throws IOException { throw new RuntimeException("skeleton method"); }
    public void close() throws IOException { throw new RuntimeException("skeleton method"); }
    public boolean readBoolean() throws IOException { throw new RuntimeException("skeleton method"); }
    public byte readByte() throws IOException  { throw new RuntimeException("skeleton method"); }
    public int readUnsignedByte()  throws IOException { throw new RuntimeException("skeleton method"); }
    public char readChar()  throws IOException { throw new RuntimeException("skeleton method"); }
    public short readShort()  throws IOException { throw new RuntimeException("skeleton method"); }
    public int readUnsignedShort() throws IOException { throw new RuntimeException("skeleton method"); }
    public int readInt()  throws IOException { throw new RuntimeException("skeleton method"); }
    public long readLong()  throws IOException { throw new RuntimeException("skeleton method"); }
    public float readFloat() throws IOException { throw new RuntimeException("skeleton method"); }
    public double readDouble() throws IOException { throw new RuntimeException("skeleton method"); }
    public void readFully(byte @ReadOnly [] buf) throws IOException { throw new RuntimeException("skeleton method"); }
    public void readFully(byte @ReadOnly [] buf, int off, int len) throws IOException { throw new RuntimeException("skeleton method"); }
    public int skipBytes(int len) throws IOException { throw new RuntimeException("skeleton method"); }
    @Deprecated
    public String readLine() throws IOException { throw new RuntimeException("skeleton method"); }
    public String readUTF() throws IOException { throw new RuntimeException("skeleton method"); }
    public static abstract class GetField {
        public abstract ObjectStreamClass getObjectStreamClass(@ReadOnly GetField this);
        public abstract boolean defaulted(@ReadOnly GetField this, String name) throws IOException;
        public abstract boolean get(@ReadOnly GetField this, String name, boolean val) throws IOException; 
        public abstract byte get(@ReadOnly GetField this, String name, byte val) throws IOException;
        public abstract char get(@ReadOnly GetField this, String name, char val) throws IOException;
        public abstract short get(@ReadOnly GetField this, String name, short val) throws IOException;
        public abstract int get(@ReadOnly GetField this, String name, int val) throws IOException;
        public abstract long get(@ReadOnly GetField this, String name, long val) throws IOException;
        public abstract float get(@ReadOnly GetField this, String name, float val) throws IOException;
        public abstract double get(@ReadOnly GetField this, String name, double val) throws IOException;
        public abstract @PolyRead Object get(@ReadOnly GetField this, String name, @PolyRead Object val) throws IOException;
    }
    private void verifySubclass(@ReadOnly ObjectInputStream this) { throw new RuntimeException("skeleton method"); }
    private static boolean auditSubclass(final @ReadOnly Class<?> subcl) { throw new RuntimeException("skeleton method"); }
    private void clear() { throw new RuntimeException("skeleton method"); }
    private Object readObject0(boolean unshared) throws IOException { throw new RuntimeException("skeleton method"); }
    private Object checkResolve(@ReadOnly ObjectInputStream this, Object obj) throws IOException { throw new RuntimeException("skeleton method"); }
    String readTypeString() throws IOException { throw new RuntimeException("skeleton method"); }
    private Object readNull() throws IOException { throw new RuntimeException("skeleton method"); }
    private Object readHandle(boolean unshared) throws IOException { throw new RuntimeException("skeleton method"); }
    private Class readClass(boolean unshared) throws IOException { throw new RuntimeException("skeleton method"); }
    private ObjectStreamClass readClassDesc(boolean unshared) throws IOException { throw new RuntimeException("skeleton method"); }
    private ObjectStreamClass readProxyDesc(boolean unshared) throws IOException { throw new RuntimeException("skeleton method"); }
    private ObjectStreamClass readNonProxyDesc(boolean unshared) throws IOException { throw new RuntimeException("skeleton method"); }
    private String readString(boolean unshared) throws IOException { throw new RuntimeException("skeleton method"); }
    private Object readArray(boolean unshared) throws IOException { throw new RuntimeException("skeleton method"); }
    private Enum readEnum(boolean unshared) throws IOException { throw new RuntimeException("skeleton method"); }
    private Object readOrdinaryObject(boolean unshared) throws IOException { throw new RuntimeException("skeleton method"); }
    private void readExternalData(Externalizable obj, ObjectStreamClass desc) throws IOException { throw new RuntimeException("skeleton method"); }
    private void readSerialData(Object obj, ObjectStreamClass desc) throws IOException { throw new RuntimeException("skeleton method"); }
    private void skipCustomData() throws IOException { throw new RuntimeException("skeleton method"); }
    private void defaultReadFields(Object obj, ObjectStreamClass desc) throws IOException { throw new RuntimeException("skeleton method"); }
    private IOException readFatalException() throws IOException { throw new RuntimeException("skeleton method"); }
    private void handleReset() throws StreamCorruptedException { throw new RuntimeException("skeleton method"); }
    // REMIND: remove once hotspot inlines Float.intBitsToFloat
    private static native void bytesToFloats(byte @ReadOnly [] src, int srcpos,
                                             float[] dst, int dstpos,
                                             int nfloats);
    // REMIND: remove once hotspot inlines Double.longBitsToDouble
    private static native void bytesToDoubles(byte @ReadOnly [] src, int srcpos,
                                              double[] dst, int dstpos,
                                              int ndoubles);
    // REMIND: change name to something more accurate?
    private static native ClassLoader latestUserDefinedLoader();
    private class GetFieldImpl extends GetField {
        private final ObjectStreamClass desc;
        private final byte[] primVals;
        private final Object[] objVals;
        private final int[] objHandles;
        GetFieldImpl(ObjectStreamClass desc) { throw new RuntimeException("skeleton method"); }
        public ObjectStreamClass getObjectStreamClass(@ReadOnly GetFieldImpl this) { throw new RuntimeException("skeleton method"); }
        public boolean defaulted(@ReadOnly GetFieldImpl this, String name) throws IOException { throw new RuntimeException("skeleton method"); }
        public boolean get(@ReadOnly GetFieldImpl this, String name, boolean val) throws IOException { throw new RuntimeException("skeleton method"); }
        public byte get(@ReadOnly GetFieldImpl this, String name, byte val) throws IOException { throw new RuntimeException("skeleton method"); }
        public char get(@ReadOnly GetFieldImpl this, String name, char val) throws IOException { throw new RuntimeException("skeleton method"); }
        public short get(@ReadOnly GetFieldImpl this, String name, short val) throws IOException { throw new RuntimeException("skeleton method"); }
        public int get(@ReadOnly GetFieldImpl this, String name, int val) throws IOException { throw new RuntimeException("skeleton method"); }
        public float get(@ReadOnly GetFieldImpl this, String name, float val) throws IOException { throw new RuntimeException("skeleton method"); }
        public long get(@ReadOnly GetFieldImpl this, String name, long val) throws IOException { throw new RuntimeException("skeleton method"); }
        public double get(@ReadOnly GetFieldImpl this, String name, double val) throws IOException { throw new RuntimeException("skeleton method"); }
        public Object get(@ReadOnly GetFieldImpl this, String name, @ReadOnly Object val) throws IOException { throw new RuntimeException("skeleton method"); }
        void readFields() throws IOException { throw new RuntimeException("skeleton method"); }
        private int getFieldOffset(@ReadOnly GetFieldImpl this, String name, Class type) { throw new RuntimeException("skeleton method"); }
    }
    private static class ValidationList {
        private static class Callback {
            final ObjectInputValidation obj;
            final int priority;
            Callback next;
            final AccessControlContext acc;
            Callback(ObjectInputValidation obj, int priority, Callback next,
                AccessControlContext acc)
            { throw new RuntimeException("skeleton method"); }
        }
        private Callback list;
        ValidationList() { throw new RuntimeException("skeleton method"); }
        void register(ObjectInputValidation obj, int priority) throws InvalidObjectException { throw new RuntimeException("skeleton method"); }
        void doCallbacks() throws InvalidObjectException { throw new RuntimeException("skeleton method"); }
        public void clear() { throw new RuntimeException("skeleton method"); }
    }
    private static class PeekInputStream extends InputStream {
        private final InputStream in;
        private int peekb = -1;
        PeekInputStream(InputStream in) { throw new RuntimeException("skeleton method"); }
        int peek(@ReadOnly PeekInputStream this) throws IOException { throw new RuntimeException("skeleton method"); }
        public int read() throws IOException { throw new RuntimeException("skeleton method"); }
        public int read(byte[] b, int off, int len) throws IOException { throw new RuntimeException("skeleton method"); }
        void readFully(byte[] b, int off, int len) throws IOException { throw new RuntimeException("skeleton method"); }
        public long skip(long n) throws IOException { throw new RuntimeException("skeleton method"); }
        public int available(@ReadOnly PeekInputStream this) throws IOException { throw new RuntimeException("skeleton method"); }
        public void close() throws IOException { throw new RuntimeException("skeleton method"); }
    }
    private class BlockDataInputStream
        extends InputStream implements DataInput
    {
        private static final int MAX_BLOCK_SIZE = 1024;
        private static final int MAX_HEADER_SIZE = 5;
        private static final int CHAR_BUF_SIZE = 256;
        private static final int HEADER_BLOCKED = -2;
        private final byte[] buf = new byte[MAX_BLOCK_SIZE];
        private final byte[] hbuf = new byte[MAX_HEADER_SIZE];
        private final char[] cbuf = new char[CHAR_BUF_SIZE];
        private boolean blkmode = false;
        // block data state fields; values meaningful only when blkmode true
        private int pos = 0;
        private int end = -1;
        private int unread = 0;
        private final PeekInputStream in;
        private final DataInputStream din;
        BlockDataInputStream(InputStream in) { throw new RuntimeException("skeleton method"); }
        boolean setBlockDataMode(boolean newmode) throws IOException { throw new RuntimeException("skeleton method"); }
        boolean getBlockDataMode() { throw new RuntimeException("skeleton method"); }
        void skipBlockData() throws IOException { throw new RuntimeException("skeleton method"); }
        private int readBlockHeader(boolean canBlock) throws IOException { throw new RuntimeException("skeleton method"); }
        private void refill() throws IOException { throw new RuntimeException("skeleton method"); }
        int currentBlockRemaining(@ReadOnly BlockDataInputStream this) { throw new RuntimeException("skeleton method"); }
        int peek(@ReadOnly BlockDataInputStream this) throws IOException { throw new RuntimeException("skeleton method"); }
        byte peekByte(@ReadOnly BlockDataInputStream this) throws IOException { throw new RuntimeException("skeleton method"); }
        public int read() throws IOException { throw new RuntimeException("skeleton method"); }
        public int read(byte[] b, int off, int len) throws IOException { throw new RuntimeException("skeleton method"); }
        public long skip(long len) throws IOException { throw new RuntimeException("skeleton method"); }
        public int available(@ReadOnly BlockDataInputStream this) throws IOException { throw new RuntimeException("skeleton method"); }
        public void close() throws IOException { throw new RuntimeException("skeleton method"); }
        int read(byte[] b, int off, int len, boolean copy) throws IOException { throw new RuntimeException("skeleton method"); }
        public void readFully(byte[] b) throws IOException { throw new RuntimeException("skeleton method"); }
        public void readFully(byte[] b, int off, int len) throws IOException { throw new RuntimeException("skeleton method"); }
        public void readFully(byte[] b, int off, int len, boolean copy) throws IOException { throw new RuntimeException("skeleton method"); }
        public int skipBytes(int n) throws IOException { throw new RuntimeException("skeleton method"); }
        public boolean readBoolean() throws IOException { throw new RuntimeException("skeleton method"); }
        public byte readByte() throws IOException { throw new RuntimeException("skeleton method"); }
        public int readUnsignedByte() throws IOException { throw new RuntimeException("skeleton method"); }
        public char readChar() throws IOException { throw new RuntimeException("skeleton method"); }
        public short readShort() throws IOException { throw new RuntimeException("skeleton method"); }
        public int readUnsignedShort() throws IOException { throw new RuntimeException("skeleton method"); }
        public int readInt() throws IOException { throw new RuntimeException("skeleton method"); }
        public float readFloat() throws IOException { throw new RuntimeException("skeleton method"); }
        public long readLong() throws IOException { throw new RuntimeException("skeleton method"); }
        public double readDouble() throws IOException { throw new RuntimeException("skeleton method"); }
        public String readUTF() throws IOException { throw new RuntimeException("skeleton method"); }
        public String readLine() throws IOException { throw new RuntimeException("skeleton method"); }
        void readBooleans(boolean[] v, int off, int len) throws IOException { throw new RuntimeException("skeleton method"); }
        void readChars(char[] v, int off, int len) throws IOException { throw new RuntimeException("skeleton method"); }
        void readShorts(short[] v, int off, int len) throws IOException { throw new RuntimeException("skeleton method"); }
        void readInts(int[] v, int off, int len) throws IOException { throw new RuntimeException("skeleton method"); }
        void readFloats(float[] v, int off, int len) throws IOException { throw new RuntimeException("skeleton method"); }
        void readLongs(long[] v, int off, int len) throws IOException { throw new RuntimeException("skeleton method"); }
        void readDoubles(double[] v, int off, int len) throws IOException { throw new RuntimeException("skeleton method"); }
        String readLongUTF() throws IOException { throw new RuntimeException("skeleton method"); }
        private String readUTFBody(long utflen) throws IOException { throw new RuntimeException("skeleton method"); }
        private long readUTFSpan(StringBuilder sbuf, long utflen) throws IOException { throw new RuntimeException("skeleton method"); }
        private int readUTFChar(StringBuilder sbuf, long utflen) throws IOException { throw new RuntimeException("skeleton method"); }
    }
    private static class HandleTable {
        private static final byte STATUS_OK = 1;
        private static final byte STATUS_UNKNOWN = 2;
        private static final byte STATUS_EXCEPTION = 3;
        byte[] status;
        Object[] entries;
        HandleList[] deps;
        int lowDep = -1;
        int size = 0;
        HandleTable(int initialCapacity) { throw new RuntimeException("skeleton method"); }
        int assign(Object obj) { throw new RuntimeException("skeleton method"); }
        void markDependency(int dependent, int target) { throw new RuntimeException("skeleton method"); }
        void markException(int handle, ClassNotFoundException ex) { throw new RuntimeException("skeleton method"); }
        void finish(int handle) { throw new RuntimeException("skeleton method"); }
        void setObject(int handle, Object obj) { throw new RuntimeException("skeleton method"); }
        Object lookupObject(@ReadOnly HandleTable this, int handle) { throw new RuntimeException("skeleton method"); }
        ClassNotFoundException lookupException(@ReadOnly HandleTable this, int handle) { throw new RuntimeException("skeleton method"); }
        void clear() { throw new RuntimeException("skeleton method"); }
        int size(@ReadOnly HandleTable this) { throw new RuntimeException("skeleton method"); }
        private void grow() { throw new RuntimeException("skeleton method"); }
        private static class HandleList {
            private int[] list = new int[4];
            private int size = 0;
            public HandleList() { throw new RuntimeException("skeleton method"); }
            public void add(int handle) { throw new RuntimeException("skeleton method"); }
            public int get(@ReadOnly HandleList this, int index) { throw new RuntimeException("skeleton method"); }
            public int size(@ReadOnly HandleList this) { throw new RuntimeException("skeleton method"); }
        }
    }
    private static Object cloneArray(@ReadOnly Object array) { throw new RuntimeException("skeleton method"); }
    private static class CallbackContext {
        private final Object obj;
        private final ObjectStreamClass desc;
        private final AtomicBoolean used = new AtomicBoolean();
        public CallbackContext(Object obj, ObjectStreamClass desc) { throw new RuntimeException("skeleton method"); }
        public Object getObj() throws NotActiveException { throw new RuntimeException("skeleton method"); }
        public ObjectStreamClass getDesc(@ReadOnly CallbackContext this) { throw new RuntimeException("skeleton method"); }
        private void checkAndSetUsed() throws NotActiveException { throw new RuntimeException("skeleton method"); }
        public void setUsed() { throw new RuntimeException("skeleton method"); }
    }
}
