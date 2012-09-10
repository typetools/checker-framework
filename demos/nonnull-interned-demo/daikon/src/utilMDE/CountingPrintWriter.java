package utilMDE;

import java.io.*;

// This solution only works for PrintWriter.
// The class really ought to be reimplemented as a wrapper around an
// arbitrary Writer.

/**
 * <p> Prints formatted representations of objects to a text-output
 * stream counting the number of bytes & characters printed. </p><br>
 *
 * <p> Methods in this class never throw I/O exceptions. The client may
 * inquire as to whether any errors have occurred by invoking
 * checkError(). </p>
 */

public class CountingPrintWriter extends PrintWriter {

  // Field variables

  /** Number of written bytes using 'write' methods. */
  private int writtenBytes;
  /** Number of printed bytes using 'print' & 'println' methods. */
  private int printedBytes;
  /** Number of written chars using write methods. */
  private int writtenChars;
  /** Number of printed chars using 'print' & 'println' methods. */
  private int printedChars;

  // Constructors

  /**
   * Create a new PrintWriter, without automatic line flushing, from
   * an existing OutputStream. This convenience constructor creates
   * the necessary intermediate OutputStreamWriter, which will convert
   * characters into bytes using the default character encoding.
   *
   * @param out An output stream
   */
  public CountingPrintWriter(OutputStream out) {
    super(out);
    printedBytes = 0;
    writtenChars = 0;
  }

  /**
   * Create a new PrintWriter from an existing OutputStream. This
   * convenience constructor creates the necessary intermediate
   * OutputStreamWriter, which will convert characters into bytes
   * using the default character encoding.
   *
   * @param out An output stream
   * @param autoFlush A boolean; if true, the println() methods will flush the output buffer
   */
  public CountingPrintWriter(OutputStream out, boolean autoFlush)  {
    super(out, autoFlush);
    printedBytes = 0;
    writtenChars = 0;
  }


  /** Create a new PrintWriter, without automatic line flushing.
   *  @param out a Writer
   */
  public CountingPrintWriter(Writer out) {
    super(out);
    printedBytes = 0;
    writtenChars = 0;
  }

  /**
   * Create a new PrintWriter, without automatic line flushing.
   * @param out A writer
   * @param autoFlush A boolean; if true, the println() methods will flush the output buffer
   */
  public CountingPrintWriter(Writer out, boolean autoFlush) {
    super(out,autoFlush);
    printedBytes = 0;
    writtenChars = 0;
  }

  // Public utility methods

  /**
   * Returns the number of bytes contained in a string.
   * If s is null, returns -1
   * @param s A String
   */
  public int countBytes (String s) {
    if (s == null)
      return -1;
    int numchars = s.length();
    int numbytes = 0;
    for (int i = 0 ; i < numchars ; i++) {
      char c = s.charAt(i);
      numbytes += countBytes(c);
    }
    return numbytes;
  }

  /**
   * Returns the number of bytes used to represent
   * a character.
   * @param c A character.
   */
  public int countBytes (char c) {
      if ((c >= 0x0001) && (c <= 0x007F)) return 1;
      else if (c > 0x07FF) return 3;
      else return 2;
  }

  // Accessor Methods

  /**
   *  Returns the total number of bytes printed using any of the
   *  'print' or 'println' methods of this CountingPrintBuffer.
   */
  public int getNumberOfPrintedBytes() {
    return printedBytes;
  }

  /**
   * Returns the total number of bytes printed using any of the
   * 'write' methods of this CountingPrintBuffer.
   */
  public int getNumberOfWrittenBytes() {
    return writtenBytes;
  }

  /**
   * Returns the total number of characters printed using any
   * of the 'print' or 'println' methods of this CountingPrintBuffer.
   */
  public int getNumberOfPrintedChars() {
    return printedChars;
  }

  // PRINT METHODS

  // All these methods increment the byte and char
  // count, and call their super method.


  // It seems to me that in PrintWriter all 'print' methods convert
  // their input argument to String and call print(String s) on
  // that. However, I could not reach the source code of PrintWriter,
  // so I am not sure if this is the case. If it is, then the all
  // of the following methods are unnecessary except for print(String s).

  /**
   * Print a string. If the argument is null then the string "null" is
   * printed. Otherwise, the string's characters are converted into
   * bytes according to the platform's default character encoding, and
   * these bytes are written in exactly the manner of the write(int)
   * method.
   */
  public void print(String s) {
    if (s == null) {
      printedBytes += countBytes("null");
      printedChars += 4;
    }
    else {
      printedBytes += countBytes(s);
      printedChars += s.length();
    }
    super.print(s);
  }

  /**
   * Print a boolean value. The string produced by
   * String.valueOf(boolean) is translated into bytes according to the
   * platform's default character encoding, and these bytes are
   * written in exactly the manner of the write(int) method.
   */
  public void print(boolean b) {
    String s = String.valueOf(b);
    printedBytes += countBytes(s);
    printedChars += s.length();
    super.print(b);
  }

  /**
   * Print a character. The character is translated into one or more
   * bytes according to the platform's default character encoding, and
   * these bytes are written in exactly the manner of the write(int)
   * method.
   */
  public void print(char c) {
    printedBytes += countBytes(c);
    printedChars ++;
  }

  /**
   * Print an array of characters. The characters are converted into
   * bytes according to the platform's default character encoding, and
   * these bytes are written in exactly the manner of the write(int)
   * method.
   */
  public void print(char[] s) {
    for (int i=0; i < s.length; i++) {
      printedBytes += countBytes(s[i]);

    }
    printedChars += s.length;
    super.print(s);
  }

  /**
   * Print a double-precision floating-point number. The string
   * produced by String.valueOf(double) is translated into bytes
   * according to the platform's default character encoding, and these
   * bytes are written in exactly the manner of the write(int) method.
   */
  public void print(double d) {
    String s = String.valueOf(d);
    printedBytes += countBytes(s);
    printedChars += s.length();
    super.print(d);
  }


  /**
   * Print a floating-point number. The string produced by
   * String.valueOf(float) is translated into bytes according to the
   * platform's default character encoding, and these bytes are
   * written in exactly the manner of the write(int) method.
   */
  public void print(float f) {
    String s = String.valueOf(f);
    printedBytes += countBytes(s);
    printedChars += s.length();
    super.print(f);
  }

  /**
   *  Print an integer. The string produced by String.valueOf(int) is
   *  translated into bytes according to the platform's default
   *  character encoding, and these bytes are written in exactly the
   *  manner of the write(int) method.
   */
  public void print(int i) {
    String s = String.valueOf(i);
    printedBytes += countBytes(s);
    printedChars += s.length();
    super.print(i);
  }

  /**  Resets all the byte and char counters. */
  public void resetAll() {
    resetPrintedByteCounter();
    resetPrintedCharCounter();
    resetWrittenByteCounter();
    resetWrittenCharCounter();
  }
  /**  Resets printedByte counter. */
  public void resetPrintedByteCounter() {
    printedBytes = 0;
  }
  /**  Resets printedChar counter. */
  public void resetPrintedCharCounter() {
    printedChars = 0;
  }
  /**  Resets writtenByte counter. */
  public void resetWrittenByteCounter() {
    writtenBytes = 0;
  }
  /**  Resets writtenChar counter. */
  public void resetWrittenCharCounter() {
    writtenChars = 0;
  }

  /**
   *  Print a long integer. The string produced by
   *  String.valueOf(long) is translated into bytes according to the
   *  platform's default character encoding, and these bytes are
   *  written in exactly the manner of the write(int) method.
   */
  public void print(long l) {
    String s = String.valueOf(l);
    printedBytes += countBytes(s);
    printedChars += s.length();
    super.print(l);
  }

  /**
   * Print an object. The string produced by the
   * String.valueOf(Object) method is translated into bytes according
   * to the platform's default character encoding, and these bytes are
   * written in exactly the manner of the write(int) method.
   */
  public void print(Object obj) {
    String s = String.valueOf(obj);
    printedBytes += countBytes(s);
    printedChars += s.length();
    super.print(obj);
  }

  /**
   * Terminate the current line by writing the line separator
   * string.  The line separator string is defined by the system
   * property line.separator, and is not necessarily a single newline
   * character ('\n').
   * <p>
   * When incrementing the byte count of PrintWriter, also accounts for the
   * bytes needed to represent the line separator string.
   */
  public void println() {
    String s = System.getProperty("line.separator");
    printedBytes += countBytes(s);
    printedChars += s.length();
    super.println();
  }

  /**
   * Print a String and then terminate the line. This method behaves
   * as though it invokes print(String) and then println().
   */
  public void println(String x) {
    print(x);
    println();
  }


  /**
   * Write an array of characters. This method cannot be inherited
   * from the Writer class because it must suppress I/O exceptions.
   */
  public void write(char[] buf) {
    for (int i=0; i < buf.length; i++) {
      writtenBytes += countBytes(buf[i]);
    }
    writtenChars += buf.length;
    super.write(buf);
  }

  /**
   * Write a portion of a character array.
   *
   * @param buf character array
   * @param off Offset from which to start writing characters
   * @param len Number of characters to write
   */
  public void write(char[] buf, int off, int len) {
    for (int i=off; i < off+len; i++) {
      writtenBytes += countBytes(buf[i]);
    }
    writtenChars += len;
    super.write(buf,off,len);
  }

  /**
   * Write a string.
   */
  public void write(String s) {
    writtenBytes += countBytes(s);
    writtenChars += s.length();
    super.write(s);
  }


  /**
   * Write a portion of a string.
   *
   * @param s string to be printed
   * @param off Offset from which to start writing characters
   * @param len Number of characters to write
   */
  public void write(String s, int off, int len) {
    writtenBytes += countBytes(s.substring(off,len));
    writtenChars += len;
    super.write(s,off,len);
  }

}
