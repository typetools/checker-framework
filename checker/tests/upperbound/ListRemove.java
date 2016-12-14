import java.util.*;
import org.checkerframework.checker.index.qual.*;
import org.checkerframework.checker.upperbound.qual.*;

//@skip-test can't handle until TreeUtils.getMethod has a way to precisly handle method overloading

class ListRemove {

    List<Integer> listField;

    void ListRemove(
            @LTLengthOf("list") int index, @LTEqLengthOf("list") int notIndex, List<Integer> list) {
        list.remove(index);

        //:: warning: (list.access.unsafe.high)
        list.remove(notIndex);
    }

    void ListRemoveWrongName(@LTLengthOf("arr") int index, List<Integer> list) {
        //:: warning: (list.access.unsafe.high)
        list.remove(index);
    }

    void ListRemoveField() {
        listField.remove(listField.size() - 1);
        listField.remove(this.listField.size() - 1);
        this.listField.remove(listField.size() - 1);
        this.listField.remove(this.listField.size() - 1);

        //:: warning: (list.access.unsafe.high)
        listField.remove(listField.size());
        //:: warning: (list.access.unsafe.high)
        listField.remove(this.listField.size());
        //:: warning: (list.access.unsafe.high)
        this.listField.remove(listField.size());
        //:: warning: (list.access.unsafe.high)
        this.listField.remove(this.listField.size());
    }

    void ListRemoveFieldUserAnnotation(@IndexFor("listField") int i) {
        listField.remove(i);
        this.listField.remove(i);

        //:: warning: (list.access.unsafe.high)
        listField.remove(i + 1);
        //:: warning: (list.access.unsafe.high)
        this.listField.remove(i + 1);
    }

    void ListRemoveUserAnnotation(@IndexFor("list") int i, List<Integer> list) {
        list.remove(i);

        //:: warning: (list.access.unsafe.high)
        list.remove(i + 1);
        //:: warning: (list.access.unsafe.high)
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
        //:: warning: (list.access.unsafe.high)
        list.get(m);
    }
}
