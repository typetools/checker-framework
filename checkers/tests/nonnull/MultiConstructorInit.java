import checkers.nonnull.quals.*;
import checkers.commitment.quals.*;
import static checkers.nonnull.util.NonNullUtils.*;

class MultiConstructorInit {
	
	String a;
	
	public MultiConstructorInit(boolean t) {
		a = "";
	}
	
	public MultiConstructorInit() {
		this(true);
	}
	
	//:: error: (fields.uninitialized)
	public MultiConstructorInit(int t) {
		new MultiConstructorInit();
	}
	
	public static void main(String[] args) {
		new MultiConstructorInit();
	}
	
}
