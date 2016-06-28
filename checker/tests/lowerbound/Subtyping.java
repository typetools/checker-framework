import org.checkerframework.checker.lowerbound.qual.*;

class Subtyping{

    void foo() {
	// this error should go away when we add the intro rules
	//:: error: (assignment.type.incompatible)
	@NegativeOnePlus int i = 0;

	@Unknown int j = i;

	int k = -4;

	// not this one though
	//:: error: (assignment.type.incompatible)
	@NegativeOnePlus int l = k;

	// this error should go away when we add the intro rules
	//:: error: (assignment.type.incompatible)
	@NonNegative int n = 0;

	// this error should go away when we add the intro rules
	//:: error: (assignment.type.incompatible)
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
