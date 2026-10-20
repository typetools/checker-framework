import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings("all") // Check for crashes.
public class Issue6421D {

  private Function<JavaConstraintGenerator, JavaTypeArbitraryGeneratorSet>
      generateJavaTypeArbitrarySet = null;
  private JavaTypeArbitraryGenerator javaTypeArbitraryGenerator = null;
  private JavaArbitraryResolver javaArbitraryResolver = null;

  void method() {
    this.generateJavaTypeArbitrarySet =
        defaultIfNull(
            this.generateJavaTypeArbitrarySet,
            () ->
                constraintGenerator ->
                    new JqwikJavaTypeArbitraryGeneratorSet(
                        this.javaTypeArbitraryGenerator, javaArbitraryResolver));
  }

  public interface JavaArbitraryResolver {}

  public interface JavaConstraintGenerator {}

  public interface JavaTypeArbitraryGeneratorSet {}

  public final class JqwikJavaTypeArbitraryGeneratorSet implements JavaTypeArbitraryGeneratorSet {
    public JqwikJavaTypeArbitraryGeneratorSet(
        JavaTypeArbitraryGenerator arbitraryGenerator, JavaArbitraryResolver arbitraryResolver) {}
  }

  public interface JavaTypeArbitraryGenerator {}

  private static <T> T defaultIfNull(T obj, Supplier<T> defaultValue) {
    return obj != null ? obj : defaultValue.get();
  }
}
