package beamcrash;

public class Issue6442 {

  public static class Crash<K, I>
      extends Issue6442.PTransform<PCollection<Pair<K, I>>, PCollection<Pair<K, Iterable<I>>>> {

    public PCollection<Pair<ShardedKey<K>, Iterable<I>>> expand(
        PCollection<Pair<ShardedKey<K>, I>> x) {
      return x.apply(new Crash<>());
    }
  }

  public interface PValue extends POutput, PInput {}

  public interface POutput {}

  public interface PInput {}

  public static class Pair<K2, V2> {}

  public static class PCollection<T> extends PValueBase implements PValue {

    public <O1 extends POutput> O1 apply(PTransform<? super PCollection<T>, O1> t) {
      throw new RuntimeException();
    }
  }

  public abstract static class PValueBase implements PValue {}

  public abstract static class PTransform<I2 extends PInput, O2 extends POutput> {}

  public static class ShardedKey<K4> {}
}
