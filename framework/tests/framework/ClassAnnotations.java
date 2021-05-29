import org.checkerframework.framework.testchecker.util.*;

// ::warning: (inconsistent.constructor.type) :: error: (super.invocation)
public @Odd class ClassAnnotations {

  ClassAnnotations c;

  public void test() {
    @Odd ClassAnnotations d = c;
  }
}
