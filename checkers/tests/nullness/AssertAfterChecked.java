import checkers.nullness.quals.*;

import java.util.*;

public class AssertAfterChecked {

	class InitField {
		@Nullable Object f;

		@AssertNonNullAfter("f")
		void init() {
			f = new Object();
		}

		//:: (assert.postcondition.not.satisfied)
		@AssertNonNullAfter("f") void initBad() {
		}

		void testInit() {
			init();
			f.toString();
		}
	}
	
	static class InitStaticField {
		static @Nullable Object f;

		@AssertNonNullAfter("f")
		void init() {
			f = new Object();
		}

		@AssertNonNullAfter("f")
		void init2() {
			InitStaticField.f = new Object();
		}

		//:: (assert.postcondition.not.satisfied)
		@AssertNonNullAfter("f") void initBad() {
		}

		void testInit() {
			init();
			f.toString();
		}
		
		@AssertNonNullAfter("InitStaticField.f")
		void initE() {
			f = new Object();
		}

		@AssertNonNullAfter("InitStaticField.f")
		void initE2() {
			InitStaticField.f = new Object();
		}

		//:: (assert.postcondition.not.satisfied)
		@AssertNonNullAfter("InitStaticField.f") void initBadE() {
		}

		void testInitE() {
			initE();
			// TODO: we need to also support the unqualified static field access?
			// f.toString();
		}

		void testInitE2() {
			initE();
			InitStaticField.f.toString();
		}
	}
	
	class TestParams {
		//:: warning: (nullness.parse.error)
		@AssertNonNullAfter("get(#0)") void init(TestParams p) {
			
		}

		@Nullable Object get(Object o) {
			return null;
		}
		
		
		void testInit1() {
			init(this);
			get(this).toString();
		}

		void testInit1b() {
			init(this);
			// TODO: the explicit this does not work :-((
			// this.get(this).toString();
		}

		void testInit2(TestParams p) {
			init(p);
			get(p).toString();
		}

		void testInit3(TestParams p) {
			p.init(this);
			p.get(this).toString();
		}

		void testInit4(TestParams p) {
			p.init(this);
			//:: (dereference.of.nullable)
			this.get(this).toString();
		}

	}
}