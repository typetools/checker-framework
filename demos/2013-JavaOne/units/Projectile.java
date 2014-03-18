

class Projectile {

  // core physics function
  /** Returns the distance a projectile travels */
  @SuppressWarnings("units")
  @m double projectileRange(@mPERs double launchVelocity,
                            @radians double launchAngle) {
    return Math.pow(launchVelocity, 2) * Math.sin(2 * launchAngle) / 9.81;
  }

  // predicate in user code
  /** Returns true if the distance is beyond the range of the projectile. */
  boolean amISafe(@mPERs double launchVelocity,
                  @degrees double launchAngle,
                  @m double distance) {
    return distance > projectileRange(launchVelocity, launchAngle);
  }

  @mPERs double myLaunchVelocity;
  @degrees double myLaunchAngle;
  @m double myDistance;

  @SuppressWarnings("units")
  void init() {
    myLaunchVelocity = 100; // meters per second
    myLaunchAngle = 45; // angle: 45 degrees
    // myLaunchAngle = Math.PI/4; // angle: 45 degrees
    myDistance = 1000; // meters
  }

  void main() {
    init();

    // Returns true!
    amISafe(myLaunchVelocity, myLaunchAngle, myDistance);

    // projectileRange(100, 45) => 912
    // projectileRange(100, MATH.pi/4) = 1020
  }

}















/* Local Variables: */
/* compile-command: "javac Projectile.java" */
/* eval: (setq compile-history '("javac -processor org.checkerframework.checker.units.UnitsChecker Projectile.java")) */
/* End: */
