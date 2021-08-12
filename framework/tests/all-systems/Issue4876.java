import java.util.function.Function;

public class Issue4876 {

  interface FFunction<T, R> extends Function<T, R> {}

  interface DInterface {}

  interface MInterface<P> {}

  interface QInterface<K extends MInterface<P>, P> {}

  FFunction<String, QInterface<?, DInterface>> r;

  Issue4876(FFunction<String, QInterface<? extends MInterface<DInterface>, DInterface>> r) {
    this.r = r;
  }
}
