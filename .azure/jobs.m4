junit_job(17)
junit_job(21)
junit_jobs(canary_version)
junit_job(latest_version)

nonjunit_job(canary_version)

  # Sometimes one of the invocations of wpi-many in `./gradlew wpiManyTest`
  # takes much longer to complete than normal, and this Azure job times out.
  # When there is a timeout, one cannot examine wpi or wpi-many logs.
  # So use a timeout of 90 minutes, and hope that is enough.
inference_job(canary_version)

misc_job(17)
misc_job(21)
misc_job(canary_version)
misc_job(latest_version)

typecheck_job(canary_version)

  # TEMPORARILY commented until Daikon release 5.8.24.
  # daikon_job(canary_version)

guava_job(canary_version)

plume_lib_job(canary_version)
