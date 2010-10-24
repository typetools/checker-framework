package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public abstract interface DataInput{
  public abstract void readFully(byte[] a1) throws IOException;
  public abstract void readFully(byte[] a1, int a2, int a3) throws IOException;
  public abstract int skipBytes(int a1) throws IOException;
  public abstract boolean readBoolean() throws IOException;
  public abstract byte readByte() throws IOException;
  public abstract int readUnsignedByte() throws IOException;
  public abstract short readShort() throws IOException;
  public abstract int readUnsignedShort() throws IOException;
  public abstract char readChar() throws IOException;
  public abstract int readInt() throws IOException;
  public abstract long readLong() throws IOException;
  public abstract float readFloat() throws IOException;
  public abstract double readDouble() throws IOException;
  public abstract @Nullable String readLine() throws IOException;
  public abstract String readUTF() throws IOException;
}
