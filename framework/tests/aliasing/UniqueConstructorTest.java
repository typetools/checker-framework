import org.checkerframework.common.aliasing.qual.Unique;

class ParentUniqueClass {

    // No need for @SuppressWarnings()
    @Unique ParentUniqueClass() {
        // Doesn't raise unique.leaked error since it's a parent class (top of heirarchy)
    }
}

class ChildUniqueClass extends ParentUniqueClass {

    @Unique ChildUniqueClass() {
        // Doesn't raise unique.leaked error since its parent is @Unique
    }
}
