// Test that passing a resource to an @Owning parameter of the super constructor of an anonymous
// class transfers ownership.

import java.io.FileInputStream;
import java.io.IOException;
import org.checkerframework.checker.mustcall.qual.Owning;

class AnonymousClassOwning {

  static class Foo {
    Foo(@Owning FileInputStream s) {
      try {
        s.close();
      } catch (IOException e) {
      }
    }
  }

  class Inner {
    Inner(@Owning FileInputStream s) {
      try {
        s.close();
      } catch (IOException e) {
      }
    }
  }

  static class NotOwning {
    NotOwning(FileInputStream s) {}
  }

  void named() throws IOException {
    FileInputStream s = new FileInputStream("file.txt");
    new Foo(s);
  }

  void anonymous() throws IOException {
    FileInputStream s = new FileInputStream("file.txt");
    new Foo(s) {};
  }

  void anonymousWithEnclosingExpression(AnonymousClassOwning outer) throws IOException {
    FileInputStream s = new FileInputStream("file.txt");
    outer.new Inner(s) {};
  }

  void anonymousNotOwning() throws IOException {
    // :: error: (required.method.not.called)
    FileInputStream s = new FileInputStream("file.txt");
    new NotOwning(s) {};
  }
}
