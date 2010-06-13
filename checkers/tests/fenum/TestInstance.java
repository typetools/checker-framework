import checkers.fenum.quals.*;

public class TestInstance {
   public final @FenumDecl("A") Object ACONST1 = new Object();
   public final @FenumDecl("A") Object ACONST2 = new Object();
   public final @FenumDecl("A") Object ACONST3 = new Object();

   public final @FenumDecl("B") Object BCONST1 = new Object();
   public final @FenumDecl("B") Object BCONST2 = new Object();
   public final @FenumDecl("B") Object BCONST3 = new Object();
}

class FenumUser {
	@Fenum("A") Object state1 = new TestInstance().ACONST1;
		
	//:: (assignment.type.incompatible)
	@Fenum("B") Object state2 = new TestInstance().ACONST1;
	
	void foo(TestInstance t) {
		//:: (assignment.type.incompatible)
		state1 = new Object();

		state1 = t.ACONST2;
		state1 = t.ACONST3;

		//:: (assignment.type.incompatible)
		state1 = t.BCONST1;
		
		//:: (method.invocation.invalid)
		state1.hashCode();
		//:: (method.invocation.invalid)
		t.ACONST1.hashCode();
		
		// sanity check: unqualified instantiation and call work.
		Object o = new Object();
		o.hashCode();

		if( t.ACONST1 == t.ACONST2  ) {
		}
		
		//:: (type.incompatible)
		if( t.ACONST1 == t.BCONST2  ) {
		}

	}
}