import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling2;

abstract class MethodOverrideInSubtype2 extends MethodDefinedInSupertype {

  private @AinferSibling2 int getAinferSibling2() {
    return 0;
  }

  @java.lang.Override
  public int shouldReturnParent() {
    return getAinferSibling2();
  }
}
