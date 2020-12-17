@SuppressWarnings("all")
public class SuperThis {
    class Super {}

    // Test super() and this()
    class Inner extends Super {
        public Inner() {
            super();
        }

        public Inner(int i) {
            this();
        }
    }
}
