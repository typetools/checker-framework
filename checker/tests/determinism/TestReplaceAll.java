package determinism;

import java.util.*;
import java.util.function.*;
import org.checkerframework.checker.determinism.qual.*;

public class TestReplaceAll {
    void ListReplace(@Det List<@Det String> lst, @Det UnaryOperator<String> op) {
        lst.replaceAll(op);
    }

    void ListReplace1(@OrderNonDet List<@Det String> lst, @Det UnaryOperator<String> op) {
        lst.replaceAll(op);
    }
}
