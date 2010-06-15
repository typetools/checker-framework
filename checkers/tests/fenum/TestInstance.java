import checkers.fenum.quals.*;

@SuppressWarnings("fenum.assignment.type.incompatible")
public class TestInstance {
   public final @Fenum("A") Object ACONST1 = new Object();
   public final @Fenum("A") Object ACONST2 = new Object();
   public final @Fenum("A") Object ACONST3 = new Object();

   public final @Fenum("B") Object BCONST1 = new Object();
   public final @Fenum("B") Object BCONST2 = new Object();
   public final @Fenum("B") Object BCONST3 = new Object();
}

class FenumUser {
	@Fenum("A") Object state1 = new TestInstance().ACONST1;
		
	//:: (fenum.assignment.type.incompatible)
	@Fenum("B") Object state2 = new TestInstance().ACONST1;
	
	void foo(TestInstance t) {
		//:: (fenum.assignment.type.incompatible)
		state1 = new Object();

		state1 = t.ACONST2;
		state1 = t.ACONST3;

		//:: (fenum.assignment.type.incompatible)
		state1 = t.BCONST1;
		
		// We allow this, should we forbid it?
		state1.hashCode();
		// We allow this, should we forbid it?
		t.ACONST1.hashCode();
		
		// sanity check: unqualified instantiation and call work.
		Object o = new Object();
		o.hashCode();

		// We allow this, should we forbid it?
		o = t.ACONST1;
		
		if( t.ACONST1 == t.ACONST2  ) {
		}
		
		//:: (fenum.binary.type.incompatible)
		if( t.ACONST1 == t.BCONST2  ) {
		}

	}
}