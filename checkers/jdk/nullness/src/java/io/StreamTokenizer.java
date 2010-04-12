package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class StreamTokenizer{
  public int ttype;
  public final static int TT_EOF = -1;
  public final static int TT_EOL = 10;
  public final static int TT_NUMBER = -2;
  public final static int TT_WORD = -3;
  public @Nullable String sval;
  public double nval;
  public StreamTokenizer(InputStream a1) { throw new RuntimeException("skeleton method"); }
  public StreamTokenizer(Reader a1) { throw new RuntimeException("skeleton method"); }
  public void resetSyntax() { throw new RuntimeException("skeleton method"); }
  public void wordChars(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public void whitespaceChars(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public void ordinaryChars(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public void ordinaryChar(int a1) { throw new RuntimeException("skeleton method"); }
  public void commentChar(int a1) { throw new RuntimeException("skeleton method"); }
  public void quoteChar(int a1) { throw new RuntimeException("skeleton method"); }
  public void parseNumbers() { throw new RuntimeException("skeleton method"); }
  public void eolIsSignificant(boolean a1) { throw new RuntimeException("skeleton method"); }
  public void slashStarComments(boolean a1) { throw new RuntimeException("skeleton method"); }
  public void slashSlashComments(boolean a1) { throw new RuntimeException("skeleton method"); }
  public void lowerCaseMode(boolean a1) { throw new RuntimeException("skeleton method"); }
  public int nextToken() throws IOException { throw new RuntimeException("skeleton method"); }
  public void pushBack() { throw new RuntimeException("skeleton method"); }
  public int lineno() { throw new RuntimeException("skeleton method"); }
  public String toString() { throw new RuntimeException("skeleton method"); }
}
