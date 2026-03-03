package org.checkerframework.afu.scenelib.io;

// Not package-private because it is used from the Scene Library.
// But not intended for widespread use.

/**
 * Thrown when index file or javap parsing fails.
 *
 * <p>Because of the way the parser is implemented, sometimes the error message isn't very good; in
 * particular, it sometimes says "expected A, B or C" when there are legal tokens other than A, B,
 * and C.
 */
@SuppressWarnings("serial")
public final class ParseException extends Exception {

  public ParseException() {
    super();
  }

  public ParseException(String message) {
    super(message);
  }

  public ParseException(String message, Throwable cause) {
    super(message, cause);
  }

  public ParseException(Throwable cause) {
    super(cause);
  }
}
