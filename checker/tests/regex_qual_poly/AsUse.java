
class AsUseTest {

    static class TypeVarUse<T> {
        T next1;
        void test() {
            TypeVarUse<T> inst = null;
            String s = "test " + next1;
            String s2 = "test " + this.next1;
            String s3 = "test " + testString();
        }
        T testString() {
            return null;
        }
    }
}
