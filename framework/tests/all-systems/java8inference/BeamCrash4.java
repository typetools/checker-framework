@SuppressWarnings("unchecked")
public class BeamCrash4 {
  public abstract static class Write<DestinationT, UserT> {
    abstract static class Builder<DestinationT, UserT> {
      abstract Builder<DestinationT, UserT> setSinkFn(
          Contextful<Contextful.Fn<DestinationT, Sink<?>>> sink);

      abstract Builder<DestinationT, UserT> setOutputFn(
          Contextful<Contextful.Fn<UserT, ?>> outputFn);

      abstract Write<DestinationT, UserT> build();
    }

    abstract Builder<DestinationT, UserT> toBuilder();

    public Write<DestinationT, UserT> via(
        Contextful<Contextful.Fn<DestinationT, Sink<UserT>>> sinkFn) {
      return toBuilder()
          .setSinkFn((Contextful) sinkFn)
          .setOutputFn(fn(BeamCrash4.<UserT>identity()))
          .build();
    }
  }

  public interface SerializableFunction<InputT, OutputT> {

    OutputT apply(InputT input);
  }

  public static <T> SerializableFunction<T, T> identity() {
    return new Identity<>();
  }

  private static class Identity<T> implements SerializableFunction<T, T> {
    @Override
    public T apply(T input) {
      return input;
    }
  }

  public interface Sink<ElementT> {}

  public static final class Contextful<ClosureT> {

    public interface Fn<InputT, OutputT> {}
  }

  public static <InputT, OutputT> Contextful<Contextful.Fn<InputT, OutputT>> fn(
      final SerializableFunction<InputT, OutputT> fn) {
    throw new RuntimeException();
  }
}
