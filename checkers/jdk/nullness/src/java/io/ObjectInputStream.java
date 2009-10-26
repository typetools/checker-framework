package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class ObjectInputStream extends InputStream implements ObjectInput, ObjectStreamConstants {
  public static abstract class GetField{
    public GetField() { throw new RuntimeException("skeleton method"); }
    public abstract java.io.ObjectStreamClass getObjectStreamClass();
    public abstract boolean defaulted(java.lang.String a1) throws java.io.IOException;
    public abstract boolean get(java.lang.String a1, boolean a2) throws java.io.IOException;
    public abstract byte get(java.lang.String a1, byte a2) throws java.io.IOException;
    public abstract char get(java.lang.String a1, char a2) throws java.io.IOException;
    public abstract short get(java.lang.String a1, short a2) throws java.io.IOException;
    public abstract int get(java.lang.String a1, int a2) throws java.io.IOException;
    public abstract long get(java.lang.String a1, long a2) throws java.io.IOException;
    public abstract float get(java.lang.String a1, float a2) throws java.io.IOException;
    public abstract double get(java.lang.String a1, double a2) throws java.io.IOException;
    public abstract java.lang.Object get(java.lang.String a1, java.lang.Object a2) throws java.io.IOException;
  }
  public ObjectInputStream(java.io.InputStream a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public final java.lang.Object readObject() throws java.io.IOException, java.lang.ClassNotFoundException { throw new RuntimeException("skeleton method"); }
  public java.lang.Object readUnshared() throws java.io.IOException, java.lang.ClassNotFoundException { throw new RuntimeException("skeleton method"); }
  public void defaultReadObject() throws java.io.IOException, java.lang.ClassNotFoundException { throw new RuntimeException("skeleton method"); }
  public GetField readFields() throws java.io.IOException, java.lang.ClassNotFoundException { throw new RuntimeException("skeleton method"); }
  public void registerValidation(java.io.ObjectInputValidation a1, int a2) throws java.io.NotActiveException, java.io.InvalidObjectException { throw new RuntimeException("skeleton method"); }
  public int read() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public int read(byte[] a1, int a2, int a3) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public int available() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void close() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public boolean readBoolean() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public byte readByte() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public int readUnsignedByte() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public char readChar() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public short readShort() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public int readUnsignedShort() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public int readInt() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public long readLong() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public float readFloat() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public double readDouble() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void readFully(byte[] a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void readFully(byte[] a1, int a2, int a3) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public int skipBytes(int a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public @Nullable java.lang.String readLine() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public java.lang.String readUTF() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
}
