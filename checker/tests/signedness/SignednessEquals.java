import org.checkerframework.checker.signedness.qual.Signed;
import org.checkerframework.checker.signedness.qual.Unsigned;

import java.util.Date;
import java.util.Objects;

public class SignednessEquals {

    @Signed Object so;
    @Unsigned Object uo;

    @Signed Date sd;
    @Unsigned Date ud;

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

    void nonIntegralEquality() {
        so.equals(sd);
        so.equals(ud);
        uo.equals(sd);
        uo.equals(ud);

        Objects.equals(so, sd);
        Objects.equals(so, ud);
        Objects.equals(uo, sd);
        Objects.equals(uo, ud);

        sI.equals(sd);
        sI.equals(ud);
        uI.equals(sd);
        uI.equals(ud);

        Objects.equals(sI, sd);
        Objects.equals(sI, ud);
        Objects.equals(uI, sd);
        Objects.equals(uI, ud);
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
