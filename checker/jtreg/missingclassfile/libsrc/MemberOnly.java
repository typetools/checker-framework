package lib;

/** Mentions {@link Missing} only in member signatures, never in its supertype. */
public class MemberOnly {
  public Missing field;

  public Missing get() {
    return null;
  }

  public void set(Missing m) {}
}
