
import checkers.nonnull.quals.*;
import checkers.commitment.quals.*;

class MethodInvocation {

	String s;
	
	public MethodInvocation() {
		//:: error: (method.invocation.invalid)
		a();
		
		b();
		
		c();
		
		s = "abc";
	}
	
	public MethodInvocation(boolean p) {
		s = "abc";
		
		//:: error: (method.invocation.invalid)
		a(); // still not okay to be committed
	}
	
	public void a() {
	}

	public void b(@Free MethodInvocation this) {
		//:: error: (dereference.of.nullable)
		s.hashCode();
	}
	
	public void c() {
		//:: error: (dereference.of.nullable)
		s.hashCode(@Unclassified MethodInvocation this);
	}
	
}
