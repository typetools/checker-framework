import checkers.nullness.quals.*;

//:: error: (commitment.fields.uninitialized)
public class NonNullInitialization {
  private String test;
   
  public static void main(String[] args) {
    NonNullInitialization n = new NonNullInitialization();
    n.test.equals("ASD");
  }
   
}
