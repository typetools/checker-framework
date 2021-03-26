import org.checkerframework.common.initializedfields.qual.InitializedFields;

public class SimpleConstructor {

  int x;
  int y;
  int z;

  SimpleConstructor() {
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

    z = 3;
  }
}
