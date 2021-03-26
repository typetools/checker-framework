import org.checkerframework.checker.interning.qual.Interned;

public final @Interned class MultiAnnotations {

  private MultiAnnotations() {}

  public static final MultiAnnotations NO_CHANGE = new MultiAnnotations();

  MultiAnnotations foo() {
    return MultiAnnotations.NO_CHANGE;
  }
}
