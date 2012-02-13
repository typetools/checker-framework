package utilMDE;

import java.util.*;
import java.io.*;

import checkers.quals.Interned;

// NEEDS DOCUMENTATION!
// (Probably mostly Javadoc "see" directives, possibly with first line of relevant method doc.)

/**
 * Like StringBuilder, but adds a delimiter between each pair of strings
 * that are insered into the Stringbuilder.  This can simplify the logic of
 * programs and also avoid errors.
 *
 * Does not extend StringBuilder because that would probably break, due to
 * the possibility of calling the wrong version of append.  Also, I don't
 * (yet) want to override all the methods; this simpler version seems
 * sufficient for the time being.
 **/
public class StringBuilderDelimited implements Appendable, CharSequence {

  private StringBuilder delegate = new StringBuilder();
  private boolean empty = true;
  private final String delimiter;

  public StringBuilderDelimited(String delimiter) {
    this.delimiter = delimiter;
  }

  private void appendDelimiter() {
    if (empty) {
      empty = false;
    } else {
      delegate.append(delimiter);
    }
  }

  public StringBuilderDelimited append(String str) {
    appendDelimiter();
    delegate.append(str);
    return this;
  }

  public StringBuilderDelimited append(Object o) {
    appendDelimiter();
    delegate.append(o.toString());
    return this;
  }

  public StringBuilderDelimited append(char c) {
    appendDelimiter();
    delegate.append(c);
    return this;
  }

  public StringBuilderDelimited append(CharSequence csq) {
    appendDelimiter();
    delegate.append(csq);
    return this;
  }

  public StringBuilderDelimited append(CharSequence csq, int start, int end) {
    appendDelimiter();
    delegate.append(csq, start, end);
    return this;
  }

  public char charAt(int index) {
    return delegate.charAt(index);
  }

  public int length() {
    return delegate.length();
  }

  public CharSequence subSequence(int start, int end) {
    return delegate.subSequence(start, end);
  }

  public String toString() {
    return delegate.toString();
  }

}
