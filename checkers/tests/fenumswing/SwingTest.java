import javax.swing.SwingConstants;
import checkers.fenum.quals.SwingBoxOrientation;

public class SwingTest {

	static void m(@SwingBoxOrientation int box) {}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// ok
		m(SwingConstants.BOTTOM);
		
		//:: (fenum.argument.type.incompatible)
		m(5);
		
		//:: (fenum.argument.type.incompatible)
		m(SwingConstants.NORTH);
	}
	
	@SuppressWarnings("swingboxorientation")
	static void ignoreAll() {
		m(SwingConstants.NORTH);
		
		@SwingBoxOrientation int b = 5;
	}
	
	@SuppressWarnings("fenum.argument.type.incompatible")
	static void ignoreOne() {
		m(SwingConstants.NORTH);
		
		//:: (fenum.assignment.type.incompatible)
		@SwingBoxOrientation int b = 5;
	}
	
	void test() {
		// This enum should not be used on ints, but I wanted to 
		// test how an Object enum and null interact.
		// Unfortunately, FenumBottom is not a subtype of the programmer
		// introduced qualifiers, so this is forbidden.
		// Is there a way around this?
		//:: (fenum.assignment.type.incompatible)
		@SwingBoxOrientation Object box = null;
	}

}
