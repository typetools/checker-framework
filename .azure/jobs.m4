junit_job(11)
junit_job(17)
junit_job(21)
junit_job(25)

nonjunit_job(canary_version)

# Sometimes one of the invocations of wpi-many in `./gradlew wpiManyTest`
# takes much longer to complete than normal, and this Azure job times out.
# When there is a timeout, one cannot examine wpi or wpi-many logs.
# So use a timeout of 90 minutes, and hope that is enough.
inference_job_split(canary_version)

# Unlimited fetchDepth (0) for misc_jobs, because of need to make contributors.tex .
misc_job(11)
misc_job(17)
misc_job(21)
misc_job(25)

typecheck_job_split(canary_version)

daikon_job_split(canary_version)

## I'm not sure why the guava_jdk11 job is failing (it's due to Error Prone).
guava_job(canary_version)

plume_lib_job(canary_version)
