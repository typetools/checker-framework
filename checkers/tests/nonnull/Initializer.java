import java.util.ArrayList;

import checkers.nullness.quals.*;

class Initializer {
	
	public String a;
	public String b = "abc";
	
	//:: error: (assignment.type.incompatible)
	public String c = null;
	
	public Initializer() {
		//:: error: (assignment.type.incompatible)
		a = null;
		a = "";
		c = "";
	}
	
	//:: error: (commitment.fields.uninitialized)
	public Initializer(boolean foo) {
	}
	
	public Initializer(int foo) {
		a = "";
		c = "";
	}
	
}
