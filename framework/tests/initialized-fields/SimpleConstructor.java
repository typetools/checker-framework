import org.checkerframework.common.initializedfields.qual.InitializedFields;

public class SimpleConstructor {

    int x;
    int y;
    int z;

    SimpleConstructor(byte ignore) {
        // :: error: (assignment.type.incompatible)
        @InitializedFields({"x", "y", "z"}) SimpleConstructor sc1 = this;
        @InitializedFields() SimpleConstructor sc2 = this;

        x = 1;

        // :: error: (assignment.type.incompatible)
        @InitializedFields({"x", "y", "z"}) SimpleConstructor sc3 = this;
        @InitializedFields({"x"}) SimpleConstructor sc4 = this;

        this.y = 1;

        // :: error: (assignment.type.incompatible)
        @InitializedFields({"x", "y", "z"}) SimpleConstructor sc5 = this;
        @InitializedFields({"x", "y"}) SimpleConstructor sc6 = this;
        @InitializedFields({"y", "x"}) SimpleConstructor sc7 = this;
    }

    SimpleConstructor(short ignore) {}

    SimpleConstructor(int ignore) {}

    SimpleConstructor(long ignore) {}

    SimpleConstructor(float ignore) {}

    SimpleConstructor(double ignore) {}

    SimpleConstructor(boolean ignore) {}

    SimpleConstructor(char ignore) {}
}
