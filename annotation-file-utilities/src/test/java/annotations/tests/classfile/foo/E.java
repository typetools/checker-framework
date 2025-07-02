package annotations.tests.classfile.foo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE_USE)
public @interface E {
  int fieldA();

  @A String fieldB();
}
