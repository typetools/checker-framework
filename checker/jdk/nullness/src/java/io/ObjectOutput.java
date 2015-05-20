package java.io;

public interface ObjectOutput extends DataOutput {
  void writeObject(Object a1) throws IOException;
  void write(int a1) throws IOException;
  void write(byte[] a1) throws IOException;
  void write(byte[] a1, int a2, int a3) throws IOException;
  void flush() throws IOException;
  void close() throws IOException;
}
