package java.io;

public interface ObjectInput extends DataInput {
  Object readObject() throws ClassNotFoundException, IOException;
  int read() throws IOException;
  int read(byte[] a1) throws IOException;
  int read(byte[] a1, int a2, int a3) throws IOException;
  long skip(long a1) throws IOException;
  int available() throws IOException;
  void close() throws IOException;
}
