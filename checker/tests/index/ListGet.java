import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.LTEqLengthOf;
import org.checkerframework.checker.index.qual.LTLengthOf;

// @skip-test until we bring list support back

class ListGet {

    List<Integer> listField;
    int[] arr = {0};

    void ListGet(
            @LTLengthOf("#3") int index, @LTEqLengthOf("#3") int notIndex, List<Integer> list) {
        list.get(index);

        // :: error: (list.access.unsafe.high)
        list.get(notIndex);
    }

    void ListGetWrongName(@LTLengthOf("arr") int index, List<Integer> list) {
        // :: error: (list.access.unsafe.high)
        list.get(index);
    }

    void ListGetField() {
        listField.get(listField.size() - 1);
        listField.get(this.listField.size() - 1);
        this.listField.get(listField.size() - 1);
        this.listField.get(this.listField.size() - 1);

        // :: error: (list.access.unsafe.high)
        listField.get(listField.size());
        // :: error: (list.access.unsafe.high)
        listField.get(this.listField.size());
        // :: error: (list.access.unsafe.high)
        this.listField.get(listField.size());
        // :: error: (list.access.unsafe.high)
        this.listField.get(this.listField.size());
    }

    void ListGetFieldUserAnnotation(@IndexFor("listField") int i) {
        listField.get(i);
        this.listField.get(i);

        // :: error: (list.access.unsafe.high)
        listField.get(i + 1);
        // :: error: (list.access.unsafe.high)
        this.listField.get(i + 1);
    }

    void ListGetUserAnnotation(@IndexFor("#2") int i, List<Integer> list) {
        list.get(i);

        // :: error: (list.access.unsafe.high)
        list.get(i + 1);
    }
}
