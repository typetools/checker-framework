import checkers.fenum.quals.SwingBoxOrientation;
import checkers.fenum.quals.SwingHorizontalOrientation;
import checkers.fenum.quals.SwingVerticalOrientation;
import checkers.fenum.quals.SwingCompassDirection;

public class SwingTest {

	static @SwingVerticalOrientation int BOTTOM;
	static @SwingCompassDirection int NORTH;
	
	static void m(@SwingVerticalOrientation int box) {}
	
	public static void main(String[] args) {
		// ok
		m(BOTTOM);
		
		//:: (argument.type.incompatible)
		m(5);
		
		//:: (argument.type.incompatible)
		m(NORTH);
	}
	
	@SuppressWarnings("swingverticalorientation")
	static void ignoreAll() {
		m(NORTH);
		
		@SwingVerticalOrientation int b = 5;
	}
	
	@SuppressWarnings("fenum:argument.type.incompatible")
	static void ignoreOne() {
		m(NORTH);
		
		//:: (assignment.type.incompatible)
		@SwingVerticalOrientation int b = 5;
	}
	
	void testNull() {
		// This enum should only be used on ints, but I wanted to 
		// test how an Object enum and null interact.
		// Unfortunately, FenumBottom is not a subtype of the programmer
		// introduced qualifiers, so this is forbidden.
		// TODO: Is there a way around this?
		//:: (assignment.type.incompatible)
		@SwingVerticalOrientation Object box = null;
	}

	@SwingVerticalOrientation int testInference0() {
		//:: (assignment.type.incompatible)
		@SwingVerticalOrientation int boxint = 5;
		int box = boxint;
		return box;
	}
	
	Object testInference1() {
	    Object o = new String();
	    return o;
	}

	@SwingVerticalOrientation int testInference2() {
	    int i = BOTTOM;
	    return i;
	}
	
	@SwingVerticalOrientation Object testInference3() {
		//:: (assignment.type.incompatible)
		@SwingVerticalOrientation Object boxobj = new Object();
		Object obox = boxobj;
		return obox;
	}
	
	int testInference4() {
		int aint = 5;
		return aint;
	}

	Object testInference5() {
	    Object o = null;
	    if( 5==4 ) {
	    	o = new Object();
	    }
	    return o;
	}
	
	Object testInference5b() {
	    Object o = null;
	    if( 5==4 ) {
	    	o = new Object();
	    } else {}
	    // the empty else branch actually covers a different code path!
	    return o;
	}
	
	int testInference6() {
		int last = 0;
		last += 1;
		return last;
	}

	// TODO: doesn't work with a null initialisation yet,
	// b/c null is not a subtype of the Swing fenums.
	@SwingBoxOrientation Object testInference7() {
	    Object o = new @SwingVerticalOrientation Object();
	    if( 5==4 ) {
	    	o = new @SwingHorizontalOrientation Object();
	    } else {
	    // 	o = new @SwingVerticalOrientation Object();
	    }
	    return o;
	}
	
	@SwingVerticalOrientation Object testDefaulting0() {
		@checkers.quals.DefaultQualifier("SwingVerticalOrientation")
	    Object o = new String();
	    return o;
	}
}
