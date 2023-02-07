// Reproduces a crash that occurred when running WPI on Apache Hadoop.

import java.io.*;

class JavaSerialization {
  private interface Serializer<T> {}

  static class JavaSerializationSerializer implements Serializer<Serializable> {

    private ObjectOutputStream oos;

    // Note that it is important to reproduce the crash that the name of this parameter not
    // be changed: if it is e.g., "iShouldBeTreatedAsSibling1", no crash occurs.
    public JavaSerializationSerializer(OutputStream out) throws IOException {
      oos =
          new ObjectOutputStream(out) {
            @Override
            protected void writeStreamHeader() {
              // no header
            }
          };
    }

    public void close() throws IOException {
      oos.close();
    }
  }
}
