
import java.util.List;
import java.util.ArrayList;

class TestTeranry {
    void foo(boolean b, List<Object> res) {
        Object o = b ? "x" : (b ? "y" : "z");
        res.add(o);
    }
}
