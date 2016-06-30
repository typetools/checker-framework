import org.checkerframework.checker.lowerbound.qual.*;

public class TransferSub {

    void test() {
	// zero, one, and two
	int a = 1;

	@NonNegative int b = a - 1;
	//:: error: (assignment.type.incompatible)
	@Positive int c = a - 1;
	@NegativeOnePlus int d = a - 2;

	//:: error: (assignment.type.incompatible)
	@NonNegative int e = a -2;

	@NegativeOnePlus int f = b - 1;
	//:: error: (assignment.type.incompatible)
	@NonNegative int g = b - 1;

	//:: error: (assignment.type.incompatible)
	@NegativeOnePlus int h = f - 1;

	@NegativeOnePlus int i = f - 0;
	@NonNegative int j = b - 0;
	@Positive int k = a - 0;

	//:: error: (assignment.type.incompatible)
	@Positive int l = j - 0;
	//:: error: (assignment.type.incompatible)
	@NonNegative int m = i - 0;

	//:: error: (assignment.type.incompatible)
	@Positive int n = a - k;
	//:: error: (assignment.type.incompatible)
	@NonNegative int o = b - j;
	//:: error: (assignment.type.incompatible)
	@NegativeOnePlus int p = i - d;
    }
}
