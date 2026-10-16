package org.checkerframework.afu.annotator.tests;

public class ConstructorReceivers {
  class C0 {
    public C0() {}

    public C0(int i) {}
  }

  class C1 {
    public C1(ConstructorReceivers ConstructorReceivers.this) {}

    public C1(ConstructorReceivers ConstructorReceivers.this, int i) {}
  }

  class C2 {
    public C2() {}
  }

  class C3 {
    public C3(ConstructorReceivers ConstructorReceivers.this) {}
  }

  class P0<K, V> {
    class C4 {
      public C4() {}

      public C4(int i) {}
    }

    class C5 {
      public C5(ConstructorReceivers.P0<K, V> ConstructorReceivers.P0.this) {}

      public C5(ConstructorReceivers.P0<K, V> ConstructorReceivers.P0.this, int i) {}
    }

    class C6 {
      public C6() {}

      public C6(ConstructorReceivers.P0<K, V> other) {}
    }

    class C7 {
      public C7(ConstructorReceivers.P0<K, V> ConstructorReceivers.P0.this) {}

      public C7(
          ConstructorReceivers.P0<K, V> ConstructorReceivers.P0.this,
          ConstructorReceivers.P0<K, V> other) {}
    }
  }

  class P1<K extends Object, V> {
    class C8 {
      public C8() {}
    }

    class C9 {
      public C9(ConstructorReceivers.P1<K, V> ConstructorReceivers.P1.this) {}
    }
  }
}
