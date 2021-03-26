// See gist: https://gist.github.com/JonathanBurke/6c1c1c28161a451611ad
// for more information on what was going wrong here
public class Issue457<T extends Number> {

  @SuppressWarnings("unused")
  public void f(T t) {
    final T obj = t;

    @SuppressWarnings("signedness:assignment.type.incompatible") // cast
    Float objFloat = (obj instanceof Float) ? (Float) obj : null;

    // An error will be emitted on this line before the fix for Issue457
    t = obj;
  }
}
