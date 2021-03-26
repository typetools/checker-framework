import org.checkerframework.common.initializedfields.qual.EnsuresInitializedFields;
import org.checkerframework.common.initializedfields.qual.InitializedFields;

public class HelperMethodInitializesFields {

  int x;
  int y;
  int z;

  HelperMethodInitializesFields(int ignore) {
    helperMethodXY();
    @InitializedFields({"x", "y"}) HelperMethodInitializesFields hmif2 = this;
    z = 3;
  }

  HelperMethodInitializesFields(int ignore, String ignore2) {
    helperMethodXY2();
    @InitializedFields({"x", "y"}) HelperMethodInitializesFields hmif2 = this;
    z = 3;
  }

  HelperMethodInitializesFields(long ignore) {
    this.helperMethodXY();
    @InitializedFields({"x", "y"}) HelperMethodInitializesFields hmif2 = this;
    z = 3;
  }

  HelperMethodInitializesFields(long ignore, String ignore2) {
    this.helperMethodXY2();
    @InitializedFields({"x", "y"}) HelperMethodInitializesFields hmif2 = this;
    z = 3;
  }

  HelperMethodInitializesFields(float ignore) {
    staticHelperMethodXY(this);
    @InitializedFields({"x", "y"}) HelperMethodInitializesFields hmif2 = this;
    z = 3;
  }

  HelperMethodInitializesFields(double ignore) {
    this.staticHelperMethodXY(this);
    @InitializedFields({"x", "y"}) HelperMethodInitializesFields hmif2 = this;
    z = 3;
  }

  HelperMethodInitializesFields(boolean ignore) {
    new OtherClass().helperMethodXY(this);
    @InitializedFields({"x", "y"}) HelperMethodInitializesFields hmif2 = this;
    z = 3;
  }

  HelperMethodInitializesFields(char ignore) {
    new OtherClass().helperMethodXY2(0, this);
    @InitializedFields({"x", "y"}) HelperMethodInitializesFields hmif2 = this;
    z = 3;
  }

  HelperMethodInitializesFields(int ignore1, byte ignore2) {
    new OtherClass().staticHelperMethodXY(this);
    @InitializedFields({"x", "y"}) HelperMethodInitializesFields hmif2 = this;
    z = 3;
  }

  HelperMethodInitializesFields(int ignore1, short ignore2) {
    new OtherClass().staticHelperMethodXY2(0, this);
    @InitializedFields({"x", "y"}) HelperMethodInitializesFields hmif2 = this;
    z = 3;
  }

  HelperMethodInitializesFields(int ignore1, int ignore2) {
    OtherClass.staticHelperMethodXY(this);
    @InitializedFields({"x", "y"}) HelperMethodInitializesFields hmif2 = this;
    z = 3;
  }

  HelperMethodInitializesFields(int ignore1, long ignore2) {
    OtherClass.staticHelperMethodXY2(0, this);
    @InitializedFields({"x", "y"}) HelperMethodInitializesFields hmif2 = this;
    z = 3;
  }

  // Simple tests of  LUB

  HelperMethodInitializesFields(boolean ignore1, int ignore) {
    z = 3;
    helperMethodXY();
    @InitializedFields({"x", "y", "z"}) HelperMethodInitializesFields hmif2 = this;
  }

  HelperMethodInitializesFields(boolean ignore1, char ignore) {
    z = 3;
    helperMethodXY2();
    @InitializedFields({"x", "y", "z"}) HelperMethodInitializesFields hmif2 = this;
  }

  HelperMethodInitializesFields(boolean ignore1, float ignore) {
    z = 3;
    staticHelperMethodXY(this);
    @InitializedFields({"x", "y", "z"}) HelperMethodInitializesFields hmif2 = this;
  }

  HelperMethodInitializesFields(boolean ignore1, boolean ignore) {
    z = 3;
    new OtherClass().helperMethodXY(this);
    @InitializedFields({"x", "y", "z"}) HelperMethodInitializesFields hmif2 = this;
  }

  HelperMethodInitializesFields(boolean ignore1, short ignore2) {
    z = 3;
    OtherClass.staticHelperMethodXY(this);
    @InitializedFields({"x", "y", "z"}) HelperMethodInitializesFields hmif2 = this;
  }

  // More complex tests of  LUB

  HelperMethodInitializesFields(byte ignore1, int ignore) {
    y = 2;
    z = 3;
    helperMethodXY();
    @InitializedFields({"x", "y", "z"}) HelperMethodInitializesFields hmif2 = this;
  }

  HelperMethodInitializesFields(byte ignore1, long ignore) {
    y = 2;
    helperMethodXY();
    @InitializedFields({"x", "y"}) HelperMethodInitializesFields hmif2 = this;
    z = 3;
  }

  // The helper methods

  @EnsuresInitializedFields(
      value = "this",
      fields = {"x", "y"})
  void helperMethodXY() {
    x = 1;
    this.y = 1;
  }

  @EnsuresInitializedFields(fields = {"x", "y"})
  void helperMethodXY2() {
    x = 1;
    this.y = 1;
  }

  @EnsuresInitializedFields(
      value = "#1",
      fields = {"x", "y"})
  static void staticHelperMethodXY(HelperMethodInitializesFields hmif) {
    hmif.x = 1;
    hmif.y = 1;
  }
}

class OtherClass {

  @EnsuresInitializedFields(
      value = "#1",
      fields = {"x", "y"})
  void helperMethodXY(HelperMethodInitializesFields hmif) {
    hmif.x = 1;
    hmif.y = 1;
  }

  @EnsuresInitializedFields(
      value = "#2",
      fields = {"x", "y"})
  void helperMethodXY2(int ignore, HelperMethodInitializesFields hmif) {
    hmif.x = 1;
    hmif.y = 1;
  }

  @EnsuresInitializedFields(
      value = "#1",
      fields = {"x", "y"})
  static void staticHelperMethodXY(HelperMethodInitializesFields hmif) {
    hmif.x = 1;
    hmif.y = 1;
  }

  @EnsuresInitializedFields(
      value = "#2",
      fields = {"x", "y"})
  static void staticHelperMethodXY2(int ignore, HelperMethodInitializesFields hmif) {
    hmif.x = 1;
    hmif.y = 1;
  }
}
