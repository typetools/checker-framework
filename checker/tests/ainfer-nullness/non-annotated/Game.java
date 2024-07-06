import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;

interface Game {
  void newGame();
}

class GameImpl implements Game { // package-private
  private MoveValidator moveValidator;

  public GameImpl(MoveValidator mValidator) {
    mValidator.setGame(this);
    moveValidator = mValidator;
  }

  public GameImpl() {}

  @Override
  public void newGame() {
    // Implementation of starting a new game
  }
}

interface MoveValidator {
  void setGame(Game game);
}

class PlayerDependentMoveValidator implements MoveValidator { // package-private
  public Game game;
  private MoveValidator blackMoveValidator = new SimpleMoveValidator();

  @SuppressWarnings({"override.param", "contracts.postcondition"})
  @Override
  public void setGame(Game game) {
    this.game = game;
  }

  public PlayerDependentMoveValidator() {}

  @SuppressWarnings({"contracts.postcondition", "argument", "method.invocation"})
  public PlayerDependentMoveValidator(Game game) {
    this.setGame(game);
    blackMoveValidator.setGame(game);
  }
}

class SimpleMoveValidator implements MoveValidator { // package-private
  private Game game;

  @SuppressWarnings({"override.param", "contracts.postcondition"})
  @Override
  public void setGame(Game game) {
    this.game = game;
  }

  public SimpleMoveValidator() {}

  @SuppressWarnings({
    "purity.not.deterministic.object.creation",
    "purity" + ".not.sideeffectfree.call",
    "contracts.postcondition",
    "return"
  })
  public MoveValidator createMoveValidator() {
    return new PlayerDependentMoveValidator(game);
  }

  public void test() {
    PlayerDependentMoveValidator g1 = new PlayerDependentMoveValidator(game);
    // :: warning: (assignment)
    this.game = g1.game;
  }
}
