changequote
changequote(`[',`]')dnl
include([defs.m4])dnl
version: 2.1

jobs:

  # Only proceed to other jobs if canary-jobs passes.
  canary-jobs:
    docker:
      - image: 'cimg/base:2025.09'
    environment:
      CIRCLE_COMPARE_URL: << pipeline.project.git_url >>/compare/<< pipeline.git.base_revision >>..<<pipeline.git.revision>>
    steps:
      - run: /bin/true

include([../.azure/jobs.m4])dnl

# The "workflows" section determines which jobs run and what other jobs they depend on.
# For an explanation of the dependence logic, see ../.azure/azure-pipelines.yml .
workflows:
  version: 2
  build:
    jobs:
      - canary-jobs:
          requires:
            - quick-txt-diff-jdk[]canary_version
            - nonquick-txt-diff-jdk[]canary_version
            - non-txt-diff-jdk[]canary_version
            - misc-jdk[]canary_version
            - kvasir-jdk[]canary_version
            - typecheck-bundled-jdk[]canary_version
job_dependences(17, junit_job)
job_dependences(21, junit_job)
job_dependences(25, junit_job)
job_dependences(canary_version, nonjunit_job)
job_dependences(canary_version, inference_job_split)
job_dependences(17, misc_job)
job_dependences(21, misc_job)
job_dependences(25, misc_job)
