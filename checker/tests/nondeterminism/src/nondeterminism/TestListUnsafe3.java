package nondeterminism;

import java.util.ArrayList;
import org.checkerframework.checker.nondeterminism.qual.*;

public class TestListUnsafe3 {
    void TestList() {
        @ValueNonDet ArrayList<@Det Integer> lst = new @ValueNonDet ArrayList<@Det Integer>();
        @Det ArrayList<@Det Integer> cpy = lst;
        lst.add(20);
    }
}
