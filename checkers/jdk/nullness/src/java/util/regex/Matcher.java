package java.util.regex;

import checkers.nullness.quals.*;

public final class Matcher implements java.util.regex.MatchResult {
  protected Matcher() {}
  public @NonNull java.util.regex.Pattern pattern() { throw new RuntimeException("skeleton method"); }
  public @NonNull java.util.regex.MatchResult toMatchResult() { throw new RuntimeException("skeleton method"); }
  public @NonNull java.util.regex.Matcher usePattern(@NonNull java.util.regex.Pattern a1) { throw new RuntimeException("skeleton method"); }
  public @NonNull java.util.regex.Matcher reset() { throw new RuntimeException("skeleton method"); }
  public @NonNull java.util.regex.Matcher reset(java.lang.CharSequence a1) { throw new RuntimeException("skeleton method"); }
  public int start() { throw new RuntimeException("skeleton method"); }
  public int start(int a1) { throw new RuntimeException("skeleton method"); }
  public int end() { throw new RuntimeException("skeleton method"); }
  public int end(int a1) { throw new RuntimeException("skeleton method"); }
  public @NonNull java.lang.String group() { throw new RuntimeException("skeleton method"); }
  public @Nullable java.lang.String group(int a1) { throw new RuntimeException("skeleton method"); }
  public int groupCount() { throw new RuntimeException("skeleton method"); }
  public boolean matches() { throw new RuntimeException("skeleton method"); }
  public boolean find() { throw new RuntimeException("skeleton method"); }
  public boolean find(int a1) { throw new RuntimeException("skeleton method"); }
  public boolean lookingAt() { throw new RuntimeException("skeleton method"); }
  public static @NonNull java.lang.String quoteReplacement(@NonNull java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public @NonNull java.util.regex.Matcher appendReplacement(@NonNull java.lang.StringBuffer a1, @NonNull java.lang.String a2) { throw new RuntimeException("skeleton method"); }
  public @NonNull java.lang.StringBuffer appendTail(@NonNull java.lang.StringBuffer a1) { throw new RuntimeException("skeleton method"); }
  public @NonNull java.lang.String replaceAll(@NonNull java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public @NonNull java.lang.String replaceFirst(@NonNull java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public @NonNull java.util.regex.Matcher region(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public int regionStart() { throw new RuntimeException("skeleton method"); }
  public int regionEnd() { throw new RuntimeException("skeleton method"); }
  public boolean hasTransparentBounds() { throw new RuntimeException("skeleton method"); }
  public @NonNull java.util.regex.Matcher useTransparentBounds(boolean a1) { throw new RuntimeException("skeleton method"); }
  public boolean hasAnchoringBounds() { throw new RuntimeException("skeleton method"); }
  public @NonNull java.util.regex.Matcher useAnchoringBounds(boolean a1) { throw new RuntimeException("skeleton method"); }
  public @NonNull java.lang.String toString() { throw new RuntimeException("skeleton method"); }
  public boolean hitEnd() { throw new RuntimeException("skeleton method"); }
  public boolean requireEnd() { throw new RuntimeException("skeleton method"); }
}
