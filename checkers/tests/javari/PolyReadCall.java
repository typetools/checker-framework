import checkers.javari.quals.*;

public class PolyReadCall {
    private /*this-mutable*/ PolyReadCall f;

    private /*romaybe*/ @PolyRead PolyReadCall getF(/*romaybe*/ @PolyRead PolyReadCall this) {
        return this.f;
    }

    private void barWithThis(/*readonly*/ @ReadOnly PolyReadCall this) {
        /*readonly*/ @ReadOnly PolyReadCall x = this.getF();
    }

    private void barWithoutThis(/*readonly*/ @ReadOnly PolyReadCall this) {
        /*readonly*/ @ReadOnly PolyReadCall x = getF();
    }


}
