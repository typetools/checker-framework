import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.LTEqLengthOf;
import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.checker.index.qual.LTOMLengthOf;

// @skip-test until we bring list support back

public class ListAdd {

    List<Integer> listField;

    void ListAdd(
            @LTLengthOf("#3") int index, @LTEqLengthOf("#3") int notIndex, List<Integer> list) {
        list.add(index, 4);

        // :: error: (list.access.unsafe.high)
        list.add(notIndex + 1, 4);
    }

    int[] arr = {0};

    void ListAddWrongName(@LTLengthOf("arr") int index, List<Integer> list) {
        // :: error: (list.access.unsafe.high)
        list.add(index, 4);
    }

    void ListAddField() {
        listField.add(listField.size() - 1, 4);
        listField.add(this.listField.size() - 1, 4);
        this.listField.add(listField.size() - 1, 4);
        this.listField.add(this.listField.size() - 1, 4);

        // :: error: (list.access.unsafe.high)
        listField.add(listField.size(), 4);
        // :: error: (list.access.unsafe.high)
        listField.add(this.listField.size(), 4);
        // :: error: (list.access.unsafe.high)
        this.listField.add(listField.size(), 4);
        // :: error: (list.access.unsafe.high)
        this.listField.add(this.listField.size(), 4);
    }

    void ListAddFieldUserAnnotation(@IndexFor("listField") int i) {
        listField.add(i, 4);
        this.listField.add(i, 4);

        // :: error: (list.access.unsafe.high)
        listField.add(i + 4, 4);
        // :: error: (list.access.unsafe.high)
        this.listField.add(i + 4, 4);
    }

    void ListAddUserAnnotation(@IndexFor("#2") int i, List<Integer> list) {
        list.add(i, 4);

        // :: error: (list.access.unsafe.high)
        list.add(i + 4, 4);
    }

    void ListAddUpdateValue(List<Integer> list) {
        @LTEqLengthOf("list") int i = list.size();
        @LTLengthOf("list") int r = list.size() - 1;
        list.add(0);
        @LTLengthOf("list") int k = i;
        @LTOMLengthOf("list") int p = r;
    }

    void ListAddTwo(@LTEqLengthOf({"#2", "#3"}) int i, List<Integer> list, List<Integer> list2) {
        @LTEqLengthOf({"list", "list2"}) int j = i;
        list.add(0);
        // :: error: (list.access.unsafe.high)
        list.get(i);
        // :: error: (list.access.unsafe.high)
        list2.get(i);
    }
}
