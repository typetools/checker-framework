import org.checkerframework.checker.guieffect.qual.SafeEffect;

public interface IFooSafe {
  @SafeEffect
  public void foo();
}
