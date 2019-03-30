// Test case for issue #2367: http://tinyurl.com/cfissue/2367

public class Issue2367 {

    // Within the signed byte range

    byte b1 = 75;
    byte b2 = (byte) 75;
    byte b3 = (byte) -120;

    // Outside the signed byte range

    // Without the `(byte)` cast, all of these produce the following javac error:
    //   error: incompatible types: possible lossy conversion from int to byte
    // The Value Checker's `cast.unsafe` error is analogous and is desirable.

    // :: warning: (cast.unsafe)
    byte b4 = (byte) 139; // b4 == -117
    // :: warning: (cast.unsafe)
    byte b5 = (byte) -240;
    // :: warning: (cast.unsafe)
    byte b6 = (byte) 251;

    // Outside the signed byte range, but written as a hexadecimal literal.

    // As a special case, the Constant Value Checker could not issue a warning when a value is
    // within the signed byte range AND the value was specified in hexadecimal.
    // Such a special case is not yet implemented, and I don't see how to do so.
    // The program element "(byte) 0x8B" has already been converted to "(byte)139" by javac before
    // the Checker Framework gets access to it.

    // :: warning: (cast.unsafe)
    byte b7 = (byte) 0x8B; // 0x8B == 137, and b4 == -117
}
