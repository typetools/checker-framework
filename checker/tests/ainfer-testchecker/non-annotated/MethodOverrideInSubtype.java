import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

public class MethodOverrideInSubtype extends MethodDefinedInSupertype {
  @java.lang.Override
  public int shouldReturnAinferSibling1() {
    return getAinferSibling1();
  }

  private @AinferSibling1 int getAinferSibling1() {
    return 0;
  }

  @java.lang.Override
  public int shouldReturnParent() {
    return getAinferSibling1();
  }
}
