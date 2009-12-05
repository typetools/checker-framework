package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public abstract interface DataOutput {
  public abstract void write(int a1) throws java.io.IOException;
  public abstract void write(byte[] a1) throws java.io.IOException;
  public abstract void write(byte[] a1, int a2, int a3) throws java.io.IOException;
  public abstract void writeBoolean(boolean a1) throws java.io.IOException;
  public abstract void writeByte(int a1) throws java.io.IOException;
  public abstract void writeShort(int a1) throws java.io.IOException;
  public abstract void writeChar(int a1) throws java.io.IOException;
  public abstract void writeInt(int a1) throws java.io.IOException;
  public abstract void writeLong(long a1) throws java.io.IOException;
  public abstract void writeFloat(float a1) throws java.io.IOException;
  public abstract void writeDouble(double a1) throws java.io.IOException;
  public abstract void writeBytes(java.lang.String a1) throws java.io.IOException;
  public abstract void writeChars(java.lang.String a1) throws java.io.IOException;
  public abstract void writeUTF(java.lang.String a1) throws java.io.IOException;
}
