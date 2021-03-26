import org.checkerframework.checker.signedness.qual.Signed;
import org.checkerframework.checker.signedness.qual.SignedPositive;
import org.checkerframework.checker.signedness.qual.Unsigned;

public class SignednessAssignments {

  @Signed byte sb;
  @Unsigned byte ub;
  @Signed Byte sB;
  @Unsigned Byte uB;

  @Unsigned char uc;
  @Unsigned Character uC;

  @Signed short ss;
  @Unsigned short us;
  @Signed Short sS;
  @Unsigned Short uS;

  @Signed int si;
  @Unsigned int ui;
  @Signed Integer sI;
  @Unsigned Integer uI;

  @Signed long sl;
  @Unsigned long ul;
  @Signed Long sL;
  @Unsigned Long uL;

  void assignmentsByte() {
    @Signed byte i1 = sb;
    @Unsigned byte i2 = ub;
    @Signed byte i3 = sB;
    @Unsigned byte i4 = uB;

    @Signed Byte i91 = sb;
    @Unsigned Byte i92 = ub;
    @Signed Byte i93 = sB;
    @Unsigned Byte i94 = uB;
  }

  void assignmentsShort() {
    @SignedPositive short i1 = sb;
    @SignedPositive short i2 = ub;
    @SignedPositive short i3 = sB;
    @SignedPositive short i4 = uB;

    @Signed short i9 = ss;
    @Unsigned short i10 = us;
    @Signed short i11 = sS;
    @Unsigned short i12 = uS;

    @Signed Short i91 = ss;
    @Unsigned Short i92 = us;
    @Signed Short i93 = sS;
    @Unsigned Short i94 = uS;
  }

  void assignmentsChar() {
    // These are commented out because they are Java errors.
    // @Unsigned char i2 = ub;
    // @Unsigned char i4 = uB;
    // @Unsigned char i10 = us;
    // @Unsigned char i12 = uS;
  }

  void assignmentsInt() {
    @SignedPositive int i1 = sb;
    @SignedPositive int i2 = ub;
    @SignedPositive int i3 = sB;
    @SignedPositive int i4 = uB;

    @SignedPositive int i6 = uc;
    @SignedPositive int i8 = uC;

    @SignedPositive int i9 = ss;
    @SignedPositive int i10 = us;
    @SignedPositive int i11 = sS;
    @SignedPositive int i12 = uS;

    @Signed int i13 = si;
    @Unsigned int i14 = ui;
    @Signed int i15 = sI;
    @Unsigned int i16 = uI;

    @Signed Integer i91 = si;
    @Unsigned Integer i92 = ui;
    @Signed Integer i93 = sI;
    @Unsigned Integer i94 = uI;
  }

  void assignmentsLong() {
    @SignedPositive long i1 = sb;
    @SignedPositive long i2 = ub;
    @SignedPositive long i3 = sB;
    @SignedPositive long i4 = uB;

    @SignedPositive long i6 = uc;
    @SignedPositive long i8 = uC;

    @SignedPositive long i9 = ss;
    @SignedPositive long i10 = us;
    @SignedPositive long i11 = sS;
    @SignedPositive long i12 = uS;

    @SignedPositive long i13 = si;
    @SignedPositive long i14 = ui;
    @SignedPositive long i15 = sI;
    @SignedPositive long i16 = uI;

    @Signed long i17 = sl;
    @Unsigned long i18 = ul;
    @Signed long i19 = sL;
    @Unsigned long i20 = uL;

    @Signed Long i91 = sl;
    @Unsigned Long i92 = ul;
    @Signed Long i93 = sL;
    @Unsigned Long i94 = uL;
  }
}
