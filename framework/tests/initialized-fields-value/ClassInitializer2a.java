import org.checkerframework.common.value.qual.IntVal;

public class ClassInitializer2a {

    @IntVal(1) int x;

    @IntVal(2) int y;

    int z;

    {
        x = 1;
    }

    // :: error: (contracts.postcondition.not.satisfied)
    ClassInitializer2a() {}
}
