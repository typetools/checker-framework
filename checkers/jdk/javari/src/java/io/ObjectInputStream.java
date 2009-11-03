package java.io;
import checkers.javari.quals.*;

public class ObjectInputStream extends InputStream implements ObjectInput, ObjectStreamConstants {
    public abstract static class GetField {
        public GetField() {};
        public abstract boolean defaulted(String name) throws IOException;
        public abstract boolean get(String name, boolean val) throws IOException;
        public abstract byte get(String name, byte val) throws IOException;
        public abstract char get(String name, char val) throws IOException;
        public abstract double get(String name, double val) throws IOException;
        public abstract float get(String name, float val) throws IOException;
        public abstract int get(String name, int val) throws IOException;
        public abstract long get(String name, long val) throws IOException;
        public abstract @PolyRead Object get(String name, @PolyRead Object val) throws IOException;
        public abstract short get(String name, short val) throws IOException;
        public abstract ObjectStreamClass getObjectStreamClass();
    }

    protected ObjectInputStream() throws IOException, SecurityException { throw new RuntimeException("skeleton method"); }
    public ObjectInputStream(InputStream in) throws IOException { throw new RuntimeException("skeleton method"); }
    public int available() @ReadOnly throws IOException { throw new RuntimeException("skeleton method"); }
    public void close() throws IOException{ throw new RuntimeException("skeleton method"); }
    public void defaultReadObject() throws IOException, ClassNotFoundException { throw new RuntimeException("skeleton method"); }
    protected boolean enableResolveObject(boolean enable) throws SecurityException { throw new RuntimeException("skeleton method"); }
    public int read() throws IOException { throw new RuntimeException("skeleton method"); }
    public int read(byte @ReadOnly [] buf, int off, int len) throws IOException { throw new RuntimeException("skeleton method"); }
    public boolean readBoolean() throws IOException { throw new RuntimeException("skeleton method"); }
    public byte readByte() throws IOException { throw new RuntimeException("skeleton method"); }
    public char readChar() throws IOException { throw new RuntimeException("skeleton method"); }
    protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException { throw new RuntimeException("skeleton method"); }
    public double readDouble() throws IOException { throw new RuntimeException("skeleton method"); }
    public @PolyRead ObjectInputStream.GetField readFields() throws IOException, ClassNotFoundException { throw new RuntimeException("skeleton method"); }
    public float readFloat() throws IOException { throw new RuntimeException("skeleton method"); }
    public void readFully(byte @ReadOnly [] buf) throws IOException { throw new RuntimeException("skeleton method"); }
    public void readFully(byte @ReadOnly [] buf, int off, int len) throws IOException { throw new RuntimeException("skeleton method"); }
    public int readInt() throws IOException { throw new RuntimeException("skeleton method"); }
    @Deprecated public String readLine() throws IOException { throw new RuntimeException("skeleton method"); }
    public long readLong() throws IOException { throw new RuntimeException("skeleton method"); }
    public final Object readObject() throws IOException, ClassNotFoundException { throw new RuntimeException("skeleton method"); }
    protected Object readObjectOverride() throws IOException, ClassNotFoundException { throw new RuntimeException("skeleton method"); }
    public short readShort() throws IOException { throw new RuntimeException("skeleton method"); }
    protected void readStreamHeader() throws IOException, StreamCorruptedException { throw new RuntimeException("skeleton method"); }
    public Object readUnshared() throws IOException, ClassNotFoundException { throw new RuntimeException("skeleton method"); }
    public int readUnsignedByte() throws IOException { throw new RuntimeException("skeleton method"); }
    public int readUnsignedShort() throws IOException { throw new RuntimeException("skeleton method"); }
    public String readUTF() throws IOException { throw new RuntimeException("skeleton method"); }
    public void registerValidation(ObjectInputValidation obj, int prio) throws NotActiveException, InvalidObjectException { throw new RuntimeException("skeleton method"); }
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException { throw new RuntimeException("skeleton method"); }
    protected Object resolveObject(Object obj) throws IOException { throw new RuntimeException("skeleton method"); }
    protected Class<?> resolveProxyClass(String @ReadOnly [] interfaces) throws IOException, ClassNotFoundException { throw new RuntimeException("skeleton method"); }
    public int skipBytes(int len) throws IOException{ throw new RuntimeException("skeleton method"); }
}
