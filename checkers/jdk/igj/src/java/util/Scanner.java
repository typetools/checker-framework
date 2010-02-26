package java.util;

import checkers.igj.quals.*;

@I
public final class Scanner implements @Immutable Iterator<String> {
  public Scanner(Readable a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public Scanner(java.io.InputStream a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public Scanner(java.io.InputStream a1, String a2) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public Scanner(java.io.File a1) @AssignsFields throws java.io.FileNotFoundException { throw new RuntimeException("skeleton method"); }
  public Scanner(java.io.File a1, String a2) @AssignsFields throws java.io.FileNotFoundException { throw new RuntimeException("skeleton method"); }
  public Scanner(String a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public Scanner(java.nio.channels.ReadableByteChannel a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public Scanner(java.nio.channels.ReadableByteChannel a1, String a2) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public void close() @Mutable { throw new RuntimeException("skeleton method"); }
  public java.io.IOException ioException() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public java.util.regex.Pattern delimiter() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I Scanner useDelimiter(java.util.regex.Pattern a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public @I Scanner useDelimiter(String a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public Locale locale() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I Scanner useLocale(Locale a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int radix() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I Scanner useRadix(int a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public java.util.regex.MatchResult match() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public String toString() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean hasNext() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public String next() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public void remove() @Mutable { throw new RuntimeException("skeleton method"); }
  public boolean hasNext(String a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public String next(String a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean hasNext(java.util.regex.Pattern a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public String next(java.util.regex.Pattern a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean hasNextLine() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public String nextLine() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public String findInLine(String a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public String findInLine(java.util.regex.Pattern a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public String findWithinHorizon(String a1, int a2) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public String findWithinHorizon(java.util.regex.Pattern a1, int a2) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I Scanner skip(java.util.regex.Pattern a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public @I Scanner skip(String a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public boolean hasNextBoolean() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean nextBoolean() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean hasNextByte() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean hasNextByte(int a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public byte nextByte() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public byte nextByte(int a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean hasNextShort() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean hasNextShort(int a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public short nextShort() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public short nextShort(int a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean hasNextInt() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean hasNextInt(int a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int nextInt() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int nextInt(int a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean hasNextLong() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean hasNextLong(int a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public long nextLong() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public long nextLong(int a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean hasNextFloat() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public float nextFloat() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean hasNextDouble() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public double nextDouble() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean hasNextBigInteger() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean hasNextBigInteger(int a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public java.math.BigInteger nextBigInteger() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public java.math.BigInteger nextBigInteger(int a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean hasNextBigDecimal() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public java.math.BigDecimal nextBigDecimal() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I Scanner reset() @Mutable { throw new RuntimeException("skeleton method"); }
}
