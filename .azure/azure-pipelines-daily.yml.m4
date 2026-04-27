# DO NOT EDIT azure-pipelines-daily.yml.  Edit azure-pipelines-daily.yml.m4 and defs.m4 instead.

changequote(`[',`]')dnl
include([defs-common.m4])dnl
include([defs.m4])dnl
trigger: none
pr: none

schedules:
  # 8am UTC is midnight PST.
  - cron: '0 8 * * *'
    displayName: Daily midnight build
    branches:
      include:
        - master

variables:
  system.debug: true

jobs:

  # The dependsOn clauses are:
  #  * Everything depends on the canary jobs (the main jdk25 jobs), except those jobs themselves.
  #  * Any other *_jdkNN job depends on the corresponding *_jdk25 job.

  - job: canary_jobs
    dependsOn:
      - junit_part1_jdk[]canary_version
      - junit_part2_jdk[]canary_version
      - nonjunit_jdk[]canary_version
      - inference_part1_jdk[]canary_version
      - inference_part2_jdk[]canary_version
      - typecheck_part1_jdk[]canary_version
      - typecheck_part2_jdk[]canary_version
    pool:
      vmImage: 'ubuntu-latest'
    steps:
      - bash: true
        displayName: canary_jobs

junit_job(17)
junit_job(21)
junit_jobs(canary_version)
junit_job(latest_version)

nonjunit_job(17)
nonjunit_job(21)
nonjunit_job(canary_version)
nonjunit_job(latest_version)

  # Sometimes one of the invocations of wpi-many in `./gradlew wpiManyTest`
  # takes much longer to complete than normal, and this Azure job times out.
  # When there is a timeout, one cannot examine wpi or wpi-many logs.
  # So use a timeout of 90 minutes, and hope that is enough.
inference_job(17)
inference_job(21)
inference_job(canary_version)
inference_job(latest_version)

  # Do not run misc_job daily, because it does diffs that assume it is running in
  # a pull request.

typecheck_job(17)
typecheck_job(21)
typecheck_job(canary_version)
typecheck_job(latest_version)

  # TEMPORARILY commented until Daikon release 5.8.24.
  # daikon_job(17)
  # daikon_job(21)
  # daikon_job(canary_version)
  # daikon_job(latest_version)

guava_job(17)
guava_job(21)
guava_job(canary_version)
guava_job(latest_version)

plume_lib_job(17)
plume_lib_job(21)
plume_lib_job(canary_version)
plume_lib_job(latest_version)

ifelse([
Local Variables:
eval: (add-hook 'after-save-hook '(lambda () (run-command nil "make")) nil 'local)
end:
])
