import java.io.InputStream;

class TmpVarErrorMessage {
  interface Foo {
    InputStream getStream();
  }

  void process(Foo f) {
    m(f.getStream());
  }

  void m(InputStream s) {}
}
