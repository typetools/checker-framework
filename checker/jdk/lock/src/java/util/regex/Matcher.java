package java.util.regex;

import org.checkerframework.checker.lock.qual.*;

public final class Matcher implements MatchResult {
  protected Matcher() {}
  public Pattern pattern() { throw new RuntimeException("skeleton method"); }
  public MatchResult toMatchResult() { throw new RuntimeException("skeleton method"); }
  public Matcher usePattern(Pattern a1) { throw new RuntimeException("skeleton method"); }
  public Matcher reset() { throw new RuntimeException("skeleton method"); }
  public Matcher reset(CharSequence a1) { throw new RuntimeException("skeleton method"); }

  public int start() { throw new RuntimeException("skeleton method"); }

  public int start(int a1) { throw new RuntimeException("skeleton method"); }

  public int end() { throw new RuntimeException("skeleton method"); }

  public int end(int a1) { throw new RuntimeException("skeleton method"); }

  public String group() { throw new RuntimeException("skeleton method"); }

  public String group(int a1) { throw new RuntimeException("skeleton method"); }

  public int groupCount() { throw new RuntimeException("skeleton method"); }
  public boolean matches() { throw new RuntimeException("skeleton method"); }
  public boolean find() { throw new RuntimeException("skeleton method"); }
  public boolean find(int a1) { throw new RuntimeException("skeleton method"); }
  public boolean lookingAt() { throw new RuntimeException("skeleton method"); }
  public static String quoteReplacement(String a1) { throw new RuntimeException("skeleton method"); }
  public Matcher appendReplacement(StringBuffer a1, String a2) { throw new RuntimeException("skeleton method"); }
  public StringBuffer appendTail(StringBuffer a1) { throw new RuntimeException("skeleton method"); }
  public String replaceAll(String a1) { throw new RuntimeException("skeleton method"); }
  public String replaceFirst(String a1) { throw new RuntimeException("skeleton method"); }
  public Matcher region(int a1, int a2) { throw new RuntimeException("skeleton method"); }

  public int regionStart() { throw new RuntimeException("skeleton method"); }

  public int regionEnd() { throw new RuntimeException("skeleton method"); }

  public boolean hasTransparentBounds() { throw new RuntimeException("skeleton method"); }
  public Matcher useTransparentBounds(boolean a1) { throw new RuntimeException("skeleton method"); }

  public boolean hasAnchoringBounds() { throw new RuntimeException("skeleton method"); }
  public Matcher useAnchoringBounds(boolean a1) { throw new RuntimeException("skeleton method"); }
   public String toString(@GuardSatisfied Matcher this) { throw new RuntimeException("skeleton method"); }

  public boolean hitEnd() { throw new RuntimeException("skeleton method"); }

  public boolean requireEnd() { throw new RuntimeException("skeleton method"); }
}
