public class AnonInner {
  class InnerOne {
    Object m1(String p) {
      return new Object() {
        public Object e1(Object o) {
          return new Object();
        }
      };
    }

    Object m2(String p) {
      return new Object() {
        public Object e2(Object o) {
          return new Object();
        }
      };
    }
  }
}
