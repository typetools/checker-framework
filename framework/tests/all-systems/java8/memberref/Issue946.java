// Test case for Issue 946
// https://github.com/typetools/checker-framework/issues/946

interface Supply946<R> {
  R supply();
}

public class Issue946 {
  class MethodRefInnerA {
    Supply946<MethodRefInnerB> constructorReferenceField = MethodRefInnerB::new;

    MethodRefInnerA(Issue946 Issue946.this) {
      Supply946<MethodRefInnerB> constructorReference = MethodRefInnerB::new;
    }

    void method() {
      Supply946<MethodRefInnerB> constructorReference = MethodRefInnerB::new;
    }

    class MethodRefInnerAInner {
      void method() {
        Supply946<MethodRefInnerB> constructorReference = MethodRefInnerB::new;
      }
    }
  }

  class MethodRefInnerB {
    MethodRefInnerB(Issue946 Issue946.this) {}

    void method() {
      Supply946<MethodRefInnerB> constructorReference = MethodRefInnerB::new;
    }
  }

  void method() {
    Supply946<MethodRefInnerB> constructorReference = MethodRefInnerB::new;
  }
}
