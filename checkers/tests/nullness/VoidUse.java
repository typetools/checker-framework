import checkers.nullness.quals.*;
public class VoidUse {

  private Class<?> main_class = Void.TYPE;

  public Void voidReturn(Void p) {
      voidReturn(null);
      return null;
  }

}
