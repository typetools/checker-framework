import checkers.nullness.quals.*;

class AnnotatedGenerics3 {
    class Cell<T extends @Nullable Object> {
	T f;

	void setField(@Nullable T p) {
	    //:: error: (assignment.type.incompatible)
	    this.f = p;
	}
    }
}