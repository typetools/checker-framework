package java.io;

import org.checkerframework.checker.nullness.qual.Nullable;


public class ObjectOutputStream extends OutputStream implements ObjectOutput, ObjectStreamConstants{
  public static abstract class PutField{
    public PutField() { throw new RuntimeException("skeleton method"); }
    public abstract void put(String a1, boolean a2);
    public abstract void put(String a1, byte a2);
    public abstract void put(String a1, char a2);
    public abstract void put(String a1, short a2);
    public abstract void put(String a1, int a2);
    public abstract void put(String a1, long a2);
    public abstract void put(String a1, float a2);
    public abstract void put(String a1, double a2);
    public abstract void put(String a1, @Nullable Object a2);
    public abstract void write(ObjectOutput a1) throws IOException;
  }
  public ObjectOutputStream(OutputStream a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public void useProtocolVersion(int a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public final void writeObject(@Nullable Object a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public void writeUnshared(@Nullable Object a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public void defaultWriteObject() throws IOException { throw new RuntimeException("skeleton method"); }
  public ObjectOutputStream.PutField putFields() throws IOException { throw new RuntimeException("skeleton method"); }
  public void writeFields() throws IOException { throw new RuntimeException("skeleton method"); }
  public void reset() throws IOException { throw new RuntimeException("skeleton method"); }
  public void write(int a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public void write(byte[] a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public void write(byte[] a1, int a2, int a3) throws IOException { throw new RuntimeException("skeleton method"); }
  public void flush() throws IOException { throw new RuntimeException("skeleton method"); }
  public void close() throws IOException { throw new RuntimeException("skeleton method"); }
  public void writeBoolean(boolean a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public void writeByte(int a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public void writeShort(int a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public void writeChar(int a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public void writeInt(int a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public void writeLong(long a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public void writeFloat(float a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public void writeDouble(double a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public void writeBytes(String a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public void writeChars(String a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public void writeUTF(String a1) throws IOException { throw new RuntimeException("skeleton method"); }
}
