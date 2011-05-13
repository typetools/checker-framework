package java.util.regex;

import checkers.nullness.quals.*;

public final class Matcher implements MatchResult {
  protected Matcher() {}
  public @NonNull Pattern pattern() { throw new RuntimeException("skeleton method"); }
  public @NonNull MatchResult toMatchResult() { throw new RuntimeException("skeleton method"); }
  public @NonNull Matcher usePattern(@NonNull Pattern a1) { throw new RuntimeException("skeleton method"); }
  public @NonNull Matcher reset() { throw new RuntimeException("skeleton method"); }
  public @NonNull Matcher reset(CharSequence a1) { throw new RuntimeException("skeleton method"); }
  public int start() { throw new RuntimeException("skeleton method"); }
  public int start(int a1) { throw new RuntimeException("skeleton method"); }
  public int end() { throw new RuntimeException("skeleton method"); }
  public int end(int a1) { throw new RuntimeException("skeleton method"); }
  public @NonNull String group() { throw new RuntimeException("skeleton method"); }
  public @Nullable String group(int a1) { throw new RuntimeException("skeleton method"); }
  public int groupCount() { throw new RuntimeException("skeleton method"); }
  public boolean matches() { throw new RuntimeException("skeleton method"); }
  public boolean find() { throw new RuntimeException("skeleton method"); }
  public boolean find(int a1) { throw new RuntimeException("skeleton method"); }
  public boolean lookingAt() { throw new RuntimeException("skeleton method"); }
  public static @NonNull String quoteReplacement(@NonNull String a1) { throw new RuntimeException("skeleton method"); }
  public @NonNull Matcher appendReplacement(@NonNull StringBuffer a1, @NonNull String a2) { throw new RuntimeException("skeleton method"); }
  public @NonNull StringBuffer appendTail(@NonNull StringBuffer a1) { throw new RuntimeException("skeleton method"); }
  public @NonNull String replaceAll(@NonNull String a1) { throw new RuntimeException("skeleton method"); }
  public @NonNull String replaceFirst(@NonNull String a1) { throw new RuntimeException("skeleton method"); }
  public @NonNull Matcher region(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public int regionStart() { throw new RuntimeException("skeleton method"); }
  public int regionEnd() { throw new RuntimeException("skeleton method"); }
  public boolean hasTransparentBounds() { throw new RuntimeException("skeleton method"); }
  public @NonNull Matcher useTransparentBounds(boolean a1) { throw new RuntimeException("skeleton method"); }
  public boolean hasAnchoringBounds() { throw new RuntimeException("skeleton method"); }
  public @NonNull Matcher useAnchoringBounds(boolean a1) { throw new RuntimeException("skeleton method"); }
  public @NonNull String toString() { throw new RuntimeException("skeleton method"); }
  public boolean hitEnd() { throw new RuntimeException("skeleton method"); }
  public boolean requireEnd() { throw new RuntimeException("skeleton method"); }
}
