// Test case for issue #4579

// @skip-test until the issue is fixed

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;

public class Issue4579 {

  public Issue4579() {
    final JButton button = new JButton();

    // this reports a warning about under initialization
    button.addActionListener(l -> doAction());

    // this reports no warnings
    button.addActionListener(new ListenerClass());

    // this reports a warning about under initialization
    button.addActionListener(
        new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            doAction();
          }
        });
  }

  private void doAction() {
    System.out.println("Action");
  }

  private class ListenerClass implements ActionListener {

    @Override
    public void actionPerformed(final ActionEvent e) {
      doAction();
    }
  }
}
