package advancedcrash;

public class LetItCrash implements SomeInterface {

  Integer longer = 0;

  @Override
  public void doSomethingFancy() {
    System.out.print("Yay");
  }

  @Override
  public void makeItLongerAndCrash() {
    this.longer += 0;
  }
}
