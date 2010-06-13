import checkers.fenum.quals.*;

public class TestStatic {
	public static final @FenumDecl("A") int ACONST1 = 1;
	public static final @FenumDecl("A") int ACONST2 = 2;
	public static final @FenumDecl("A") int ACONST3 = 3;

	public static final @FenumDecl("B") int BCONST1 = 4;
	public static final @FenumDecl("B") int BCONST2 = 5;
	public static final @FenumDecl("B") int BCONST3 = 6;
}

class FenumUser {
	@Fenum("A")	int state1 = TestStatic.ACONST1;

	//:: (assignment.type.incompatible)
	@Fenum("B")	int state2 = TestStatic.ACONST1;

	void foo() {
		//:: (assignment.type.incompatible)
		state1 = 4;

		state1 = TestStatic.ACONST2;
		state1 = TestStatic.ACONST3;

		//:: (assignment.type.incompatible)
		state1 = TestStatic.BCONST1;
	}
}