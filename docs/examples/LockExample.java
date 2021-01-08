import org.checkerframework.checker.lock.qual.*;

class BankAccount {
    int balance;

    void withdraw(@GuardSatisfied BankAccount this, int amount) {
        this.balance = this.balance - amount;
    }

    void deposit(@GuardedBy("<self>") BankAccount this, int amount) {
        synchronized (this) {
            this.balance = this.balance + amount;
        }
    }
}

public class LockExample {
    final @GuardedBy("<self>") BankAccount myAccount;

    LockExample(@GuardedBy("<self>") BankAccount in) {
        this.myAccount = in;
    }

    void demo1() {
        myAccount.withdraw(100); // error!

        synchronized (myAccount) {
            myAccount.withdraw(100); // OK
        }
    }

    @Holding("myAccount")
    void demo1b() {
        myAccount.withdraw(100); // OK
    }

    void demo1c() {
        demo1b(); // error!

        synchronized (myAccount) {
            demo1b();
        }
    }

    void demo2() {
        myAccount.deposit(500); // OK
    }

    void demo3(Object someotherlock, @GuardedBy("someotherlock") BankAccount otherAccount) {
        otherAccount.deposit(500); // error!
    }

    void demo3b(Object someotherlock, @GuardedBy("#1") BankAccount otherAccount) {
        synchronized (someotherlock) {
            otherAccount.deposit(500); // error!
        }
    }

    void demo4() {
        BankAccount spouseAccount = myAccount; // OK
        spouseAccount.deposit(500); // OK

        synchronized (myAccount) {
            spouseAccount.withdraw(100); // error!
        }
        synchronized (spouseAccount) {
            spouseAccount.withdraw(200); // OK
        }
    }
}
