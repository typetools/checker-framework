package java.io;

import org.checkerframework.checker.nullness.qual.Nullable;


public class ObjectInputStream extends InputStream implements ObjectInput, ObjectStreamConstants {
  public static abstract class GetField{
    public GetField() { throw new RuntimeException("skeleton method"); }
    public abstract ObjectStreamClass getObjectStreamClass();
    public abstract boolean defaulted(String a1) throws IOException;
    public abstract boolean get(String a1, boolean a2) throws IOException;
    public abstract byte get(String a1, byte a2) throws IOException;
    public abstract char get(String a1, char a2) throws IOException;
    public abstract short get(String a1, short a2) throws IOException;
    public abstract int get(String a1, int a2) throws IOException;
    public abstract long get(String a1, long a2) throws IOException;
    public abstract float get(String a1, float a2) throws IOException;
    public abstract double get(String a1, double a2) throws IOException;
    public abstract @Nullable Object get(String a1, @Nullable Object a2) throws IOException;
  }
  public ObjectInputStream(InputStream a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public final Object readObject() throws IOException, ClassNotFoundException { throw new RuntimeException("skeleton method"); }
  public Object readUnshared() throws IOException, ClassNotFoundException { throw new RuntimeException("skeleton method"); }
  public void defaultReadObject() throws IOException, ClassNotFoundException { throw new RuntimeException("skeleton method"); }
  public GetField readFields() throws IOException, ClassNotFoundException { throw new RuntimeException("skeleton method"); }
  public void registerValidation(ObjectInputValidation a1, int a2) throws NotActiveException, InvalidObjectException { throw new RuntimeException("skeleton method"); }
  public int read() throws IOException { throw new RuntimeException("skeleton method"); }
  public int read(byte[] a1, int a2, int a3) throws IOException { throw new RuntimeException("skeleton method"); }
  public int available() throws IOException { throw new RuntimeException("skeleton method"); }
  public void close() throws IOException { throw new RuntimeException("skeleton method"); }
  public boolean readBoolean() throws IOException { throw new RuntimeException("skeleton method"); }
  public byte readByte() throws IOException { throw new RuntimeException("skeleton method"); }
  public int readUnsignedByte() throws IOException { throw new RuntimeException("skeleton method"); }
  public char readChar() throws IOException { throw new RuntimeException("skeleton method"); }
  public short readShort() throws IOException { throw new RuntimeException("skeleton method"); }
  public int readUnsignedShort() throws IOException { throw new RuntimeException("skeleton method"); }
  public int readInt() throws IOException { throw new RuntimeException("skeleton method"); }
  public long readLong() throws IOException { throw new RuntimeException("skeleton method"); }
  public float readFloat() throws IOException { throw new RuntimeException("skeleton method"); }
  public double readDouble() throws IOException { throw new RuntimeException("skeleton method"); }
  public void readFully(byte[] a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public void readFully(byte[] a1, int a2, int a3) throws IOException { throw new RuntimeException("skeleton method"); }
  public int skipBytes(int a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public @Nullable String readLine() throws IOException { throw new RuntimeException("skeleton method"); }
  public String readUTF() throws IOException { throw new RuntimeException("skeleton method"); }
}
