import java.io.*;

class JavaSerialization {
  private interface Serializer<T> {}

  static class JavaSerializationSerializer implements Serializer<Serializable> {

    private ObjectOutputStream oos;

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
