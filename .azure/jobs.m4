junit_job(17)
junit_job(21)
junit_jobs(canary_jdk)
junit_job(latest_jdk)

nonjunit_job(canary_jdk)

  # Sometimes one of the invocations of wpi-many in `./gradlew wpiManyTest`
  # takes much longer to complete than normal, and this Azure job times out.
  # When there is a timeout, one cannot examine wpi or wpi-many logs.
  # So use a timeout of 90 minutes, and hope that is enough.
inference_job(canary_jdk)

misc_job(21)
misc_job(canary_jdk)
misc_job(latest_jdk)

typecheck_job(canary_jdk)

  # TEMPORARILY commented until Daikon release 5.8.24.
  # daikon_job(canary_jdk)

guava_job(canary_jdk)

plume_lib_job(canary_jdk)
