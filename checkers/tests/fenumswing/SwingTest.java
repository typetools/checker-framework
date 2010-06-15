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
	}
	
	@SuppressWarnings("fenum.argument.type.incompatible")
	static void ignoreOne() {
		m(SwingConstants.NORTH);
	}

}
