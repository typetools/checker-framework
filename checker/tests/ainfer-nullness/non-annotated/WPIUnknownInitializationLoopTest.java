// WPI gets stuck in a loop for codes like this because it
// does not currently consider the facts related to the absence of some initializations
// when there are multiple constructors for a class. A solution for this problem makes the
// ainfernullness test on this code pass. The current solution changes the UnknownInitialization
// annotations with all arguments for a Nullable or MonotonicNonNull object to
// UnknownInitialization(java.lang.Object.class).

interface Game {
  void newGame();
}

class GameImpl implements Game {
  private MoveValidator moveValidator;

  public GameImpl(MoveValidator mValidator) {
    mValidator.setGame(this);
    moveValidator = mValidator;
  }

  public GameImpl() {}

  @Override
  public void newGame() {}
}

interface MoveValidator {
  void setGame(Game game);
}

class PlayerDependentMoveValidator implements MoveValidator {
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

class SimpleMoveValidator implements MoveValidator {
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
