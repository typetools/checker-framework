package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class RandomAccessFile implements DataOutput, DataInput, Closeable {
  public RandomAccessFile(String a1, String a2) throws FileNotFoundException { throw new RuntimeException("skeleton method"); }
  public RandomAccessFile(File a1, String a2) throws FileNotFoundException { throw new RuntimeException("skeleton method"); }
  public final FileDescriptor getFD() throws IOException { throw new RuntimeException("skeleton method"); }
  public final java.nio.channels.FileChannel getChannel() { throw new RuntimeException("skeleton method"); }
  public int read(byte[] a1, int a2, int a3) throws IOException { throw new RuntimeException("skeleton method"); }
  public int read(byte[] a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public final void readFully(byte[] a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public final void readFully(byte[] a1, int a2, int a3) throws IOException { throw new RuntimeException("skeleton method"); }
  public int skipBytes(int a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public void write(byte[] a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public void write(byte[] a1, int a2, int a3) throws IOException { throw new RuntimeException("skeleton method"); }
  public void close() throws IOException { throw new RuntimeException("skeleton method"); }
  public final boolean readBoolean() throws IOException { throw new RuntimeException("skeleton method"); }
  public final byte readByte() throws IOException { throw new RuntimeException("skeleton method"); }
  public final int readUnsignedByte() throws IOException { throw new RuntimeException("skeleton method"); }
  public final short readShort() throws IOException { throw new RuntimeException("skeleton method"); }
  public final int readUnsignedShort() throws IOException { throw new RuntimeException("skeleton method"); }
  public final char readChar() throws IOException { throw new RuntimeException("skeleton method"); }
  public final int readInt() throws IOException { throw new RuntimeException("skeleton method"); }
  public final long readLong() throws IOException { throw new RuntimeException("skeleton method"); }
  public final float readFloat() throws IOException { throw new RuntimeException("skeleton method"); }
  public final double readDouble() throws IOException { throw new RuntimeException("skeleton method"); }
  public final @Nullable String readLine() throws IOException { throw new RuntimeException("skeleton method"); }
  public final String readUTF() throws IOException { throw new RuntimeException("skeleton method"); }
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

  public native int read() throws IOException;
  public native long getFilePointer() throws IOException;
  public native long length() throws IOException;
  public native void seek(long a1) throws IOException;
  public native void setLength(long a1) throws IOException;
  public native void write(int a1) throws IOException;

}
