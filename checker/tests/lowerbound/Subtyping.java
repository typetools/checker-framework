import org.checkerframework.checker.lowerbound.qual.*;

class Subtyping{

    void foo(){
	// this error should go away when we add the intro rules
	//:: error: (assignment.type.incompatible)
	@NegativeOnePlus int i = 0;

	@Unknown int j = i;

	int k = -4;

	// not this one though
	//:: error: (assignment.type.incompatible)
	@NegativeOnePlus int l = k;
    }
}
