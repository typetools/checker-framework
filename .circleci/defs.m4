changequote
changequote(`[',`]')dnl
ifelse([each macro takes two arguments, the OS name and the JDK version])dnl
dnl
define([circleci_boilerplate], [dnl
    resource_class: large
    environment:
      CIRCLE_COMPARE_URL: << pipeline.project.git_url >>/compare/<< pipeline.git.base_revision >>..<<pipeline.git.revision>>
    steps:
      - restore_cache:
          keys:
            - &source-cache source-v1-{{ .Branch }}-{{ .Revision }}
            - 'source-v1-{{ .Branch }}-'
            - source-v1-
      - checkout
      - save_cache:
          key: *source-cache
          paths:
            - .git])dnl
dnl
define([junit_job], [dnl
  junit_jdk$1:
    docker:
      - image: 'mdernst/cf-ubuntu-jdk$1'
circleci_boilerplate
      - run:
         name: test-cftests-junit.sh
         command: export ORG_GRADLE_PROJECT_jdkTestVersion=$1 && ./checker/bin-devel/test-cftests-junit.sh
])dnl
dnl
define([nonjunit_job], [dnl
  nonjunit_jdk$1:
    docker:
      - image: 'mdernst/cf-ubuntu-jdk$1'
circleci_boilerplate
      - run:
         name: test-cftests-nonjunit.sh
         command: export ORG_GRADLE_PROJECT_jdkTestVersion=$1 && ./checker/bin-devel/test-cftests-nonjunit.sh
])dnl
dnl
define([inference_job_split], [dnl
# Split into part1 and part2 only for the inference job that "canary_jobs" depends on.
  inference_part1_jdk$1:
    docker:
      - image: 'mdernst/cf-ubuntu-jdk$1'
circleci_boilerplate
      - run:
         name: test-cftests-inference-part1.sh
         command: export ORG_GRADLE_PROJECT_jdkTestVersion=$1 && ./checker/bin-devel/test-cftests-inference-part1.sh
  inference_part2_jdk$1:
    docker:
      - image: 'mdernst/cf-ubuntu-jdk$1'
circleci_boilerplate
      - run:
         name: test-cftests-inference-part2.sh
         command: export ORG_GRADLE_PROJECT_jdkTestVersion=$1 && ./checker/bin-devel/test-cftests-inference-part2.sh
])dnl
dnl
define([inference_job], [dnl
  inference_jdk$1:
    docker:
      - image: 'mdernst/cf-ubuntu-jdk$1'
circleci_boilerplate
      - run:
         name: test-cftests-inference.sh
         command: export ORG_GRADLE_PROJECT_jdkTestVersion=$1 && ./checker/bin-devel/test-cftests-inference.sh
])dnl
dnl
define([misc_job], [dnl
  misc_jdk$1:
    docker:
      - image: 'mdernst/cf-ubuntu-jdk$1'
circleci_boilerplate
      - run:
         name: test-misc.sh
         command: export ORG_GRADLE_PROJECT_jdkTestVersion=$1 && ./checker/bin-devel/test-misc.sh
])dnl
dnl
define([typecheck_job_split], [dnl
# Split into part1 and part2 only for the typecheck job that "canary_jobs" depends on.
  typecheck_part1_jdk$1:
    docker:
      - image: 'mdernst/cf-ubuntu-jdk$1'
circleci_boilerplate
      - run:
         name: test-cftests-typecheck-part1.sh
         command: export ORG_GRADLE_PROJECT_jdkTestVersion=$1 && ./checker/bin-devel/test-cftests-typecheck-part1.sh
  typecheck_part2_jdk$1:
    docker:
      - image: 'mdernst/cf-ubuntu-jdk$1'
circleci_boilerplate
      - run:
         name: test-cftests-typecheck-part2.sh
         command: export ORG_GRADLE_PROJECT_jdkTestVersion=$1 && ./checker/bin-devel/test-cftests-typecheck-part2.sh
])dnl
dnl
define([typecheck_job], [dnl
  typecheck_jdk$1:
    docker:
      - image: 'mdernst/cf-ubuntu-jdk$1'
circleci_boilerplate
      - run:
         name: test-cftests-typecheck.sh
         command: export ORG_GRADLE_PROJECT_jdkTestVersion=$1 && ./checker/bin-devel/test-cftests-typecheck.sh
])dnl
dnl
define([typecheck_job], [dnl
ifelse($1,canary_version,,[dnl
  typecheck_jdk$1:
    docker:
     - image: 'mdernst/cf-ubuntu-jdk$1-plus[]docker_testing:latest'
circleci_boilerplate
      - run:
         name: test-typecheck.sh
         command: export ORG_GRADLE_PROJECT_jdkTestVersion=$1 && ./checker/bin-devel/test-typecheck.sh
])])dnl
dnl
define([daikon_job_split], [dnl
  daikon_part1_jdk$1:
    docker:
     - image: 'mdernst/cf-ubuntu-jdk$1[]docker_testing:latest'
circleci_boilerplate
      - run:
         name: test-daikon.sh
         command: export ORG_GRADLE_PROJECT_jdkTestVersion=$1 && ./checker/bin-devel/test-daikon-part1.sh
  daikon_part2_jdk$1:
    docker:
     - image: 'mdernst/cf-ubuntu-jdk$1[]docker_testing:latest'
circleci_boilerplate
      - run:
         name: test-daikon-part2.sh
         command: export ORG_GRADLE_PROJECT_jdkTestVersion=$1 && ./checker/bin-devel/test-daikon.sh
])dnl
dnl
define([daikon_job], [dnl
  daikon_jdk$1:
    docker:
     - image: 'mdernst/cf-ubuntu-jdk$1[]docker_testing:latest'
circleci_boilerplate
      - run:
         name: test-daikon.sh
         command: export ORG_GRADLE_PROJECT_jdkTestVersion=$1 && ./checker/bin-devel/test-daikon.sh
])dnl
dnl
define([guava_job], [dnl
  guava_jdk$1:
    docker:
     - image: 'mdernst/cf-ubuntu-jdk$1[]docker_testing:latest'
circleci_boilerplate
      - run:
         name: test-guava.sh
         command: export ORG_GRADLE_PROJECT_jdkTestVersion=$1 && ./checker/bin-devel/test-guava.sh
])dnl
dnl
define([plume_lib_job], [dnl
  plume_lib_jdk$1:
    docker:
     - image: 'mdernst/cf-ubuntu-jdk$1[]docker_testing:latest'
circleci_boilerplate
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
ifelse([
Local Variables:
eval: (add-hook 'after-save-hook '(lambda () (compile "make")) nil 'local)
end:
])dnl
