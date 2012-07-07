
import checkers.nonnull.quals.*;

public class FlowInitialization {
	
	@NonNull String f;
	@Nullable String g;
	
	//:: error: (fields.uninitialized)
	public FlowInitialization() {
		
	}
	
	public FlowInitialization(long l) {
		g = "";
		f = g;
	}
	
	//:: error: (fields.uninitialized)
	public FlowInitialization(boolean b) {
		if (b) {
			f = "";
		}
	}
	
	//:: error: (fields.uninitialized)
	public FlowInitialization(int i) {
		if (i == 0) {
			throw new RuntimeException();
		}
	}
	
	public FlowInitialization(char c) {
		if (c == 'c') {
			//:: error: (fields.uninitialized)
			return;
		}
		f = "";
	}
	
	public static void main(String[] args) {
		try {
			FlowInitialization t = new FlowInitialization(false);
			t.f.toLowerCase();
		} catch (NullPointerException e) {
			System.err.println("NullPointerException 1");
		}
		try {
			FlowInitialization t = new FlowInitialization(1);
			t.f.toLowerCase();
		} catch (NullPointerException e) {
			System.err.println("NullPointerException 2");
		}
		try {
			FlowInitialization t = new FlowInitialization('c');
			t.f.toLowerCase();
		} catch (NullPointerException e) {
			System.err.println("NullPointerException 3");
		}
	}
}
