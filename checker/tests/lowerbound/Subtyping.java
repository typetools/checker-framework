import org.checkerframework.checker.lowerbound.qual.*;

class Subtyping{

    void foo() {

	@NegativeOnePlus int i = -1;

	@Unknown int j = i;

	int k = -4;

	// not this one though
	//:: error: (assignment.type.incompatible)
	@NegativeOnePlus int l = k;

	@NonNegative int n = 0;

	@Positive int a = 1;

	// check that everything is aboveboard
	j = a;
	j = n;
	l = n;
	n = a;
	
	// error cases
	
	//:: error: (assignment.type.incompatible)
	@NonNegative int p = i;
	//:: error: (assignment.type.incompatible)
	@Positive int b = i;

	//:: error: (assignment.type.incompatible)
	@NonNegative int r = k;
	//:: error: (assignment.type.incompatible)
	@Positive int c = k;

	//:: error: (assignment.type.incompatible)
	@Positive int d = r;
	
	
	
    }
}
