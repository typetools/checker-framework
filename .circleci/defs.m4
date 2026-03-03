changequote
changequote(`[',`]')dnl
ifelse([The built-in "dnl" macro means "discard to next line".])dnl
dnl
ifelse([This macro takes 1-3 arguments: the JDK version and optionally a docker
image name suffix like "-plus", and a checkout method "full".])dnl
define([circleci_boilerplate], [dnl
    docker:
      - image: 'mdernst/cf-ubuntu-jdk$1[]$2[]docker_testing'
    resource_class: large
    environment:
      CIRCLE_COMPARE_URL: << pipeline.project.git_url >>/compare/<< pipeline.git.base_revision >>..<<pipeline.git.revision>>
      TERM: dumb
    steps:
      - restore_cache:
          keys:
            - &source$3-cache source-v1$3-{{ .Branch }}-{{ .Revision }}
            - 'source-v1$3-{{ .Branch }}-'
            - source-v1$3-
      - checkout[]ifelse($3,full,[:
          method: full])
      - save_cache:
          key: *source$3-cache
          paths:
            - .git])dnl
dnl
ifelse([Each macro takes one argument, the JDK version.])dnl
dnl
define([junit_job], [dnl
  junit_jdk$1:
circleci_boilerplate($1,)
      - run:
          name: test-cftests-junit.sh
          command: export ORG_GRADLE_PROJECT_jdkTestVersion=$1 && ./checker/bin-devel/test-cftests-junit.sh
          no_output_timeout: "30m"
])dnl
dnl
define([nonjunit_job], [dnl
  nonjunit_jdk$1:
circleci_boilerplate($1,)
      - run:
          name: test-cftests-nonjunit.sh
          command: export ORG_GRADLE_PROJECT_jdkTestVersion=$1 && ./checker/bin-devel/test-cftests-nonjunit.sh
])dnl
dnl
define([inference_job], [dnl
ifelse($1,canary_version, [dnl
  # Split into part1 and part2 only for the inference job that "canary_jobs" depends on.
  inference_part1_jdk$1:
circleci_boilerplate($1,)
      - run:
          name: test-cftests-inference-part1.sh
          command: export ORG_GRADLE_PROJECT_jdkTestVersion=$1 && ./checker/bin-devel/test-cftests-inference-part1.sh
  inference_part2_jdk$1:
circleci_boilerplate($1,)
      - run:
          name: test-cftests-inference-part2.sh
          command: export ORG_GRADLE_PROJECT_jdkTestVersion=$1 && ./checker/bin-devel/test-cftests-inference-part2.sh
], [dnl
  inference_jdk$1:
circleci_boilerplate($1,)
      - run:
          name: test-cftests-inference.sh
          command: export ORG_GRADLE_PROJECT_jdkTestVersion=$1 && ./checker/bin-devel/test-cftests-inference.sh
])])dnl
dnl
define([misc_job], [dnl
  misc_jdk$1:
circleci_boilerplate($1,-plus,full)
      - run:
          name: test-misc.sh
          command: export ORG_GRADLE_PROJECT_jdkTestVersion=$1 && ./checker/bin-devel/test-misc.sh
])dnl
dnl
define([typecheck_job], [dnl
ifelse($1,canary_version,[dnl
  # Split into part1 and part2 only for the typecheck job that "canary_jobs" depends on.
  typecheck_part1_jdk$1:
circleci_boilerplate($1,)
      - run:
          name: test-typecheck-part1.sh
          command: export ORG_GRADLE_PROJECT_jdkTestVersion=$1 && ./checker/bin-devel/test-typecheck-part1.sh
  typecheck_part2_jdk$1:
circleci_boilerplate($1,)
      - run:
          name: test-typecheck-part2.sh
          command: export ORG_GRADLE_PROJECT_jdkTestVersion=$1 && ./checker/bin-devel/test-typecheck-part2.sh
], [dnl
  typecheck_jdk$1:
circleci_boilerplate($1,)
      - run:
          name: test-typecheck.sh
          command: export ORG_GRADLE_PROJECT_jdkTestVersion=$1 && ./checker/bin-devel/test-typecheck.sh
])])dnl
dnl
define([daikon_job], [dnl
  daikon_part1_jdk$1:
circleci_boilerplate($1,)
      - run:
          name: test-daikon-part1.sh
          command: export ORG_GRADLE_PROJECT_jdkTestVersion=$1 && ./checker/bin-devel/test-daikon-part1.sh
          no_output_timeout: "30m"
  daikon_part2_jdk$1:
circleci_boilerplate($1,)
      - run:
          name: test-daikon-part2.sh
          command: export ORG_GRADLE_PROJECT_jdkTestVersion=$1 && ./checker/bin-devel/test-daikon-part2.sh
          no_output_timeout: "30m"
  daikon_part3_jdk$1:
circleci_boilerplate($1,)
      - run:
          name: test-daikon-part3.sh
          command: export ORG_GRADLE_PROJECT_jdkTestVersion=$1 && ./checker/bin-devel/test-daikon-part3.sh
          no_output_timeout: "30m"
])dnl
dnl
define([guava_job], [dnl
  guava_jdk$1:
circleci_boilerplate($1,)
      - run:
          name: test-guava.sh
          command: export ORG_GRADLE_PROJECT_jdkTestVersion=$1 && ./checker/bin-devel/test-guava.sh
          no_output_timeout: "50m"
])dnl
dnl
define([plume_lib_job], [dnl
  plume_lib_jdk$1:
circleci_boilerplate($1,)
      - run:
          name: test-plume-lib.sh
          command: export ORG_GRADLE_PROJECT_jdkTestVersion=$1 && ./checker/bin-devel/test-plume-lib.sh
])dnl
dnl
define([job_dependences], [dnl
ifelse([This is tricky because whether the ":" should appear depends on whether the subsequent "requires: exists,])dnl
      - $2[]_jdk$1[]dnl
ifelse($1,canary_version,,[:
          requires:
            - canary_jobs
ifelse($1,canary_version,,[dnl
            - $2_jdk[]canary_version
])dnl
])dnl
])dnl
dnl
define([job_dependences_not_in_canary], [dnl
      - $2[]_jdk$1[]:
          requires:
            - canary_jobs
])dnl
dnl
ifelse([
Local Variables:
eval: (add-hook 'after-save-hook '(lambda () (run-command nil "make")) nil 'local)
end:
])dnl
