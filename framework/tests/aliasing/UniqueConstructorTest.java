import org.checkerframework.common.aliasing.qual.Unique;

public class UniqueConstructorTest {

    @Unique UniqueConstructorTest() {
        // No need for @SuppressWarnings()
        // Doesn't raise unique.leaked error since it's a top parent class (@Unique Object subclass
        // by default)
    }

    class ParentClass extends UniqueConstructorTest {

        ParentClass() {
            // Doesn't raise unique.leaked error since it's a non-unique class with a unique parent
        }
    }

    class ChildUniqueClass extends ParentClass {

        // ::error: (unique.leaked)
        @Unique ChildUniqueClass() {
            // Raises unique.leaked error since its parent is not unique
        }
    }
}
