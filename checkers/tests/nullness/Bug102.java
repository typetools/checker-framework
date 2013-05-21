// Test case for Issue 102
public final class Bug102 {
    class C<T extends /*@checkers.nullness.quals.Nullable*/ Object> { }

	void bug1() {
		C<String> c = new C<String>();
		m(c);
		m(c); // note: the bug disapear if calling m only once
	}

	void bug2() {
		C<String> c = new C<String>();
		m(c);
	}

	</*@checkers.nullness.quals.PolyNull*/ S> void m(final C</*@checkers.nullness.quals.PolyNull*/ String> a) {}
}