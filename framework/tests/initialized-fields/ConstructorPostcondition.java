// import org.checkerframework.common.initializedfields.qual.InitializedFields;

public class ConstructorPostcondition {

    int x;
    int y;
    int z;

    ConstructorPostcondition() {
        x = 1;
        y = 2;
        z = 3;
    }

    // :: error: (contracts.postcondition.not.satisfied)
    ConstructorPostcondition(int ignore) {
        x = 1;
        y = 2;
    }
}
