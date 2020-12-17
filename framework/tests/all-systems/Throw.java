import java.util.List;

public class Throw {
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
