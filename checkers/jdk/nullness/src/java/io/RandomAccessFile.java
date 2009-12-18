package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class RandomAccessFile implements DataOutput, DataInput, Closeable {
  public RandomAccessFile(java.lang.String a1, java.lang.String a2) throws java.io.FileNotFoundException { throw new RuntimeException("skeleton method"); }
  public RandomAccessFile(java.io.File a1, java.lang.String a2) throws java.io.FileNotFoundException { throw new RuntimeException("skeleton method"); }
  public final java.io.FileDescriptor getFD() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public final java.nio.channels.FileChannel getChannel() { throw new RuntimeException("skeleton method"); }
  public int read(byte[] a1, int a2, int a3) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public int read(byte[] a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public final void readFully(byte[] a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public final void readFully(byte[] a1, int a2, int a3) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public int skipBytes(int a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void write(byte[] a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void write(byte[] a1, int a2, int a3) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public void close() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public final boolean readBoolean() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public final byte readByte() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public final int readUnsignedByte() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public final short readShort() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public final int readUnsignedShort() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public final char readChar() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public final int readInt() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public final long readLong() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public final float readFloat() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public final double readDouble() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public final @Nullable java.lang.String readLine() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public final java.lang.String readUTF() throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public final void writeBoolean(boolean a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public final void writeByte(int a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public final void writeShort(int a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public final void writeChar(int a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public final void writeInt(int a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public final void writeLong(long a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public final void writeFloat(float a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public final void writeDouble(double a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public final void writeBytes(java.lang.String a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public final void writeChars(java.lang.String a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public final void writeUTF(java.lang.String a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }

  public native int read() throws java.io.IOException;
  public native long getFilePointer() throws java.io.IOException;
  public native long length() throws java.io.IOException;
  public native void seek(long a1) throws java.io.IOException;
  public native void setLength(long a1) throws java.io.IOException;
  public native void write(int a1) throws java.io.IOException;

}
