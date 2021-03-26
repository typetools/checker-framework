// Example code from the user manual

import org.checkerframework.checker.signedness.qual.Unsigned;

public class SignednessManualExample {

  int s1 = -2;
  int s2 = -1;

  @Unsigned int u1 = 2147483646; // unsigned: 2^32 - 2, signed: -2
  @Unsigned int u2 = 2147483647; // unsigned: 2^32 - 1, signed: -1

  void m() {
    int w = s1 / s2; // OK: result is 2, which is correct for -2 / -1
    // :: error: (operation.unsignedlhs)
    int x = u1 / u2; // ERROR: result is 2, which is incorrect for (2^32 - 2) / (2^32 - 1)
  }

  int s3 = -1;
  int s4 = 5;

  @Unsigned int u3 = 2147483647; // unsigned: 2^32 - 1, signed: -1
  @Unsigned int u4 = 5;

  void m2() {
    int y = s3 % s4; // OK: result is -1, which is correct for -1 % 5
    // :: error: (operation.unsignedlhs)
    int z = u3 % u4; // ERROR: result is -1, which is incorrect for (2^32 - 1) % 5 = 2
  }

  void useLocalVariables() {

    int s1 = -2;
    int s2 = -1;

    @Unsigned int u1 = 2147483646; // unsigned: 2^32 - 2, signed: -2
    @Unsigned int u2 = 2147483647; // unsigned: 2^32 - 1, signed: -1

    int w = s1 / s2; // OK: result is 2, which is correct for -2 / -1
    int x = u1 / u2; // OK; computation over constants, interpreted as signed; result is signed

    int s3 = -1;
    int s4 = 5;

    @Unsigned int u3 = 2147483647; // unsigned: 2^32 - 1, signed: -1
    @Unsigned int u4 = 5;

    int y = s3 % s4; // OK: result is -1, which is correct for -1 % 5
    int z = u3 % u4; // OK; computation over constants, interpreted as signed; result is signed
  }
}
