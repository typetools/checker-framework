import tests.util.*;

class AnnotatedGenerics {

    public static void testNullableTypeVariable() {
        class Test<T> {
            @Odd T get() { return null; }
        }
        Test<String> l = null;
        String l1 = l.get();
        @Odd String l2 = l.get();

        Test<@Odd String> n = null;
        String n1 = n.get();
        @Odd String n2 = n.get();
    }

    class MyClass<T> implements java.util.Iterator<@Odd T> {
        public boolean hasNext() { return true; }
        public @Odd T next() { return null; }
        public void remove() { }
      }
}