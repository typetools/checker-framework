import org.checkerframework.checker.nullness.qual.Nullable;

public class NullnessBeamCrash3<T> {

  @SuppressWarnings({"unchecked", "rawtypes"})
  private CoderOrFailure<T> inferCoderOrFail(PInput input, PTransform<?, ?> transform) {
    return new CoderOrFailure<>(((PTransform) transform).getDefaultOutputCoder(input, this), null);
  }

  private static class CoderOrFailure<T> {

    private final @Nullable Coder<T> coder;
    private final @Nullable String failure;

    public CoderOrFailure(@Nullable Coder<T> coder, @Nullable String failure) {
      this.coder = coder;
      this.failure = failure;
    }
  }

  public interface PInput {}

  public abstract static class Coder<T> {}

  public abstract static class PTransform<InputT extends PInput, OutputT extends POutput> {

    public <T> Coder<T> getDefaultOutputCoder(InputT input, NullnessBeamCrash3<T> output) {
      throw new RuntimeException();
    }
  }

  public interface POutput {}
}
