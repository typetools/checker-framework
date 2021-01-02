import org.checkerframework.common.value.qual.IntVal;

public class ClassInitializer3 {

    @IntVal(1) int x;

    @IntVal(2) int y;

    int z;

    {
        if (Math.random() < 0) {
            x = 1;
        } else {
            x = 1;
        }
    }

    {
        y = 2;
    }

    ClassInitializer3() {}
}
