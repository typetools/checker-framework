import checkers.fenum.quals.*;

public class TestPrimitive {
	public final @FenumDecl("A") int ACONST1 = 1;
	public final @FenumDecl("A") int ACONST2 = 2;
	public final @FenumDecl("A") int ACONST3 = 3;

	public final @FenumDecl("B") int BCONST1 = 4;
	public final @FenumDecl("B") int BCONST2 = 5;
	public final @FenumDecl("B") int BCONST3 = 6;
}

class FenumUser {
	@Fenum("A") int state1 = new TestPrimitive().ACONST1;
		
	//:: (assignment.type.incompatible)
	@Fenum("B") int state2 = new TestPrimitive().ACONST1;
	
	void foo(TestPrimitive t) {
		//:: (assignment.type.incompatible)
		state1 = 4;
		
		state1 = t.ACONST2;
		state1 = t.ACONST3;
		
		//:: (assignment.type.incompatible)
		state1 = t.BCONST1;
		
		//:: (assignment.type.incompatible)
		int x = t.ACONST1;
		
		if( t.ACONST1 < t.ACONST2  ) {
			// ok
		}

		//:: (type.incompatible)
		if( t.ACONST1 < t.BCONST2  ) {
		}
		//:: (type.incompatible)
		if( t.ACONST1 == t.BCONST2  ) {
		}

		//:: (type.incompatible)
		if( t.ACONST1 < 5 ) {
		}
	}
}