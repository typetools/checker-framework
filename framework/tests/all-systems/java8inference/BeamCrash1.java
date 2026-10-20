@SuppressWarnings("unchecked")
public abstract class BeamCrash1<DestinationT, UserT> {

  abstract Builder<DestinationT, UserT> toBuilder();

  public <OutputT> void via(
      Contextful<Contextful.Fn<UserT, OutputT>> outputFn,
      Contextful<Contextful.Fn<DestinationT, Sink<OutputT>>> sinkFn) {
    toBuilder().setSinkFn((Contextful) sinkFn).setOutputFn(outputFn);
  }

  abstract static class Builder<DestinationT, UserT> {

    abstract Builder<DestinationT, UserT> setSinkFn(
        Contextful<Contextful.Fn<DestinationT, Sink<?>>> sink);

    abstract Builder<DestinationT, UserT> setOutputFn(Contextful<Contextful.Fn<UserT, ?>> outputFn);

    abstract Write<DestinationT, UserT> build();
  }

  public interface Sink<ElementT> {}

  public abstract static class Write<DestinationT, UserT> {}

  public static final class Contextful<ClosureT> {

    public interface Fn<InputT, OutputT> {}
  }
}
