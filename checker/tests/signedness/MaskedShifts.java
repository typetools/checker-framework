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

        // :: error: (shift.signed)
        testRes = (unsigned >> 8) & 0x1FFFFFF;

        // :: error: (shift.unsigned)
        testRes = (signed >>> 8) & 0x1FFFFFF;
        testRes = (signed >> 8) & 0x1FFFFFF;

        // Use mask that doesn't render the MSB irrelevent, but does render the next 7 MSB_s
        // irrelevant.

        // Now the left-most introduced bit matters
        testRes = (unsigned >>> 8) & 0x90FFFFFF;

        // :: error: (shift.signed)
        testRes = (unsigned >> 8) & 0x90FFFFFF;

        // :: error: (shift.unsigned)
        testRes = (signed >>> 8) & 0x90FFFFFF;
        testRes = (signed >> 8) & 0x90FFFFFF;

        // Use mask that doesn't render any bits irrelevent

        testRes = (unsigned >>> 8) & 0xFFFFFFFF;

        // :: error: (shift.signed)
        testRes = (unsigned >> 8) & 0xFFFFFFFF;

        // :: error: (shift.unsigned)
        testRes = (signed >>> 8) & 0xFFFFFFFF;
        testRes = (signed >> 8) & 0xFFFFFFFF;

        // Tests with no parenthesis (only 8 and 0 MSB_s)

        // Use mask that renders the 8 MSB_s irrelevant.

        // Shifting right by 8, the introduced bits are still masked away.
        testRes = unsigned >>> 8 & 0xFFFFFF;
        testRes = unsigned >> 8 & 0xFFFFFF;
        testRes = signed >>> 8 & 0xFFFFFF;
        testRes = signed >> 8 & 0xFFFFFF;

        // Use mask that doesn't render any bits irrelevent

        testRes = unsigned >>> 8 & 0xFFFFFFFF;

        // :: error: (shift.signed)
        testRes = unsigned >> 8 & 0xFFFFFFFF;

        // :: error: (shift.unsigned)
        testRes = signed >>> 8 & 0xFFFFFFFF;
        testRes = signed >> 8 & 0xFFFFFFFF;

        // Tests with double parenthesis (only 8 and 0 MSB_s)

        // Use mask that renders the 8 MSB_s irrelevant.

        // Shifting right by 8, the introduced bits are still masked away.
        testRes = ((unsigned >>> 8)) & 0xFFFFFF;
        testRes = ((unsigned >> 8)) & 0xFFFFFF;
        testRes = ((signed >>> 8)) & 0xFFFFFF;
        testRes = ((signed >> 8)) & 0xFFFFFF;

        // Use mask that doesn't render any bits irrelevent

        testRes = ((unsigned >>> 8)) & 0xFFFFFFFF;

        // :: error: (shift.signed)
        testRes = ((unsigned >> 8)) & 0xFFFFFFFF;

        // :: error: (shift.unsigned)
        testRes = ((signed >>> 8)) & 0xFFFFFFFF;
        testRes = ((signed >> 8)) & 0xFFFFFFFF;

        // Tests shift on right (only 8 and 0 MSB_s)

        // Use mask that renders the 8 MSB_s irrelevant.

        // Shifting right by 8, the introduced bits are still masked away.
        testRes = 0xFFFFFF & (unsigned >>> 8);
        testRes = 0xFFFFFF & (unsigned >> 8);
        testRes = 0xFFFFFF & (signed >>> 8);
        testRes = 0xFFFFFF & (signed >> 8);

        // Use mask that doesn't render any bits irrelevent

        testRes = 0xFFFFFFFF & (unsigned >>> 8);

        // :: error: (shift.signed)
        testRes = 0xFFFFFFFF & (unsigned >> 8);

        // :: error: (shift.unsigned)
        testRes = 0xFFFFFFFF & (signed >>> 8);
        testRes = 0xFFFFFFFF & (signed >> 8);

        // Tests with parenthesis on mask (only 8 and 0 MSB_s)

        // Use mask that renders the 8 MSB_s irrelevant.

        // Shifting right by 8, the introduced bits are still masked away.
        testRes = unsigned >>> 8 & (0xFFFFFF);
        testRes = unsigned >> 8 & (0xFFFFFF);
        testRes = signed >>> 8 & (0xFFFFFF);
        testRes = signed >> 8 & (0xFFFFFF);

        // Use mask that doesn't render any bits irrelevent

        testRes = unsigned >>> 8 & (0xFFFFFFFF);

        // :: error: (shift.signed)
        testRes = unsigned >> 8 & (0xFFFFFFFF);

        // :: error: (shift.unsigned)
        testRes = signed >>> 8 & (0xFFFFFFFF);
        testRes = signed >> 8 & (0xFFFFFFFF);

        // Tests with double parenthesis on mask (only 8 and 0 MSB_s)

        // Use mask that renders the 8 MSB_s irrelevant.

        // Shifting right by 8, the introduced bits are still masked away.
        testRes = unsigned >>> 8 & ((0xFFFFFF));
        testRes = unsigned >> 8 & ((0xFFFFFF));
        testRes = signed >>> 8 & ((0xFFFFFF));
        testRes = signed >> 8 & ((0xFFFFFF));

        // Use mask that doesn't render any bits irrelevent

        testRes = unsigned >>> 8 & ((0xFFFFFFFF));

        // :: error: (shift.signed)
        testRes = unsigned >> 8 & ((0xFFFFFFFF));

        // :: error: (shift.unsigned)
        testRes = signed >>> 8 & ((0xFFFFFFFF));
        testRes = signed >> 8 & ((0xFFFFFFFF));
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

        // :: error: (shift.signed)
        testRes = (unsigned >> 8) | 0xFE000000;

        // :: error: (shift.unsigned)
        testRes = (signed >>> 8) | 0xFE000000;
        testRes = (signed >> 8) | 0xFE000000;

        // Use mask that doesn't render the MSB irrelevent, but does render the next 7 MSB_s
        // irrelevant.

        // Now the left-most introduced bit matters
        testRes = (unsigned >>> 8) | 0x8F000000;

        // :: error: (shift.signed)
        testRes = (unsigned >> 8) | 0x8F000000;

        // :: error: (shift.unsigned)
        testRes = (signed >>> 8) | 0x8F000000;
        testRes = (signed >> 8) | 0x8F000000;

        // Use mask that doesn't render any bits irrelevent

        testRes = (unsigned >>> 8) | 0x0;

        // :: error: (shift.signed)
        testRes = (unsigned >> 8) | 0x0;

        // :: error: (shift.unsigned)
        testRes = (signed >>> 8) | 0x0;
        testRes = (signed >> 8) | 0x0;

        // Tests with no parenthesis (only 8 and 0 MSB_s)

        // Use mask that renders the 8 MSB_s irrelevant.

        // Shifting right by 8, the introduced bits are still masked away.
        testRes = unsigned >>> 8 | 0xFF000000;
        testRes = unsigned >> 8 | 0xFF000000;
        testRes = signed >>> 8 | 0xFF000000;
        testRes = signed >> 8 | 0xFF000000;

        // Use mask that doesn't render any bits irrelevent

        testRes = unsigned >>> 8 | 0x0;

        // :: error: (shift.signed)
        testRes = unsigned >> 8 | 0x0;

        // :: error: (shift.unsigned)
        testRes = signed >>> 8 | 0x0;
        testRes = signed >> 8 | 0x0;

        // Tests with double parenthesis (only 8 and 0 MSB_s)

        // Use mask that renders the 8 MSB_s irrelevant.

        // Shifting right by 8, the introduced bits are still masked away.
        testRes = ((unsigned >>> 8)) | 0xFF000000;
        testRes = ((unsigned >> 8)) | 0xFF000000;
        testRes = ((signed >>> 8)) | 0xFF000000;
        testRes = ((signed >> 8)) | 0xFF000000;

        // Use mask that doesn't render any bits irrelevent

        testRes = ((unsigned >>> 8)) | 0x0;

        // :: error: (shift.signed)
        testRes = ((unsigned >> 8)) | 0x0;

        // :: error: (shift.unsigned)
        testRes = ((signed >>> 8)) | 0x0;
        testRes = ((signed >> 8)) | 0x0;

        // Tests shift on right (only 8 and 0 MSB_s)

        // Use mask that renders the 8 MSB_s irrelevant.

        // Shifting right by 8, the introduced bits are still masked away.
        testRes = 0xFF000000 | (unsigned >>> 8);
        testRes = 0xFF000000 | (unsigned >> 8);
        testRes = 0xFF000000 | (signed >>> 8);
        testRes = 0xFF000000 | (signed >> 8);

        // Use mask that doesn't render any bits irrelevent

        testRes = 0x0 | (unsigned >>> 8);

        // :: error: (shift.signed)
        testRes = 0x0 | (unsigned >> 8);

        // :: error: (shift.unsigned)
        testRes = 0x0 | (signed >>> 8);
        testRes = 0x0 | (signed >> 8);

        // Tests with parenthesis on mask (only 8 and 0 MSB_s)

        // Use mask that renders the 8 MSB_s irrelevant.

        // Shifting right by 8, the introduced bits are still masked away.
        testRes = unsigned >>> 8 | (0xFF000000);
        testRes = unsigned >> 8 | (0xFF000000);
        testRes = signed >>> 8 | (0xFF000000);
        testRes = signed >> 8 | (0xFF000000);

        // Use mask that doesn't render any bits irrelevent

        testRes = unsigned >>> 8 | (0x0);

        // :: error: (shift.signed)
        testRes = unsigned >> 8 | (0x0);

        // :: error: (shift.unsigned)
        testRes = signed >>> 8 | (0x0);
        testRes = signed >> 8 | (0x0);

        // Tests with double parenthesis on mask (only 8 and 0 MSB_s)

        // Use mask that renders the 8 MSB_s irrelevant.

        // Shifting right by 8, the introduced bits are still masked away.
        testRes = unsigned >>> 8 | ((0xFF000000));
        testRes = unsigned >> 8 | ((0xFF000000));
        testRes = signed >>> 8 | ((0xFF000000));
        testRes = signed >> 8 | ((0xFF000000));

        // Use mask that doesn't render any bits irrelevent

        testRes = unsigned >>> 8 | ((0x0));

        // :: error: (shift.signed)
        testRes = unsigned >> 8 | ((0x0));

        // :: error: (shift.unsigned)
        testRes = signed >>> 8 | ((0x0));
        testRes = signed >> 8 | ((0x0));
    }

    public void ZeroShiftTests(@Unsigned int unsigned, @Signed int signed) {
        @UnknownSignedness int testRes;

        // Tests shift by zero followed by "and" mask
        testRes = (unsigned >>> 0) & 0xFFFFFFFF;
        testRes = (unsigned >> 0) & 0xFFFFFFFF;
        testRes = (signed >>> 0) & 0xFFFFFFFF;
        testRes = (signed >> 0) & 0xFFFFFFFF;

        // Tests shift by zero followed by "or" mask
        testRes = (unsigned >>> 0) | 0x0;
        testRes = (unsigned >> 0) | 0x0;
        testRes = (signed >>> 0) | 0x0;
        testRes = (signed >> 0) | 0x0;
    }
}
