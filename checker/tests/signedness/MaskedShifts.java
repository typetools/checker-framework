import org.checkerframework.checker.signedness.qual.*;

public class MaskedShifts {

    public void MaskedAndShifts(@Unsigned int unsigned, @Signed int signed) {

        @UnknownSignedness int testRes;

        // Use mask that renders the 9 MSB_s irrelevant.

        // Shifting right by 8, the introduced bits are masked away
        testRes = (unsigned >>> 8) & 0x7FFFFF;
        testRes = (unsigned >> 8) & 0x7FFFFF;
        testRes = (signed >>> 8) & 0x7FFFFF;
        testRes = (signed >> 8) & 0x7FFFFF;

        // Use mask that renders the 8 MSB_s irrelevant.

        // Shifting right by 8, the introduced bits are still masked away.
        testRes = (unsigned >>> 8) & 0xFFFFFF;
        testRes = (unsigned >> 8) & 0xFFFFFF;
        testRes = (signed >>> 8) & 0xFFFFFF;
        testRes = (signed >> 8) & 0xFFFFFF;

        // Use mask that renders the 7 MSB_s irrelevant

        // Now the right-most introduced bit matters
        testRes = (unsigned >>> 8) & 0x1FFFFFF;

        //:: error: (shift.signed)
        testRes = (unsigned >> 8) & 0x1FFFFFF;

        //:: error: (shift.unsigned)
        testRes = (signed >>> 8) & 0x1FFFFFF;
        testRes = (signed >> 8) & 0x1FFFFFF;
    }

    public void MaskedOrShifts(@Unsigned int unsigned, @Signed int signed) {

        @UnknownSignedness int testRes;

        // Use mask that renders the 9 MSB_s irrelevant.

        // Shifting right by 8, the introduced bits are masked away.
        testRes = (unsigned >>> 8) | 0xFF800000;
        testRes = (unsigned >> 8) | 0xFF800000;
        testRes = (signed >>> 8) | 0xFF800000;
        testRes = (signed >> 8) | 0xFF800000;

        // Use mask that render ths 8 MSB_s irrelevant.

        // Shifting right by 8, the introduced bits are still masked away.
        testRes = (unsigned >>> 8) | 0xFF000000;
        testRes = (unsigned >> 8) | 0xFF000000;
        testRes = (signed >>> 8) | 0xFF000000;
        testRes = (signed >> 8) | 0xFF000000;

        // Use mask that renders the 7 MSB_s irrelevant.

        // The right-most introduced bit now matters.
        testRes = (unsigned >>> 8) | 0xFE000000;

        //:: error: (shift.signed)
        testRes = (unsigned >> 8) | 0xFE000000;

        //:: error: (shift.unsigned)
        testRes = (signed >>> 8) | 0xFE000000;
        testRes = (signed >> 8) | 0xFE000000;
    }
}
