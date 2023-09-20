// @below-java21-jdk-skip-test
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// These are examples copied from:
// https://openjdk.org/jeps/441

@SuppressWarnings("i18n") // true postives.
public class JEP441 {

  // We enhance switch statements and expressions in four ways:
  //  * Improve enum constant case labels
  //  * Extend case labels to include patterns and null in addition to constants
  //  * Broaden the range of types permitted for the selector expressions of both switch statements
  //    and switch expressions (along with the required richer analysis of exhaustiveness of switch
  //    blocks)
  //  * Allow optional when clauses to follow case labels.

  // Prior to Java 21
  static String formatter(Object obj) {
    String formatted = "unknown";
    if (obj instanceof Integer i) {
      formatted = String.format("int %d", i);
    } else if (obj instanceof Long l) {
      formatted = String.format("long %d", l);
    } else if (obj instanceof Double d) {
      formatted = String.format("double %f", d);
    } else if (obj instanceof String s) {
      formatted = String.format("String %s", s);
    }
    return formatted;
  }
  static void formatterPatternSwitchStatement(Object obj) {
     switch (obj) {
      case Integer i: String.format("int %d", i); break;
      case Long l   :String.format("long %d", l);break;
      case Double d  :String.format("double %f", d);break;
      case String s  :String.format("String %s", s);break;
      default        : obj.toString();
    };
  }

  static String formatterPatternSwitch(Object obj) {
    return switch (obj) {
      case Integer i -> String.format("int %d", i);
      case Long l    -> String.format("long %d", l);
      case Double d  -> String.format("double %f", d);
      case String s  -> String.format("String %s", s);
      default        -> obj.toString();
    };
  }
  // As of Java 21
  static void testFooBarNew(String s) {
    switch (s) {
      case null         -> System.out.println("Oops");
      case "Foo", "Bar" -> System.out.println("Great");
      default           -> System.out.println("Ok");
    }
  }

  static void testStringOld(String response) {
    switch (response) {
      case null -> { }
      case String s -> {
        if (s.equalsIgnoreCase("YES"))
          System.out.println("You got it");
        else if (s.equalsIgnoreCase("NO"))
          System.out.println("Shame");
        else
          System.out.println("Sorry?");
      }
    }
  }

  static void testStringNew(String response) {
    switch (response) {
      case null -> { }
      case String s
          when s.equalsIgnoreCase("YES") -> {
        System.out.println("You got it");
      }
      case String s
          when s.equalsIgnoreCase("NO") -> {
        System.out.println("Shame");
      }
      case String s -> {
        System.out.println("Sorry?");
      }
    }
  }

  static void testStringEnhanced(String response) {
    switch (response) {
      case null -> { }
      case "y", "Y" -> {
        System.out.println("You got it");
      }
      case "n", "N" -> {
        System.out.println("Shame");
      }
      case String s
          when s.equalsIgnoreCase("YES") -> {
        System.out.println("You got it");
      }
      case String s
          when s.equalsIgnoreCase("NO") -> {
        System.out.println("Shame");
      }
      case String s -> {
        System.out.println("Sorry?");
      }
    }
  }

  sealed interface CardClassification permits Suit, Tarot {}
  public enum Suit implements CardClassification { CLUBS, DIAMONDS, HEARTS, SPADES }
  final class Tarot implements CardClassification {}

  static void exhaustiveSwitchWithoutEnumSupport(CardClassification c) {
    switch (c) {
      case Suit s when s == Suit.CLUBS -> {
        System.out.println("It's clubs");
      }
      case Suit s when s == Suit.DIAMONDS -> {
        System.out.println("It's diamonds");
      }
      case Suit s when s == Suit.HEARTS -> {
        System.out.println("It's hearts");
      }
      case Suit s -> {
        System.out.println("It's spades");
      }
      case Tarot t -> {
        System.out.println("It's a tarot");
      }
    }
  }

  static void exhaustiveSwitchWithBetterEnumSupport(CardClassification c) {
    switch (c) {
      case Suit.CLUBS -> {
        System.out.println("It's clubs");
      }
      case Suit.DIAMONDS -> {
        System.out.println("It's diamonds");
      }
      case Suit.HEARTS -> {
        System.out.println("It's hearts");
      }
      case Suit.SPADES -> {
        System.out.println("It's spades");
      }
      case Tarot t -> {
        System.out.println("It's a tarot");
      }
    }
  }
  sealed interface Currency permits Coin {}
  enum Coin implements Currency { HEADS, TAILS }

  static void goodEnumSwitch1(Currency c) {
    switch (c) {
      case Coin.HEADS -> {    // Qualified name of enum constant as a label
        System.out.println("Heads");
      }
      case Coin.TAILS -> {
        System.out.println("Tails");
      }
    }
  }

  static void goodEnumSwitch2(Coin c) {
    switch (c) {
      case HEADS -> {
        System.out.println("Heads");
      }
      case Coin.TAILS -> {    // Unnecessary qualification but allowed
        System.out.println("Tails");
      }
    }
  }

  record Point(int i, int j) {}
  enum Color { RED, GREEN, BLUE; }

  static void typeTester(Object obj) {
    switch (obj) {
      case null     -> System.out.println("null");
      case String s -> System.out.println("String");
      case Color c  -> System.out.println("Color: " + c.toString());
      case Point p  -> System.out.println("Record class: " + p.toString());
      case int[] ia -> System.out.println("Array of ints of length" + ia.length);
      default       -> System.out.println("Something else");
    }
  }

  static void first(Object obj) {
    switch (obj) {
      case String s ->
          System.out.println("A string: " + s);
      case CharSequence cs ->
          System.out.println("A sequence of length " + cs.length());
      default -> {
        break;
      }
    }
  }

  void fragment( Integer i ){
    // TODO: This would be a good test case for the Value Checker.
//    switch (i) {
//      case -1, 1 -> ...                   // Special cases
//      case Integer j when j > 0 -> ...    // Positive integer cases
//      case Integer j -> ...               // All the remaining integers
//    }
  }
}
