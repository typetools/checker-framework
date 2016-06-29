import org.checkerframework.checker.lowerbound.qual.*;

public class IntroRules{

    void test() {
	@Positive int a = 10;
	@NonNegative int b = 9;
	@NegativeOnePlus int c = 8;
	@Unknown int d = 7;

	//:: error: (assignment.type.incompatible)
	@Positive int e = 0;
	//:: error: (assignment.type.incompatible)
	@Positive int f = -1;
	//:: error: (assignment.type.incompatible)
	@Positive int g = -6;

	@NonNegative int h = 0;
	@NegativeOnePlus int i = 0;
	@Unknown int j = 0;
	//:: error: (assignment.type.incompatible)
	@NonNegative int k = -1;
	//:: error: (assignment.type.incompatible)
	@NonNegative int l = -4;

	@NegativeOnePlus int m = -1;
	@Unknown int n = -1;
	//:: error: (assignment.type.incompatible)
	@NegativeOnePlus int o = -9;
    }
}
