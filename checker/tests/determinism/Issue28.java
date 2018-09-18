import org.checkerframework.checker.determinism.qual.*;

// @skip-test
public class Issue28 {
    @PolyDet
    int @PolyDet [] makeArr(@PolyDet int n) {
        return new @PolyDet int @PolyDet [] {n};
    }

    void f(@Det int n) {
        for (int i : makeArr(n)) {
            for (int j : makeArr(i)) {
                @Det int m = j;
            }
        }
    }
}
