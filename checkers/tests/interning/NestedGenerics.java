import checkers.interning.quals.Interned;
import java.util.*;

public class NestedGenerics {

    public void test() {
        List<List<@Interned Object>> foo = bar();
    }

    public List<List<@Interned Object>> bar() {
        return null;
    }
}
