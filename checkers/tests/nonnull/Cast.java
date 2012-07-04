
import checkers.commitment.quals.*;

class Cast {
	
	public Cast() {
		@Unclassified Cast t1 = (@Unclassified Cast) this;
		//:: error: (commitment.invalid.cast)
		@Committed Cast t2 = (@Committed Cast) this;
	}
	
}
