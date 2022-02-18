import org.checkerframework.checker.signedness.qual.Signed;
import org.checkerframework.checker.signedness.qual.Unsigned;

public class WideningConversion {

  char c1;
  char c2;
  int i1;
  int i2;
  @Signed int si1;
  @Signed int si2;
  @Unsigned int ui1;
  @Unsigned int ui2;
  @Unsigned short us1;
  @Unsigned short us2;

  void compare() {
    boolean b;
    // :: error: (comparison.unsignedlhs)
    b = c1 > c2;
    // :: error: (comparison.unsignedlhs)
    b = c1 > i2;
    // :: error: (comparison.unsignedrhs)
    b = i1 > c2;
    b = i1 > i2;
  }

  void plus() {
    // Not just "int si" because it's defaulted to TOP so every assignment would work.
    @Signed int si;
    // :: error: (assignment)
    si = c1 + c2;
    // :: error: (assignment) :: error: (operation.mixed.unsignedlhs)
    si = c1 + i2;
    // :: error: (assignment) :: error: (operation.mixed.unsignedrhs)
    si = i1 + c2;
    si = i1 + i2;

    // :: error: (assignment)
    si = c1 + c2;
    // :: error: (assignment) :: error: (operation.mixed.unsignedlhs)
    si = c1 + si2;
    // :: error: (assignment) :: error: (operation.mixed.unsignedrhs)
    si = si1 + c2;
    si = si1 + si2;

    // :: error: (assignment)
    si = c1 + c2;
    // :: error: (assignment)
    si = c1 + ui2;
    // :: error: (assignment)
    si = ui1 + c2;
    // :: error: (assignment)
    si = ui1 + ui2;

    @Unsigned int ui;
    ui = c1 + c2;
    // :: error: (assignment) :: error: (operation.mixed.unsignedlhs)
    ui = c1 + i2;
    // :: error: (assignment) :: error: (operation.mixed.unsignedrhs)
    ui = i1 + c2;
    // :: error: (assignment)
    ui = i1 + i2;

    ui = c1 + c2;
    // :: error: (assignment) :: error: (operation.mixed.unsignedlhs)
    ui = c1 + si2;
    // :: error: (assignment) :: error: (operation.mixed.unsignedrhs)
    ui = si1 + c2;
    // :: error: (assignment)
    ui = si1 + si2;

    ui = c1 + c2;
    ui = c1 + ui2;
    ui = ui1 + c2;
    ui = ui1 + ui2;

    // All of these are illegal in Java, without an explicit cast.
    // char c;
    // c = c1 + c2;
    // c = c1 + i2;
    // c = i1 + c2;
    // c = i1 + i2;

    char c;
    c = (char) (c1 + c2);
    // :: warning: (cast.unsafe) :: error: (operation.mixed.unsignedlhs)
    c = (char) (c1 + i2);
    // :: warning: (cast.unsafe) :: error: (operation.mixed.unsignedrhs)
    c = (char) (i1 + c2);
    // :: warning: (cast.unsafe)
    c = (char) (i1 + i2);

    c = (char) (c1 + c2);
    // :: warning: (cast.unsafe) :: error: (operation.mixed.unsignedlhs)
    c = (char) (c1 + si2);
    // :: warning: (cast.unsafe) :: error: (operation.mixed.unsignedrhs)
    c = (char) (si1 + c2);
    // :: warning: (cast.unsafe)
    c = (char) (si1 + si2);

    c = (char) (c1 + c2);
    c = (char) (c1 + ui2);
    c = (char) (ui1 + c2);
    c = (char) (ui1 + ui2);
  }

  void to_string() {
    // :: error: (unsigned.concat)
    String s1 = "" + us1;
    // :: error: (argument)
    String s2 = String.valueOf(us2);
    // :: error: (argument)
    String s3 = Short.toString(us1);
  }
}
