
import checkers.commitment.quals.*;
import checkers.nonnull.quals.*;

public class Lazy {
	
	@NonNull String f;
	@LazyNonNull String g;
	
	public Lazy() {
		f = "";
		// does not have to initialize g
	}
	
	void test() {
		g = "";
		test2(); // retain non-null property across method calls
		g.toLowerCase();
	}
	
	void test2() {
	}
	
	void test3() {
		//:: error: (dereference.of.nullable)
		g.toLowerCase();
	}
	
	void test4() {
		//:: error: (lazynonnull.null.assignment)
		g = null;
	}
}
