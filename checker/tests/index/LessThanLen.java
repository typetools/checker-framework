import org.checkerframework.checker.index.qual.*;

//@skip-test until issue https://github.com/kelloggm/checker-framework/issues/88
// is resolved.

public class LessThanLen {

    public static void m1() {
        int[] shorter = new int[5];
        int[] longer = new int[shorter.length * 2];
        for (int i = 0; i < shorter.length; i++) {
            longer[i] = shorter[i];
        }
    }

    public static void m2(int[] shorter) {
        int[] longer = new int[shorter.length * 2];
        for (int i = 0; i < shorter.length; i++) {
            longer[i] = shorter[i];
        }
    }

    public static void m3(int[] shorter) {
        int[] longer = new int[shorter.length + 1];
        for (int i = 0; i < shorter.length; i++) {
            longer[i] = shorter[i];
        }
    }
}
