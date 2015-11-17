// @skip-test

// Test case for issue #520: https://github.com/typetools/checker-framework/issues/520

// compile with: $CHECKERFRAMEWORK/checker/bin-devel/javac -g Issue520.java -processor nullness -AprintAllQualifiers

import java.util.*;

import org.checkerframework.checker.nullness.qual.*;

public class Issue520 {
}

abstract class Parent<T> {
	protected final List<? super @KeyForBottom T> list;

	public Parent(List<? super @KeyForBottom T> list) {
		this.list = list;
	}
}

abstract class Child extends Parent<CharSequence> {
	public Child(List<? super CharSequence> list) {
		super(list);
	}

	public void add(CharSequence seq) {
		list.add(seq);
	}
}
