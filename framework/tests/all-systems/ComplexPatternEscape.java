// A test that complicated escape patterns don't mess with a checker.
// Also useful for testing WPI's ajava mode, which needs to preserve the
// escapes.

public class ComplexPatternEscape {

  private static final String DOT_DELIMITED_IDS = "";

  // From
  // https://github.com/randoop/randoop/blob/ffed1540721212adc55da179f1ae3b3df582d0d5/agent/replacecall/src/main/java/randoop/instrument/ReplacementFileReader.java#L58
  private static final String SIGNATURE_STRING =
      DOT_DELIMITED_IDS + "(?:\\.<init>)?" + "\\([^)]*\\)";
}
