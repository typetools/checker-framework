junit_job(17)
junit_job(21)
junit_jobs(canary_jdk)
junit_job(latest_jdk)

ifelse([The jdk21 job runs the jtreg tests under checker/jtreg/nullness/jdk24/, which use the
com.sun.tools.classfile API that was removed in Java 25.])dnl
nonjunit_job(21)
nonjunit_job(canary_jdk)

inference_job(canary_jdk)

misc_job(21)
misc_job(canary_jdk)
misc_job(latest_jdk)

typecheck_job(canary_jdk)

daikon_job(canary_jdk)

guava_job(canary_jdk)

plume_lib_job(canary_jdk)
