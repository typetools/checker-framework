import checkers.fenum.quals.*;

public class Wellformed {
	//javac:: annotation type not applicable to this kind of declaration
	// void m(@FenumDecl int p) {
	// }
	
	//:: (type.invalid)
	@Fenum @FenumDecl int x;

	//:: (type.invalid)
	@Fenum @FenumDecl Object y;
	
	
	@Fenum Object[] enumarr;
}
