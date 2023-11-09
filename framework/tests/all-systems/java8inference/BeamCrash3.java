public class BeamCrash3<T> {

  @SuppressWarnings({"unchecked", "rawtypes"})
  private CoderOrFailure<T> inferCoderOrFail(PInput input, PTransform<?, ?> transform) {
    return new CoderOrFailure<>(((PTransform) transform).getDefaultOutputCoder(input, this), null);
  }

  private static class CoderOrFailure<T> {
    public CoderOrFailure(Coder<T> coder, String failure) {}
  }

  public interface PInput {}

  public abstract static class Coder<T> {}

  public abstract static class PTransform<InputT extends PInput, OutputT extends POutput> {

    public <T> Coder<T> getDefaultOutputCoder(InputT input, BeamCrash3<T> output) {
      throw new RuntimeException();
    }
  }

  public interface POutput {}
}
