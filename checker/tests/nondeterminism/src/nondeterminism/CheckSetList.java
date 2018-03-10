package nondeterminism;

import java.util.ArrayList;
import org.checkerframework.checker.nondeterminism.qual.Det;
import org.checkerframework.checker.nondeterminism.qual.ValueNonDet;

public class CheckSetList {

    void setInList(@Det ArrayList<@Det Integer> lst, @ValueNonDet int i) {
        lst.set(i, 5);
        lst.set(0, i);
        lst.set(i, i);
        lst.set(1, 0);
    }

    void setInList1(
            @ValueNonDet ArrayList<@ValueNonDet Integer> lst, @Det int i, @ValueNonDet int e) {
        lst.set(i, e);
        lst.set(e, i);
        lst.set(e, e);
        lst.set(i, i);
    }

    void setInList2(@ValueNonDet ArrayList<@Det Integer> lst, @Det int i, @ValueNonDet int e) {
        lst.set(i, e);
        lst.set(e, i);
        lst.set(i, i);
        lst.set(e, e);
    }

    void setInList3(@ValueNonDet ArrayList<@Det Integer> lst, @ValueNonDet int i) {
        lst.set(i, 5);
        lst.set(0, i);
        lst.set(i, i);
        lst.set(0, 0);
    }
}
