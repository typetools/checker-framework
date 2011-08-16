import checkers.javari.quals.*;

public class PolyReadCall {
    private /*this-mutable*/ PolyReadCall f;

    private /*romaybe*/ @PolyRead PolyReadCall getF() /*romaybe*/ @PolyRead {
        return this.f;
    }

    private void barWithThis() /*readonly*/ @ReadOnly {
        /*readonly*/ @ReadOnly PolyReadCall x = this.getF();
    }

    private void barWithoutThis() /*readonly*/ @ReadOnly {
        /*readonly*/ @ReadOnly PolyReadCall x = getF();
    }


}
