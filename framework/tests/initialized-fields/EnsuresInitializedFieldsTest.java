import org.checkerframework.common.initializedfields.qual.EnsuresInitializedFields;

// import org.checkerframework.common.initializedfields.qual.InitializedFields;

public class EnsuresInitializedFieldsTest {

    int x;
    int y;
    int z;

    EnsuresInitializedFieldsTest() {
        x = 1;
        y = 2;
        z = 3;
    }

    @EnsuresInitializedFields(
            value = "this",
            fields = {"x", "y"})
    // :: error: (contracts.postcondition.not.satisfied)
    void setsX() {
        x = 1;
    }

    @EnsuresInitializedFields(fields = {"x", "y"})
    // :: error: (contracts.postcondition.not.satisfied)
    void setsX2() {
        x = 1;
    }

    @EnsuresInitializedFields(
            value = "#1",
            fields = {"x", "y"})
    // :: error: (contracts.postcondition.not.satisfied)
    void setsX(EnsuresInitializedFieldsTest eift) {
        eift.x = 1;
    }

    @EnsuresInitializedFields(
            value = "this",
            fields = {"x", "y"})
    void setsXY() {
        x = 1;
        y = 2;
    }

    @EnsuresInitializedFields(fields = {"x", "y"})
    void setsXY2() {
        x = 1;
        y = 2;
    }

    @EnsuresInitializedFields(
            value = "#1",
            fields = {"x", "y"})
    void setsXY(EnsuresInitializedFieldsTest eift) {
        eift.x = 1;
        eift.y = 2;
    }

    @EnsuresInitializedFields(
            value = "#1",
            fields = {"x", "y"})
    void setsXY2(EnsuresInitializedFieldsTest eift) {
        setsXY(eift);
    }

    @EnsuresInitializedFields(
            value = "#1",
            fields = {"x", "y"})
    @EnsuresInitializedFields(
            value = "#2",
            fields = {"x", "z"})
    void setsXY2(EnsuresInitializedFieldsTest eift1, EnsuresInitializedFieldsTest eift2) {
        setsXY(eift1);
        setsX(eift2);
        eift2.z = 3;
    }
}
