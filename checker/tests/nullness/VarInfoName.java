public abstract class VarInfoName {

  public abstract <T extends Object> T accept(Visitor<T> v);

  public abstract static class Visitor<T extends Object> {}

  public abstract static class BooleanAndVisitor extends Visitor<Boolean> {
    private boolean result;

    public BooleanAndVisitor(VarInfoName name) {
      // :: error: (argument.type.incompatible) :: warning: (nulltest.redundant)
      result = (name.accept(this) != null);
    }
  }
}
