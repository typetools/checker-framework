import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.LTEqLengthOf;
import org.checkerframework.checker.index.qual.LTLengthOf;

// @skip-test until we bring list support back
// @skip-test can't handle until TreeUtils.getMethod has a way to precisely handle method
// overloading

public class ListRemove {

    List<Integer> listField;

    void ListRemove(
            @LTLengthOf("#3") int index, @LTEqLengthOf("#3") int notIndex, List<Integer> list) {
        list.remove(index);

        // :: error: (list.access.unsafe.high)
        list.remove(notIndex);
    }

    void ListRemoveWrongName(@LTLengthOf("arr") int index, List<Integer> list) {
        // :: error: (list.access.unsafe.high)
        list.remove(index);
    }

    void ListRemoveField() {
        listField.remove(listField.size() - 1);
        listField.remove(this.listField.size() - 1);
        this.listField.remove(listField.size() - 1);
        this.listField.remove(this.listField.size() - 1);

        // :: error: (list.access.unsafe.high)
        listField.remove(listField.size());
        // :: error: (list.access.unsafe.high)
        listField.remove(this.listField.size());
        // :: error: (list.access.unsafe.high)
        this.listField.remove(listField.size());
        // :: error: (list.access.unsafe.high)
        this.listField.remove(this.listField.size());
    }

    void ListRemoveFieldUserAnnotation(@IndexFor("listField") int i) {
        listField.remove(i);
        this.listField.remove(i);

        // :: error: (list.access.unsafe.high)
        listField.remove(i + 1);
        // :: error: (list.access.unsafe.high)
        this.listField.remove(i + 1);
    }

    void ListRemoveUserAnnotation(@IndexFor("list") int i, List<Integer> list) {
        list.remove(i);

        // :: error: (list.access.unsafe.high)
        list.remove(i + 1);
        // :: error: (list.access.unsafe.high)
        list.remove(i);
    }

    void FailRemove(List<Integer> list) {
        @LTLengthOf("list") int i = list.size() - 1;
        try {
            list.remove(1);
        } catch (Exception e) {
        }

        @LTLengthOf("list") int m = i;
    }

    void RemoveUpdate(List<Integer> list) {
        @LTLength("list")
        int m = list.size() - 1;
        list.get(m);
        list.remove(m);
        // :: error: (list.access.unsafe.high)
        list.get(m);
    }
}
