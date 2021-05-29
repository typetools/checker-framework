import org.checkerframework.common.value.qual.*;

public class EnumValue {

  enum Direction {
    NORTH,
    WEST,
    SOUTH,
    EAST
  };

  public enum Color {
    BLUE,
    RED,
    GREEN
  };

  private enum Fruit {
    APPLE,
    ORANGE,
    PEAR
  };

  void simpleTest() {
    Direction @ArrayLen(4) [] myCompass = Direction.values();
    Color @ArrayLen(3) [] myColors = Color.values();
    Fruit @ArrayLen(3) [] myFruitBasket = Fruit.values();

    // :: error: (assignment)
    Direction @ArrayLen(7) [] badCompass = Direction.values();

    // :: error: (assignment)
    Color @ArrayLen(4) [] badColors = Color.values();

    // :: error: (assignment)
    Fruit @ArrayLen(2) [] badFruit = Fruit.values();
  }

  public enum AdvDirection {
    ANORTH {
      public AdvDirection getOpposite() {
        return ASOUTH;
      }
    },
    AEAST {
      public AdvDirection getOpposite() {
        return AWEST;
      }
    },
    ASOUTH {
      public AdvDirection getOpposite() {
        return ANORTH;
      }
    },
    AWEST {
      public AdvDirection getOpposite() {
        return AEAST;
      }
    };

    public abstract AdvDirection getOpposite();
  }

  void advTest() {
    AdvDirection @ArrayLen(4) [] myCompass = AdvDirection.values();
    // :: error: (assignment)
    AdvDirection @ArrayLen(3) [] badCompass = AdvDirection.values();
    // :: error: (assignment)
    AdvDirection @ArrayLen(5) [] badCompass2 = AdvDirection.values();
  }
}
