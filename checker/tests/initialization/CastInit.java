import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

public class CastInit {

  public CastInit() {
    @UnknownInitialization CastInit t1 = (@UnknownInitialization CastInit) this;
    // :: warning: (cast.unsafe)
    @Initialized CastInit t2 = (@Initialized CastInit) this;
  }
}
