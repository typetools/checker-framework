import org.checkerframework.common.aliasing.qual.Unique;

public class UniqueConstructorTest {

  @Unique UniqueConstructorTest() {
    // Does not raise unique.leaked error since the parent constructor (Object) is unique
  }

  class ParentClass extends UniqueConstructorTest {

    ParentClass() {
      // Does not raise unique.leaked error since the parent constructor is unique
    }
  }

  class ChildUniqueClass extends ParentClass {

    // ::error: (unique.leaked)
    @Unique ChildUniqueClass() {
      // Raises unique.leaked error since the parent constructor is not unique
    }
  }
}
