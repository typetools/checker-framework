import java.util.LinkedList;
import org.checkerframework.checker.signedness.qual.Signed;
import org.checkerframework.checker.signedness.qual.Unsigned;

public class BoxedPrimitives {

  @Signed int si;
  @Unsigned int ui;

  @Signed Integer sbi;
  @Unsigned Integer ubi;

  void argSigned(@Signed int x) {
    si = x;
    sbi = x;
    // :: error: (assignment)
    ui = x;
    // :: error: (assignment)
    ubi = x;
  }

  void argUnsigned(@Unsigned int x) {
    // :: error: (assignment)
    si = x;
    // :: error: (assignment)
    sbi = x;
    ui = x;
    ubi = x;
  }

  void argSignedBoxed(@Signed Integer x) {
    si = x;
    sbi = x;
    // :: error: (assignment)
    ui = x;
    // :: error: (assignment)
    ubi = x;
  }

  void argUnsignedBoxed(@Unsigned Integer x) {
    // :: error: (assignment)
    si = x;
    // :: error: (assignment)
    sbi = x;
    ui = x;
    ubi = x;
  }

  void client() {
    argSigned(si);
    argSignedBoxed(si);
    argSigned(sbi);
    argSignedBoxed(sbi);
    // :: error: (argument)
    argUnsigned(si);
    // :: error: (argument)
    argUnsignedBoxed(si);
    // :: error: (argument)
    argUnsigned(sbi);
    // :: error: (argument)
    argUnsignedBoxed(sbi);
    // :: error: (argument)
    argSigned(ui);
    // :: error: (argument)
    argSignedBoxed(ui);
    // :: error: (argument)
    argSigned(ubi);
    // :: error: (argument)
    argSignedBoxed(ubi);
    argUnsigned(ui);
    argUnsignedBoxed(ui);
    argUnsigned(ubi);
    argUnsignedBoxed(ubi);
  }

  public LinkedList<Integer> commands;

  void forLoop() {
    for (Integer ix : this.commands) {
      argSigned(ix);
    }
  }
}
