changequote(`[',`]')dnl
include([defs.m4])dnl
# Workaround for https://status.dev.azure.com/_event/179641421
trigger:
  branches:
    include:
    - '*'
pr:
  branches:
    include:
    - '*'

variables:
  system.debug: true

jobs:

# The dependsOn clauses are:
#  * Everything depends on the canary jobs (the main jdk21 jobs), except those jobs themselves.
#  * Anything *_jdk11 or *_jdk17 or *_jdk23 depends on *_jdk21.

- job: canary_jobs
  dependsOn:
   - junit_jdk21
   - nonjunit_jdk21
   - inference_part1_jdk21
   - inference_part2_jdk21
   - typecheck_part1_jdk21
   - typecheck_part2_jdk21
   - misc_jdk21
   - misc_jdk23
  pool:
    vmImage: 'ubuntu-latest'
  steps:
  - bash: true
    displayName: canary_jobs

junit_job(11)
junit_job(17)
junit_job(21)
junit_job(23)

nonjunit_job(11)
nonjunit_job(17)
nonjunit_job(21)
nonjunit_job(23)

# Sometimes one of the invocations of wpi-many in `./gradlew wpiManyTest`
# takes much longer to complete than normal, and this Azure job times out.
# When there is a timeout, one cannot examine wpi or wpi-many logs.
# So use a timeout of 90 minutes, and hope that is enough.
inference_job(11)
inference_job(17)
inference_job_lts(21)
inference_job(23)

# Unlimited fetchDepth for misc_jobs, because of need to make contributors.tex
misc_job(11)
misc_job(17)
misc_job(21)
misc_job(23)

typecheck_job(11)
typecheck_job(17)
typecheck_job_lts(21)
typecheck_job(23)

daikon_job(11)
daikon_job(17)
daikon_job_lts(21)
daikon_job(23)

## I'm not sure why the guava_jdk11 job is failing (it's due to Error Prone).
guava_job(17)
guava_job(21)
guava_job(23)

plume_lib_job(11)
plume_lib_job(17)
plume_lib_job(21)
plume_lib_job(23)

## The downstream jobs are not currently needed because test-downstream.sh is empty.
# - job: downstream_jdk11
#   dependsOn:
#    - canary_jobs
#    - downstream_jdk21
#   pool:
#     vmImage: 'ubuntu-latest'
#   container: mdernst/cf-ubuntu-jdk11:latest
#   steps:
#   - checkout: self
#     fetchDepth: 25
#   - bash: ./checker/bin-devel/test-downstream.sh
#     displayName: test-downstream.sh
# - job: downstream_jdk17
#   dependsOn:
#    - canary_jobs
#    - downstream_jdk21
#   pool:
#     vmImage: 'ubuntu-latest'
#   container: mdernst/cf-ubuntu-jdk17:latest
#   steps:
#   - checkout: self
#     fetchDepth: 25
#   - bash: ./checker/bin-devel/test-downstream.sh
#     displayName: test-downstream.sh
# - job: downstream_jdk21
#   dependsOn:
#    - canary_jobs
#   pool:
#     vmImage: 'ubuntu-latest'
#   container: mdernst/cf-ubuntu-jdk21:latest
#   steps:
#   - checkout: self
#     fetchDepth: 25
#   - bash: ./checker/bin-devel/test-downstream.sh
#     displayName: test-downstream.sh
# - job: downstream_jdk23
#   dependsOn:
#    - canary_jobs
#    - downstream_jdk21
#   pool:
#     vmImage: 'ubuntu-latest'
#   container: mdernst/cf-ubuntu-jdk23:latest
#   steps:
#   - checkout: self
#     fetchDepth: 25
#   - bash: ./checker/bin-devel/test-downstream.sh
#     displayName: test-downstream.sh
dnl
ifelse([
Local Variables:
eval: (make-local-variable 'after-save-hook)
eval: (add-hook 'after-save-hook '(lambda () (compile "make")))
end:
])
