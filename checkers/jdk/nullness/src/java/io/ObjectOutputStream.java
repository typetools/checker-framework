package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class ObjectOutputStream extends OutputStream implements ObjectOutput, ObjectStreamConstants{
  public static abstract class PutField{
    public PutField() { throw new RuntimeException("skeleton method"); }
    public abstract void put(java.lang.String a1, boolean a2);
    public abstract void put(java.lang.String a1, byte a2);
    public abstract void put(java.lang.String a1, char a2);
    public abstract void put(java.lang.String a1, short a2);
    public abstract void put(java.lang.String a1, int a2);
    public abstract void put(java.lang.String a1, long a2);
    public abstract void put(java.lang.String a1, float a2);
    public abstract void put(java.lang.String a1, double a2);
    public abstract void put(java.lang.String a1, @Nullable java.lang.Object a2);
    public abstract void write(java.io.ObjectOutput a1) throws java.io.IOException;
  }
  public ObjectOutputStream(java.io.OutputStream a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void useProtocolVersion(int a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public final void writeObject(@Nullable java.lang.Object a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void writeUnshared(java.lang.Object a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void defaultWriteObject() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public java.io.ObjectOutputStream.PutField putFields() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void writeFields() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void reset() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void write(int a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void write(byte[] a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void write(byte[] a1, int a2, int a3) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void flush() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void close() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void writeBoolean(boolean a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void writeByte(int a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void writeShort(int a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void writeChar(int a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void writeInt(int a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void writeLong(long a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void writeFloat(float a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void writeDouble(double a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void writeBytes(java.lang.String a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void writeChars(java.lang.String a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void writeUTF(java.lang.String a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
}
