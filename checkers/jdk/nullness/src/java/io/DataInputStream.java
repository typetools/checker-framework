package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class DataInputStream extends FilterInputStream implements DataInput {
  public DataInputStream(InputStream a1) { super(a1); throw new RuntimeException("skeleton method"); }
  public final int read(byte[] a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public final int read(byte[] a1, int a2, int a3) throws IOException { throw new RuntimeException("skeleton method"); }
  public final void readFully(byte[] a1) throws IOException { throw new RuntimeException("skeleton method"); }
  public final void readFully(byte[] a1, int a2, int a3) throws IOException { throw new RuntimeException("skeleton method"); }
  public final int skipBytes(int a1) throws IOException { throw new RuntimeException("skeleton method"); }
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
  public final static String readUTF(DataInput a1) throws IOException { throw new RuntimeException("skeleton method"); }
}
