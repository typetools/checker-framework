public class CheckNotNull1 {
    <T extends Object> T checkNotNull(T ref) {
        return ref;
    }

    <S extends Object> void test(S ref) {
        checkNotNull(ref);
    }
}
