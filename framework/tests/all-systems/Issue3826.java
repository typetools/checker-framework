public class Issue3826 {
    public static <B, T extends B> void getOption(Class<T> cls, B[] opts) {}

    public static class ClassA {
        public static class InnerClassA {
            interface InnerInnerClassA {}
        }
    }

    public static class ClassB {
        public abstract static class InnerClassB {}
    }

    public static class ClassC {
        interface InterfaceClassC extends ClassA.InnerClassA.InnerInnerClassA {}

        private static class InnerClassC extends ClassA.InnerClassA implements InterfaceClassC {}

        public ClassC(ClassA.InnerClassA.InnerInnerClassA... opts) {
            // Does not crash
            Issue3826.<ClassA.InnerClassA.InnerInnerClassA, InnerClassC>getOption(
                    InnerClassC.class, opts);
            // Crashes
            Issue3826.getOption(InnerClassC.class, opts);
        }
    }
}
