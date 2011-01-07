import checkers.nullness.quals.*;

class NNOEStaticFields {
    static @Nullable String nullable = null;
    static @Nullable String otherNullable = null;
	
    @NonNullOnEntry("nullable")
    void testF() {
        nullable.toString();
    }

    @NonNullOnEntry("NNOEStaticFields.nullable")
    void testF2() {
        nullable.toString();
    }

    @NonNullOnEntry("nullable")
    void testF3() {
    	NNOEStaticFields.nullable.toString();
    }

    @NonNullOnEntry("NNOEStaticFields.nullable")
    void testF4() {
    	NNOEStaticFields.nullable.toString();
    }

    class Inner {
    	void m1(NNOEStaticFields out) {
    		NNOEStaticFields.nullable = "haha!";
    		out.testF4();
    	}

    	@NonNullOnEntry("NNOEStaticFields.nullable")
    	void m2(NNOEStaticFields out) {
    		out.testF4();
    	}
    }
    
    
    //:: (field.not.found.nullness.parse.error)
    @NonNullOnEntry("NoClueWhatThisShouldBe") void testF5() {
    	//:: (dereference.of.nullable)
    	NNOEStaticFields.nullable.toString();
    }

    void trueNegative() {
        //:: (dereference.of.nullable)
        nullable.toString();
        //:: (dereference.of.nullable)
        otherNullable.toString();
    }

    @NonNullOnEntry("nullable")
    void test1() {
        nullable.toString();
        //:: (dereference.of.nullable)
        otherNullable.toString();
    }

    @NonNullOnEntry("otherNullable")
    void test2() {
        //:: (dereference.of.nullable)
        nullable.toString();
        otherNullable.toString();
    }

    @NonNullOnEntry({"nullable", "otherNullable"})
    void test3() {
        nullable.toString();
        otherNullable.toString();
    }

}
