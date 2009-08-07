package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public abstract interface DataInput{
  public abstract void readFully(byte[] a1) throws java.io.IOException;
  public abstract void readFully(byte[] a1, int a2, int a3) throws java.io.IOException;
  public abstract int skipBytes(int a1) throws java.io.IOException;
  public abstract boolean readBoolean() throws java.io.IOException;
  public abstract byte readByte() throws java.io.IOException;
  public abstract int readUnsignedByte() throws java.io.IOException;
  public abstract short readShort() throws java.io.IOException;
  public abstract int readUnsignedShort() throws java.io.IOException;
  public abstract char readChar() throws java.io.IOException;
  public abstract int readInt() throws java.io.IOException;
  public abstract long readLong() throws java.io.IOException;
  public abstract float readFloat() throws java.io.IOException;
  public abstract double readDouble() throws java.io.IOException;
  public abstract @Nullable java.lang.String readLine() throws java.io.IOException;
  public abstract java.lang.String readUTF() throws java.io.IOException;
}
