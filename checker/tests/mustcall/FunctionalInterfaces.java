// Test that the correct type is assigned to instantiations of functional
// interfaces.
// https://github.com/typetools/checker-framework/issues/6823

import java.io.Closeable;
import org.checkerframework.checker.mustcall.qual.*;

public abstract class FunctionalInterfaces {

  @FunctionalInterface
  public interface Actor extends Closeable {
    void act();

    @Override
    default void close() {}
  }

  public static class ActorImpl implements Actor {
    @Override
    public void act() {}
  }

  public abstract void run(@MustCall({}) Actor a);

  public static void method() {}

  public void normalConstruction() {

    // :: error: (assignment)
    @MustCall({}) Actor a = new ActorImpl();
  }

  public void inlineClass() {

    class ActorImplInline implements Actor {
      @Override
      public void act() {}
    }

    // :: error: (assignment)
    @MustCall({}) Actor a = new ActorImplInline();
  }

  public void anonymousClass() {

    @MustCall({}) Actor a =
        // :: error: (assignment)
        new Actor() {
          public void act() {}
        };
  }
}
