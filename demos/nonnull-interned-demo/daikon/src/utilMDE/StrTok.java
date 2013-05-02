package utilMDE;

import java.util.*;
import java.io.*;

import checkers.quals.Interned;

/**
 * Provides a somewhat simpler interface for tokenizing strings than
 * does StreamTokenizer.  All tokenizing is done by StreamTokenizer. <p>
 *
 * The major difference from StreamTokenizer is that all tokens are
 * returned as strings.  EOF returns a null, EOL returns an empty string.
 * Delimiters are returned as one character strings.  Words and numbers
 * are returned as strings.  Quoted strings are also returned as strings
 * including their quote characters (so they can easily be differentiated
 * from words and numbers).
 *
 * Other differences are: <ul>
 *  <li> Automatic setup for tokenizing strings
 *  <li> Underscores are included in identifiers (words)
 *  <li> I/O errors (which should be impossible when tokenizing strings) are
 *       converted to RuntimeExceptions so that every call doesn't have to
 *       be included in a try block
 *  <li> Convenience functions isWord(), isQString(), and need()
 *  <li> Returned string tokens are interned for easier comparisons.
 * </ul>
 */
public class StrTok {

  Reader reader;
  public StreamTokenizer stok;
  Error err = new Error();

  /**
   * Creates a tokenizer for the specified string.
   */
  public StrTok (String s) {

    reader = new StringReader (s);
    stok = new StreamTokenizer (reader);
    stok.wordChars ('_', '_');
  }

  /**
   * Default Class for error handling.  Throws a RuntimeException when an
   * error occurs.
   *
   * @see StrTok#set_error_handler(StrTok.Error)
   */
  public static class Error {

    /**
     * Called when an unexpected token is found (see {@link #need(String)})
     */
    public void tok_error (String s) {
      throw new RuntimeException ("StrTok error: " + s);
    }
  }

  /**
   * Returns the next token as a string.  EOF returns a null, EOL
   * returns an empty string.  Delimiters are returned as one character
   * strings.  Quoted strings and words are returned as strings.
   */
  public /*@Interned*/ String nextToken() {

    // Get the next token.  Turn IO exceptions into runtime exceptions
    // so that callers don't have to catch them.
    int ttype;
    try {
      ttype = stok.nextToken();
    } catch (Exception e) {
      throw new RuntimeException ("StreamTokenizer exception: ", e);
    }

    return (token());
  }

  /**
   * Returns the current token.
   * @see #nextToken()
   */
  public /*@Interned*/ String token() {

    int ttype = stok.ttype;

    // Null indicates eof
    if (ttype == StreamTokenizer.TT_EOF)
      return (null);

    // Return end of line as an empty string
    if (ttype == StreamTokenizer.TT_EOL)
      return ("");

    // Return identifiers (words) and quoted strings.  Quoted strings
    // include their quote characters (for recognition)
    if (stok.sval != null) {
      if (ttype > 0) {
        String s = ((char) ttype) + stok.sval + ((char) ttype);
        return (s.intern());
      }
      return (stok.sval.intern());
    }

    // Other tokens are delimiters
    if (ttype > 0) {
      String s = "" + (char)ttype;
      return (s.intern());
    }

    throw new RuntimeException ("Unexpected return " + ttype +
                                " from StreamTokenizer");
  }

  /**
   * Specifies the single line comment character.
   * @see StreamTokenizer#commentChar(int)
   */
  public void commentChar (int ch) {
    stok.commentChar (ch);
  }

  /**
   * Specifies that matching pairs of this character delimit string constants.
   * @see StreamTokenizer#quoteChar(int)
   */
  public void quoteChar (int ch) {
    stok.quoteChar (ch);
  }

  /**
   * Returns the type of the current token.
   * @see StreamTokenizer#ttype
   */
  public int ttype() {
    return stok.ttype;
  }

  /** Returns true if the current token is a word (identifier) */
  public boolean isWord() {
    return (stok.ttype == StreamTokenizer.TT_WORD);
  }

  /** Returns true if the current token is a quoted string */
  public boolean isQString() {
    return ((stok.sval != null) && (stok.ttype > 0));
  }

  /**
   * Sets the error handler.  The default error handler will throw a
   * runtime exception on errors.
   * @see Error
   */
  public void set_error_handler (Error err) {
    this.err = err;
  }

  /**
   * Reads the next token and checks that it matches tok.  If it does
   * not match, calls the current error handling routine (see {@link
   * #set_error_handler(StrTok.Error) set_error_handler()}).
   * If it does match, just returns.
   */
  public void need (String tok) {

    String t = nextToken();
    if (tok.equals(t))
      return;

    err.tok_error (Fmt.spf ("Token %s found where %s expected", t, tok));
  }

}
