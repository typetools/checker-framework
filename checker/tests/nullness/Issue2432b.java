// Additional test cases for issue 2432 (unrelated to poly annotations)
// https://github.com/typetools/checker-framework/issues/2432

import java.util.ArrayList;
import java.util.List;

class Issue2432b {
    void objectAsTypeArg() {
        List<Object> objs = new ArrayList<>();
        // no error
        Object[] objarray = objs.toArray();
    }

    // seems only object list would cause this problem:
    void stringAsTypeArg() {
        List<String> strs = new ArrayList<>();
        Object[] strarray = strs.toArray();
    }

    void listAsTypeArg() {
        List<List> lists = new ArrayList<>();
        Object[] listarray = lists.toArray();
    }
}
