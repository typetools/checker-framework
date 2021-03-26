import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.LTEqLengthOf;
import org.checkerframework.checker.index.qual.LTLengthOf;

// @skip-test until we bring list support back

public class ListSet {

  List<Integer> listField;

  void ListSet(@LTLengthOf("#3") int index, @LTEqLengthOf("#3") int notIndex, List<Integer> list) {
    list.set(index, 4);

    // :: error: (list.access.unsafe.high)
    list.set(notIndex, 4);
  }

  int[] arr = {0};

  void ListSetWrongName(@LTLengthOf("arr") int index, List<Integer> list) {
    // :: error: (list.access.unsafe.high)
    list.set(index, 4);
  }

  void ListSetField() {
    listField.set(listField.size() - 1, 4);
    listField.set(this.listField.size() - 1, 4);
    this.listField.set(listField.size() - 1, 4);
    this.listField.set(this.listField.size() - 1, 4);

    // :: error: (list.access.unsafe.high)
    listField.set(listField.size(), 4);
    // :: error: (list.access.unsafe.high)
    listField.set(this.listField.size(), 4);
    // :: error: (list.access.unsafe.high)
    this.listField.set(listField.size(), 4);
    // :: error: (list.access.unsafe.high)
    this.listField.set(this.listField.size(), 4);
  }

  void ListSetFieldUserAnnotation(@IndexFor("listField") int i) {
    listField.set(i, 4);
    this.listField.set(i, 4);

    // :: error: (list.access.unsafe.high)
    listField.set(i + 1, 4);
    // :: error: (list.access.unsafe.high)
    this.listField.set(i + 1, 4);
  }

  void ListSetUserAnnotation(@IndexFor("#2") int i, List<Integer> list) {
    list.set(i, 4);

    // :: error: (list.access.unsafe.high)
    list.set(i + 1, 4);
  }
}
