// Test of use of Lock interface

import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.checkerframework.checker.lock.qual.Holding;
import org.checkerframework.checker.lock.qual.MayReleaseLocks;
import org.checkerframework.checker.lock.qual.ReleasesNoLocks;

public class LockInterfaceTest {

    static final Lock myStaticLock = new ReentrantLock(true);

    private static final Date x = new Date((long) (System.currentTimeMillis() * Math.random()));

    @Holding("myStaticLock")
    @ReleasesNoLocks
    static void method4() {
        System.out.println(x);
    }

    @Holding("LockInterfaceTest.myStaticLock")
    @ReleasesNoLocks
    static void method5() {
        System.out.println(x);
    }

    @MayReleaseLocks
    public static void test1() {
        LockInterfaceTest.myStaticLock.lock();
        method4();
        LockInterfaceTest.myStaticLock.unlock();
    }

    @MayReleaseLocks
    public static void test2() {
        LockInterfaceTest.myStaticLock.lock();
        method5();
        LockInterfaceTest.myStaticLock.unlock();
    }

    @MayReleaseLocks
    public static void test3() {
        myStaticLock.lock();
        method4();
        myStaticLock.unlock();
    }

    @MayReleaseLocks
    public static void test4() {
        myStaticLock.lock();
        method5();
        myStaticLock.unlock();
    }
}
