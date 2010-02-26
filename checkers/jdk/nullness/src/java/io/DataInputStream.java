package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class DataInputStream extends FilterInputStream implements DataInput {
  public DataInputStream(java.io.InputStream a1) { throw new RuntimeException("skeleton method"); }
  public final int read(byte[] a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public final int read(byte[] a1, int a2, int a3) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public final void readFully(byte[] a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public final void readFully(byte[] a1, int a2, int a3) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
  public final int skipBytes(int a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
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
  public final static java.lang.String readUTF(java.io.DataInput a1) throws java.io.IOException { throw new RuntimeException("skeleton method"); }
}
