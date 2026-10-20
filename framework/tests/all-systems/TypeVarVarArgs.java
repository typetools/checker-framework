abstract class TypeVarVarArgs {
  public abstract <T> T foo();

  public abstract void bar(Integer... s);

  public void m() {
    bar(foo(), foo());
  }
}
