// The WpiIgnoreFieldChecker does no type-checking; it only runs the assertions in
// WpiIgnoreFieldVisitor, which need some class to be compiled.

public class WpiIgnoreField {

  int field;

  void m() {
    field = 22;
  }
}
