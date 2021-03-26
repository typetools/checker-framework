import org.checkerframework.checker.guieffect.qual.PolyUI;
import org.checkerframework.checker.guieffect.qual.PolyUIEffect;
import org.checkerframework.checker.guieffect.qual.PolyUIType;

@PolyUIType
@PolyUI public interface IGenericTask {
  @PolyUIEffect
  public void doGenericStuff();
}
