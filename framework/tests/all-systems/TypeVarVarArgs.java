// I don't understand why, but renaming this class from TypeVarVarArgs to TypeVarVarargs (with
// lower-case a) causes inference tests to fail.
abstract class TypeVarVarArgs {
  public abstract <T> T foo();

  public abstract void bar(Integer... s);

  public void m() {
    bar(foo(), foo());
  }
}
