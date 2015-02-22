class GetClassTest {

    //See AnntoatedTypeFactory.adaptGetClassReturnTypeToReceiver
    @SuppressWarnings("javari") //I believe the issue is with Javari's postTreeAnnotator
                                //normal type systems propagate the annotations from T in the
                                // declaration of Class<T> but Javari does NOT
	void context() {
		Integer i = 4;
		Class<?> a = i.getClass();
		Class<? extends Object> b = i.getClass();
		Class<? extends Integer> c = i.getClass();
	}
	
}