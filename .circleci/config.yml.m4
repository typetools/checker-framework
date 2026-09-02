changequote
changequote(`[',`]')dnl
include([../.azure/defs-common.m4])dnl
include([defs.m4])dnl
version: 2.1

jobs:

  # Only proceed to other jobs if canary_jobs passes.
  canary_jobs:
    docker:
      - image: 'cimg/base:2026.08'
    resource_class: small
    environment:
      TERM: dumb
    steps:
      - run: /bin/true

  # Passes only if all other jobs passed.
  all_green:
    docker:
      - image: 'cimg/base:2026.08'
    resource_class: small
    environment:
      TERM: dumb
    steps:
      - run: /bin/true

  ci_info:
    docker:
      - image: 'mdernst/cf-ubuntu-jdk[]canary_jdk-plus'
    resource_class: small
    environment:
      TERM: dumb
    steps:
      - restore_cache:
          keys:
            - &sourcefull-cache source-v1full-{{ .Branch }}-{{ .Revision }}
            - 'source-v1full-{{ .Branch }}-'
            - source-v1full-
      - checkout:
          method: full
      - save_cache:
          key: *sourcefull-cache
          paths:
            - .git
      - run:
          name: getPlumeScripts
          command: ./gradlew -q getPlumeScripts
      - run:
          name: ci-org-and-branch
          command: ./checker/bin-devel/.plume-scripts/ci-org-and-branch --debug
      - run:
          name: git-changes
          command: ./checker/bin-devel/.plume-scripts/git-changes --debug

include([../.azure/jobs.m4])dnl

# The "workflows" section determines which jobs run and what other jobs they depend on.
# For an explanation of the dependence logic, see ../.azure/azure-pipelines.yml .
workflows:
  build:
    jobs:
      - canary_jobs:
          requires:
            - junit_part1_jdk[]canary_jdk
            - junit_part2_jdk[]canary_jdk
            - nonjunit_jdk[]canary_jdk
            - inference_part1_jdk[]canary_jdk
            - inference_part2_jdk[]canary_jdk
            - typecheck_part1_jdk[]canary_jdk
            - typecheck_part2_jdk[]canary_jdk
            - misc_jdk[]canary_jdk
            - misc_jdk[]latest_jdk

      - ci_info

job_dependences(canary_jdk, junit_part1)
job_dependences(canary_jdk, junit_part2)
job_dependences(canary_jdk, nonjunit)
job_dependences(canary_jdk, misc)
job_dependences(latest_jdk, misc)
job_dependences(canary_jdk, inference_part1)
job_dependences(canary_jdk, inference_part2)
job_dependences(canary_jdk, typecheck_part1)
job_dependences(canary_jdk, typecheck_part2)

ifelse([The following jobs are not canary jobs, so they run after canary jobs succeed.])dnl
job_dependences(21, misc)
job_dependences(21, nonjunit)
job_dependences(17, junit)
job_dependences(21, junit)
job_dependences(latest_jdk, junit)

ifelse([The following jobs have no corresponding job in the canary jobs.])dnl
      # TEMPORARILY commented until Daikon release 5.8.24.
      # job_dependences_not_in_canary(canary_jdk, daikon_part1)
      # job_dependences_not_in_canary(canary_jdk, daikon_part2)
      # job_dependences_not_in_canary(canary_jdk, daikon_part3)
job_dependences_not_in_canary(canary_jdk, guava_part1)
job_dependences_not_in_canary(canary_jdk, guava_part2)
job_dependences_not_in_canary(canary_jdk, plume_lib)

      - all_green:
          requires:
            - misc_jdk21
            - nonjunit_jdk21
            - junit_jdk17
            - junit_jdk21
            - junit_jdk26
            - guava_part1_jdk[]canary_jdk
            - guava_part2_jdk[]canary_jdk
            - plume_lib_jdk[]canary_jdk

ifelse([
Local Variables:
eval: (add-hook 'after-save-hook '(lambda () (run-command nil "make")) nil 'local)
end:
])dnl
