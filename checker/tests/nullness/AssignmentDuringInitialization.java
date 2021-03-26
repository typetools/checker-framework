// This test covers Issue345 at:
// https://github.com/typetools/checker-framework/issues/345
public class AssignmentDuringInitialization {
  String f1;
  String f2;

  String f3;
  String f4;

  String f5;
  String f6;

  {
    // :: error:  (assignment.type.incompatible)
    f1 = f2;
    f2 = f1;
    f2.toString(); // Null pointer exception here
  }

  public AssignmentDuringInitialization() {
    // :: error:  (assignment.type.incompatible)
    f3 = f4;
    f4 = f3;
    f4.toString(); // Null pointer exception here

    f5 = "hello";
    f6 = f5;
  }

  public void goodBehavior() {
    // this isn't a constructor or initializer
    // the receiver of this method should already be initialized
    // and therefore f1 and f2 should already be initialized
    f5 = f6;
    f6 = f5;
    f6.toString(); // No exception here
  }

  public static void main(String[] args) {
    AssignmentDuringInitialization a = new AssignmentDuringInitialization();
  }
}
