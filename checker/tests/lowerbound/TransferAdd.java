import org.checkerframework.checker.lowerbound.qual.*;

public class TransferAdd{

    void test() {

	// adding zero and one
	
	int a = -1;

	@NonNegative int b = a + 1;
	@NonNegative int c = 1 + a;

	@NegativeOnePlus int d = a + 0;
	@NegativeOnePlus int e = 0 + a;
	
	//:: error: (assignment.type.incompatible)
	@Positive int f = a + 1;

	@NonNegative int g = b + 0;

	@Positive int h = b + 1;

	@Positive int i = h + 1;
	@Positive int j = h + 0;
    }

}
