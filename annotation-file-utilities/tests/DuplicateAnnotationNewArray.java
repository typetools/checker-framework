public class DuplicateAnnotationNewArray {}

@interface A {}

@interface B {}

class NewArray2a {
  void foo() {
    @A Object a = new Integer[0];
  }
}

class NewArray2b {
  void foo() {
    @A Object a = new Integer[0];
  }
}

class NewArray2c {
  void foo() {
    @A Object a = new Integer[0];
  }
}
