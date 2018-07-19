import java.math.BigInteger;
import org.checkerframework.common.value.qual.IntRange;
import org.checkerframework.common.value.qual.PolyValue;

public class BigIntegerTest {
    void construct1(@IntRange(from = -1, to = 1) int signum, byte[] magnitude) {
        BigInteger val = new BigInteger(signum, magnitude);
    }

    void construct2(String val, @IntRange(from = 2, to = 36) int radix) {
        BigInteger value = new BigInteger(val, radix);
    }

    @PolyValue double getDoubleVal(@PolyValue BigInteger val) {
        double dval = val.doubleValue();
        return dval;
    }

    @PolyValue int getIntVal(@PolyValue BigInteger val) {
        int ival = val.intValue();
        return ival;
    }

    @PolyValue float getFloatVal(@PolyValue BigInteger val) {
        float fval = val.floatValue();
        return fval;
    }

    @PolyValue long getLongVal(@PolyValue BigInteger val) {
        long lval = val.longValue();
        return lval;
    }

    void compareTo(BigInteger val, BigInteger to) {
        @IntRange(from = -1, to = 1) int compared = val.compareTo(to);
    }

    void signum(BigInteger val) {
        @IntRange(from = -1, to = 1) int signum = val.signum();
    }
}
