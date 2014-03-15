/*
 * Make sure that unqualified type variables still work.
 */
public class TypeVariable<X> {
  X m() {
    return null;
  }

  <Y extends Object> Y bar() {
    return null;
  }
}
