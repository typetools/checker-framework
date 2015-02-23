
import java.util.List;
import polyall.quals.*;

//NEED TO ADD ONES WHERE THERE IS AN EXTENDS NEEDED IN BOTH LUBS/GLBS
class InferTypeArgsPolyChecker {
	<A> A methodA(@H2Top A a1, @H2Top A a2) {
		return null;
	}
	
	void contextA(@H1S1 @H2Bot String str, @H1Bot @H2Bot List<@H1S2 String> s) {
		@H2Bot Object a = methodA(str, s);
        @H1Top @H2Bot Object aTester = a;
	}

    <B> B methodB(List<@H2S2 B> b1, List<@H1S2 B> b2) {
        return null;
    }

    void contextB(List<@H1S1 @H2S2 String> l1, List<@H1S2 @H2S1 String> l2) {
        @H1S1 @H2S1 String str = methodB(l1, l2);
    }
}