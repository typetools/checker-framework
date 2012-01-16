
public class MoreVarargs {

    <T> T[] genericVararg(T... args) { return args; }

    void testGenericVararg() {
        genericVararg("m");
        genericVararg(new String[] { });
        genericVararg(3);
        genericVararg(new Integer[] { });
        genericVararg(new int[] { });
    }

}