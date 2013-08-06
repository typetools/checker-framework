import checkers.interning.quals.*;


public class CompileTimeConstants {
	class A {
		final static String a1 = "hello";
		@Interned String a2 = "a2";
		
		void method(){
			if(a1 == "hello"){}
			
		}
	}
	
	class B {
		final static String b1 = "hello";
		
		void method(){
			if(b1 == A.a1){}
		}
	}

}
