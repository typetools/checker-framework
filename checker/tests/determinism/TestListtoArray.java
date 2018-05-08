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
        String[] objArr = detList.toArray(new String[10]);
    }

    void ListToObjectArray4(@OrderNonDet List<@Det String> ondetList) {
        @NonDet String[] objArr = ondetList.toArray(new String[10]);
    }

    void ListToObjectArray5(@NonDet List<@Det String> nondetList) {
        @NonDet String[] objArr = nondetList.toArray(new String[10]);
    }
}
