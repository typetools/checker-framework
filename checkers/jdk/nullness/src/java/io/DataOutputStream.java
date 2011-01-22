package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class DataOutputStream extends FilterOutputStream implements DataOutput {
  public DataOutputStream(OutputStream a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void write(int a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public synchronized void write(byte[] a1, int a2, int a3) throws IOException { throw new RuntimeException("skeleton method"); }
  public void flush() throws IOException { throw new RuntimeException("skeleton method"); }
  public final void writeBoolean(boolean a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public final void writeByte(int a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public final void writeShort(int a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public final void writeChar(int a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public final void writeInt(int a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public final void writeLong(long a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public final void writeFloat(float a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public final void writeDouble(double a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public final void writeBytes(String a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public final void writeChars(String a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public final void writeUTF(String a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public final int size() { throw new RuntimeException("skeleton method"); }
}
