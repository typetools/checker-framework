import org.checkerframework.common.value.qual.IntVal;

public class ClassInitializer2 {

    @IntVal(1) int x;

    @IntVal(2) int y;

    int z;

    {
        x = 1;
    }

    {
        y = 2;
    }

    ClassInitializer2() {}
}
