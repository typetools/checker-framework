package determinism;

import java.util.List;
import org.checkerframework.checker.determinism.qual.*;

class CheckThrow {
    <E extends Exception> void throwTypeVar(E ex) {
        try {
            throw ex;
        } catch (Exception e) {
        }
    }

    void throwWildcard(List<? extends Exception> list) {
        try {
            throw list.get(0);
        } catch (Exception e) {

        }
    }
}
