package java.io;

import org.checkerframework.checker.nullness.qual.Nullable;

@org.checkerframework.framework.qual.DefaultQualifier(org.checkerframework.checker.nullness.qual.NonNull.class)

public interface DataInput{
  void readFully(byte[] a1) throws IOException;
  void readFully(byte[] a1, int a2, int a3) throws IOException;
  int skipBytes(int a1) throws IOException;
  boolean readBoolean() throws IOException;
  byte readByte() throws IOException;
  int readUnsignedByte() throws IOException;
  short readShort() throws IOException;
  int readUnsignedShort() throws IOException;
  char readChar() throws IOException;
  int readInt() throws IOException;
  long readLong() throws IOException;
  float readFloat() throws IOException;
  double readDouble() throws IOException;
  @Nullable String readLine() throws IOException;
  String readUTF() throws IOException;
}
