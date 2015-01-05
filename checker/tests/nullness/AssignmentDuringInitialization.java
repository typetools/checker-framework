// This test covers Issue345 at:
// https://code.google.com/p/checker-framework/issues/detail?id=345
/* @skip-test */
public class AssignmentDuringInitialization {
    String f1;
    String f2;

    {
        //:: error:  (assignment.type.incompatible)
        f1 = f2;
        f2 = f1;
        f2.toString();   //Null pointer exception here
    }
    
    public AssignmentDuringInitialization() {
        //:: error:  (assignment.type.incompatible)
        f1 = f2;
        f2 = f1;
        f2.toString();   //Null pointer exception here
    }
    
    public void goodBehavior() {
        //this isn't a constructor or initializer
        //the receiver of this method should already be initialized
        //and therefore f1 and f2 should already be initialized
        f1 = f2;
        f2 = f1;
        f2.toString();   //No exception here
    }
    
    public static void main(String [] args) {
        AssignmentDuringInitialization a = new AssignmentDuringInitialization();
    }
}