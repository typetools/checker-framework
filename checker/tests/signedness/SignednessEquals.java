import java.util.Objects;
import org.checkerframework.checker.signedness.qual.Signed;
import org.checkerframework.checker.signedness.qual.Unsigned;

public class SignednessEquals {

  @Signed Object so;
  @Unsigned Object uo;

  @Signed Number sn;
  @Unsigned Number un;

  @Signed byte sb;
  @Unsigned byte ub;
  @Signed Byte sB;
  @Unsigned Byte uB;

  char uc;
  Character uC;

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

  void nonIntegralEquality() {
    so.equals(sn);
    // :: error: (comparison.mixed.unsignedrhs)
    so.equals(un);
    // :: error: (comparison.mixed.unsignedlhs)
    uo.equals(sn);
    uo.equals(un);

    Objects.equals(so, sn);
    // :: error: (comparison.mixed.unsignedrhs)
    Objects.equals(so, un);
    // :: error: (comparison.mixed.unsignedlhs)
    Objects.equals(uo, sn);
    Objects.equals(uo, un);

    sI.equals(sn);
    // :: error: (comparison.mixed.unsignedrhs)
    sI.equals(un);
    // :: error: (comparison.mixed.unsignedlhs)
    uI.equals(sn);
    uI.equals(un);

    Objects.equals(sI, sn);
    // :: error: (comparison.mixed.unsignedrhs)
    Objects.equals(sI, un);
    // :: error: (comparison.mixed.unsignedlhs)
    Objects.equals(uI, sn);
    Objects.equals(uI, un);
  }

  void integralEquality() {

    so.equals(sS);
    // :: error: (comparison.mixed.unsignedrhs)
    so.equals(uS);
    // :: error: (comparison.mixed.unsignedlhs)
    uo.equals(sS);
    uo.equals(uS);

    Objects.equals(so, sS);
    // :: error: (comparison.mixed.unsignedrhs)
    Objects.equals(so, uS);
    // :: error: (comparison.mixed.unsignedlhs)
    Objects.equals(uo, sS);
    Objects.equals(uo, uS);

    sB.equals(sS);
    // :: error: (comparison.mixed.unsignedrhs)
    sB.equals(uS);
    // :: error: (comparison.mixed.unsignedlhs)
    uB.equals(sS);
    uB.equals(uS);

    Objects.equals(sB, sS);
    // :: error: (comparison.mixed.unsignedrhs)
    Objects.equals(sB, uS);
    // :: error: (comparison.mixed.unsignedlhs)
    Objects.equals(uB, sS);
    Objects.equals(uB, uS);
  }
}
