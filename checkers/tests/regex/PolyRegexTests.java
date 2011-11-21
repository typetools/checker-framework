import checkers.regex.quals.PolyRegex;
import checkers.regex.quals.Regex;

public class PolyRegexTests {

	public static @PolyRegex String method (@PolyRegex String s) {
		return s;
	}
	
	public void testRegex(@Regex String str) {
		@Regex String s = method(str);
	}
	
	public void testNonRegex(String str) {
		//:: error: (assignment.type.incompatible)
		@Regex String s = method(str);	// error
	}
}
