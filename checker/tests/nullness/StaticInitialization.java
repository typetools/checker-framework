// @skip-test How do you write a localized @SuppressWarnings rather than one that covers the whole
// class?

public class StaticInitialization {

  @SuppressWarnings({"nullness", "initialization.fields.uninitialized"})
  public static Object dontWarnAboutThisField;

  static {
  }
}
