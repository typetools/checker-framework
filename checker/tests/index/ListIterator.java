import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.LTEqLengthOf;
import org.checkerframework.checker.index.qual.LTLengthOf;

// @skip-test until we bring list support back

public class ListIterator {

    List<Integer> listField;

    void ListIterator(
            @LTLengthOf("#3") int index, @LTEqLengthOf("#3") int notIndex, List<Integer> list) {
        list.listIterator(index);

        // :: error: (list.access.unsafe.high)
        list.listIterator(notIndex);
    }

    int[] arr = {0};

    void ListIteratorWrongName(@LTLengthOf("arr") int index, List<Integer> list) {
        // :: error: (list.access.unsafe.high)
        list.listIterator(index);
    }

    void ListIteratorField() {
        listField.listIterator(listField.size() - 1);
        listField.listIterator(this.listField.size() - 1);
        this.listField.listIterator(listField.size() - 1);
        this.listField.listIterator(this.listField.size() - 1);

        // :: error: (list.access.unsafe.high)
        listField.listIterator(listField.size());
        // :: error: (list.access.unsafe.high)
        listField.listIterator(this.listField.size());
        // :: error: (list.access.unsafe.high)
        this.listField.listIterator(listField.size());
        // :: error: (list.access.unsafe.high)
        this.listField.listIterator(this.listField.size());
    }

    void ListIteratorFieldUserAnnotation(@IndexFor("listField") int i) {
        listField.listIterator(i);
        this.listField.listIterator(i);

        // :: error: (list.access.unsafe.high)
        listField.listIterator(i + 1);
        // :: error: (list.access.unsafe.high)
        this.listField.listIterator(i + 1);
    }

    void ListIteratorUserAnnotation(@IndexFor("#2") int i, List<Integer> list) {
        list.listIterator(i);

        // :: error: (list.access.unsafe.high)
        list.listIterator(i + 1);
    }
}
