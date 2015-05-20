package java.io;

public interface DataOutput {
  void write(int a1) throws IOException;
  void write(byte[] a1) throws IOException;
  void write(byte[] a1, int a2, int a3) throws IOException;
  void writeBoolean(boolean a1) throws IOException;
  void writeByte(int a1) throws IOException;
  void writeShort(int a1) throws IOException;
  void writeChar(int a1) throws IOException;
  void writeInt(int a1) throws IOException;
  void writeLong(long a1) throws IOException;
  void writeFloat(float a1) throws IOException;
  void writeDouble(double a1) throws IOException;
  void writeBytes(String a1) throws IOException;
  void writeChars(String a1) throws IOException;
  void writeUTF(String a1) throws IOException;
}
