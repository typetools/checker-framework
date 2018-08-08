import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

public class TestListtoArray {
    void ListToObjectArray(@Det List<@Det String> detList) {
        Object[] objArr = detList.toArray();
    }

    void ListToObjectArray1(@OrderNonDet List<@Det String> ondetList) {
        @NonDet Object @NonDet [] objArr = ondetList.toArray();
    }

    void ListToObjectArray2(@NonDet List<@Det String> nondetList) {
        @NonDet Object @NonDet [] objArr = nondetList.toArray();
    }

    void ListToObjectArray3(@Det List<@Det String> detList) {
        String [] objArr = detList.toArray(new @Det String @Det[10]);
    }

    void ListToObjectArray4(@OrderNonDet List<@Det String> ondetList) {
        String[] arg = new String[10];
        @NonDet String @NonDet [] objArr = ondetList.toArray(arg);
        @NonDet String @NonDet [] objArr1 = ondetList.toArray(new @NonDet String @NonDet [10]);
    }

    void ListToObjectArray5(@NonDet List<@Det String> nondetList) {
        String[] arg = new String[10];
        @NonDet String @NonDet [] objArr = nondetList.toArray(arg);
    }
}
