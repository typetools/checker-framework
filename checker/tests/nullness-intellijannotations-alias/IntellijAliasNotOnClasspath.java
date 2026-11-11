// Tests that an annotations.xml file can use an alias of a Nullness Checker qualifier even if the
// alias is not on the classpath.  The file java/lang/annotations.xml annotates the return type of
// System.getenv(String) as @androidx.annotation.NonNull, and the androidx annotations are not on
// the classpath.

public class IntellijAliasNotOnClasspath {

  // The annotated JDK's return type for System.getenv(String) is @Nullable, but the
  // annotations.xml file overrides it.
  int getenvLength() {
    return System.getenv("HOME").length();
  }
}
