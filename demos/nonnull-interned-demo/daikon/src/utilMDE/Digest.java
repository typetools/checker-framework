// This code is lifted from examples/Manifest.java.

package utilMDE;

import java.security.*;
import java.io.*;

/** Primarily for getFileDigest. */
public class Digest {

  /**
   * This convenience method is used by both create() and verify().  It
   * reads the contents of a named file and computes a message digest
   * for it, using the specified MessageDigest object.
   **/
  public static byte[] getFileDigest(String filename, MessageDigest md)
       throws IOException {
    // Make sure there is nothing left behind in the MessageDigest
    md.reset();

    // Create a stream to read from the file and compute the digest
    DigestInputStream in =
      new DigestInputStream(new FileInputStream(filename),md);

    // Read to the end of the file, discarding everything we read.
    // The DigestInputStream automatically passes all the bytes read to
    // the update() method of the MessageDigest
    while (in.read(buffer) != -1) /* do nothing */ ;

    // Finally, compute and return the digest value.
    byte[] result = md.digest();
    in.close();
    return result;
  }

  /** This static buffer is used by getFileDigest() above */
  public static byte[] buffer = new byte[4096];

  /** This array is used to convert from bytes to hexadecimal numbers */
  static final char[] digits = { '0', '1', '2', '3', '4', '5', '6', '7',
                                 '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

  /**
   * A convenience method to convert an array of bytes to a String.  We do
   * this simply by converting each byte to two hexadecimal digits.  Something
   * like Base 64 encoding is more compact, but harder to encode.
   **/
  public static String hexEncode(byte[] bytes) {
    StringBuffer s = new StringBuffer(bytes.length * 2);
    for (int i = 0; i < bytes.length; i++) {
      byte b = bytes[i];
      s.append(digits[(b & 0xf0) >> 4]);
      s.append(digits[b & 0x0f]);
    }
    return s.toString();
  }

  /**
   * A convenience method to convert in the other direction, from a string
   * of hexadecimal digits to an array of bytes.
   **/
  public static byte[] hexDecode(String s) throws IllegalArgumentException {
    try {
      int len = s.length();
      byte[] r = new byte[len/2];
      for (int i = 0; i < r.length; i++) {
        int digit1 = s.charAt(i*2), digit2 = s.charAt(i*2 + 1);
        if ((digit1 >= '0') && (digit1 <= '9')) digit1 -= '0';
        else if ((digit1 >= 'a') && (digit1 <= 'f')) digit1 -= 'a' - 10;
        if ((digit2 >= '0') && (digit2 <= '9')) digit2 -= '0';
        else if ((digit2 >= 'a') && (digit2 <= 'f')) digit2 -= 'a' - 10;
        r[i] = (byte)((digit1 << 4) + digit2);
      }
      return r;
    }
    catch (Exception e) {
      throw new IllegalArgumentException("hexDecode(): invalid input");
    }
  }

}
