changequote
changequote(`[',`]')dnl
include([../.azure/defs-common.m4])dnl
include([defs.m4])dnl
version: 2.1

jobs:

  # Only proceed to other jobs if canary_jobs passes.
  canary_jobs:
    docker:
      - image: 'cimg/base:2025.10'
    resource_class: small
    environment:
      CIRCLE_COMPARE_URL: << pipeline.project.git_url >>/compare/<< pipeline.git.base_revision >>..<<pipeline.git.revision>>
      TERM: dumb
    steps:
      - run: /bin/true

include([../.azure/jobs.m4])dnl

# The "workflows" section determines which jobs run and what other jobs they depend on.
# For an explanation of the dependence logic, see ../.azure/azure-pipelines.yml .
workflows:
  build:
    jobs:
      - canary_jobs:
          requires:
            - junit_jdk[]canary_version
            - nonjunit_jdk[]canary_version
            - typecheck_part1_jdk[]canary_version
            - typecheck_part2_jdk[]canary_version
            - misc_jdk[]canary_version
            - misc_jdk[]latest_version

job_dependences(11, junit)
job_dependences(17, junit)
job_dependences(21, junit)
job_dependences(25, junit)
job_dependences(canary_version, nonjunit)
job_dependences(11, misc)
job_dependences(17, misc)
job_dependences(21, misc)
job_dependences(25, misc)
job_dependences(canary_version, typecheck_part1)
job_dependences(canary_version, typecheck_part2)

job_dependences_not_in_canary(canary_version, inference_part1)
job_dependences_not_in_canary(canary_version, inference_part2)
job_dependences_not_in_canary(canary_version, daikon_part1)
job_dependences_not_in_canary(canary_version, daikon_part2)
job_dependences_not_in_canary(canary_version, daikon_part3)
job_dependences_not_in_canary(canary_version, guava)
job_dependences_not_in_canary(canary_version, plume_lib)

ifelse([
Local Variables:
eval: (add-hook 'after-save-hook '(lambda () (run-command nil "make")) nil 'local)
end:
])dnl
