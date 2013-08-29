import checkers.nullness.quals.*;

class Test {
    
    class Test1<CONTENT extends @Nullable Object> {
        CONTENT a;
        // To prevent the warning about un-initialized fields.
        Test1(CONTENT p1) { a = p1; }
  
        public CONTENT get() {
            return a;
        }
        @Pure
        public CONTENT get2() {
            return a;
        }
    }

    class Test2<CONTENT extends @Nullable Object> {
        @NonNull CONTENT a;
        // To prevent the warning about un-initialized fields.
        Test2(@NonNull CONTENT p1) { a = p1; }
  
        public @NonNull CONTENT get() {
            return a;
        }
        @Pure
        public @NonNull CONTENT get2() {
            return a;
        }
    }

    /*
    class Test3<CONTENT extend @Nullable Object> {
        // Change @Pure to be allowed on fields, or add some other anno.
        @Pure CONTENT f;
        // Strangely this assignment succeeded
        Test3(CONTENT p1) { f = p1; }
        // But this assignment failed, because the @Pure caused the
        // other annotations to be erased.
        public void get3(CONTENT p) {
            f = p;
        }
    }
    */

    class Test4<CONTENT extends @Nullable Object> {
        private Pair<CONTENT, CONTENT> userObject;
        Test4(Pair<CONTENT, CONTENT> p) { userObject = p; }
        
        @Pure
        public CONTENT getUserLeft() {
          return userObject.a;
        }
        public class Pair<T1 extends @Nullable Object, T2 extends @Nullable Object> {
            public T1 a;
            public T2 b;

            public Pair(T1 a, T2 b) {
              this.a = a;
              this.b = b;
            }
        }
    }
}