// Test case for issue #2367: https://tinyurl.com/cfissue/2367

public class Issue2367 {

  // Within the signed byte range

  byte b1 = 75;
  byte b2 = (byte) 75;
  byte b3 = (byte) -120;

  // Outside the signed byte range

  // Without the `(byte)` cast, all of these produce the following javac error:
  //   error: incompatible types: possible lossy conversion from int to byte
  // The Value Checker's `cast.unsafe` error is analogous and is desirable.

  byte b4 = (byte) 139; // b4 == -117
  byte b5 = (byte) -240;
  byte b6 = (byte) 251;

  // Outside the signed byte range, but written as a hexadecimal literal.

  byte b7 = (byte) 0x8B; // 0x8B == 137, and b4 == -117
}
