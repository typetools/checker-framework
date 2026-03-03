public class LocalClass {

  Object f;

  void m() {
    class InnerLocalClass {
      Object f;
    }
    new Object() {
      Object f;

      class Test {
        Object f;

        void m() {
          new Object() {
            Object f;
          };
          new Object() {
            Object f;
          };
        }
      }
    };
    new Object() {
      Object f;
    };
  }

  void m2() {
    class InnerLocalClass {
      Object f;

      class Inner {
        Object f;

        void m() {
          new Object() {
            Object f;
          };
          new Object() {
            Object f;
          };
        }
      }

      void m() {
        class OuterLocalClass {
          Object f;
        }
      }
    }
  }

  void m3() {
    class OuterLocalClass {
      Object f;

      void m() {
        class InnerLocalClass {
          Object f;
        }
      }
    }
  }
}
