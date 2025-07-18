package annotations.tests.classfile.foo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE_USE)
public @interface G {
  int fieldA();

  String fieldB();

  boolean[] fieldC();

  @D(
      fieldA = 1,
      fieldB = "value from annotation @G",
      fieldC = {3, 2})
  int fieldD();

  @D(
      fieldA = 2,
      fieldB = "value from annotation @G",
      fieldC = {3, 2})
  int fieldE();
}
