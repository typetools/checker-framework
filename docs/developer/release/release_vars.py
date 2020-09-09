#!/usr/bin/env python3
# encoding: utf-8
"""
release_vars.py

Created by Jonathan Burke on 2013-02-05.

Copyright (c) 2014 University of Washington. All rights reserved.
"""

# See release_development.html for an explanation of how the release process works
# it will be invaluable when trying to understand the scripts that drive the
# release process

import os
import datetime
import pwd
import subprocess
import shlex



#---------------------------------------------------------------------------------
# The only methods that should go here are methods that help define global release
# variables.  All other methods that aid in release should go in release_utils.py

def getAndAppend(name, append):
    """Retrieves the given environment variable and appends the given string to
    its value and returns the new value. The environment variable is not
    modified. Returns an empty string if the environment variable does not
    exist."""
    if os.environ.has_key(name):
        return os.environ[name] + append

    else:
        return ""

def execute(command_args, halt_if_fail=True, capture_output=False, working_dir=None):
  """Execute the given command.
If capture_output is true, then return the output (and ignore the halt_if_fail argument).
If capture_output is not true, return the return code of the subprocess call."""

  if working_dir != None:
    print "Executing in %s: %s" % (working_dir, command_args)
  else:
    print "Executing: %s" % (command_args)
  args = shlex.split(command_args) if isinstance(command_args, str) else command_args

  if capture_output:
    process = subprocess.Popen(args, stdout=subprocess.PIPE, cwd=working_dir)
    out = process.communicate()[0]
    process.wait()
    return out

  else:
    result = subprocess.call(args, cwd=working_dir)
    if halt_if_fail and result:
      raise Exception('Error %s while executing %s' % (result, args))
    return result

#---------------------------------------------------------------------------------

# Maximum allowable size of files when downloading, 2gi
MAX_DOWNLOAD_SIZE = 2000000000

# The location the test site is built in
HTTP_PATH_TO_DEV_SITE = "https://checkerframework.org/dev"
FILE_PATH_TO_DEV_SITE = "/cse/www2/types/checker-framework/dev"
DEV_HTACCESS = os.path.join(FILE_PATH_TO_DEV_SITE, ".htaccess")

# The location the test site is pushed to when it is ready
HTTP_PATH_TO_LIVE_SITE = "https://checkerframework.org"
FILE_PATH_TO_LIVE_SITE = "/cse/www2/types/checker-framework"
LIVE_HTACCESS = os.path.join(FILE_PATH_TO_LIVE_SITE, ".htaccess")

SONATYPE_CLOSING_DIRECTIONS_URL = "http://central.sonatype.org/pages/releasing-the-deployment.html"
SONATYPE_RELEASE_DIRECTIONS_URL = "http://central.sonatype.org/pages/releasing-the-deployment.html"
SONATYPE_DROPPING_DIRECTIONS_URL = "http://central.sonatype.org/pages/releasing-the-deployment.html"

# "USER = os.getlogin()" does not work; see http://bugs.python.org/issue584566
# Another alternative is: USER = os.getenv('USER')
USER = pwd.getpwuid(os.geteuid())[0]

# Per-user directory for the temporary files created by the release process
TMP_DIR = "/scratch/" + USER + "/jsr308-release"

# Location this and other release scripts are contained in
SCRIPTS_DIR = TMP_DIR + "/checker-framework/docs/developer/release"

# Location in which we will download files to run sanity checks
SANITY_DIR = TMP_DIR + "/sanity"
SANITY_TEST_CHECKER_FRAMEWORK_DIR = SANITY_DIR + "/test-checker-framework"

# The existence of this file indicates that release_build completed.
# It is deleted at the beginning of a release_build run, and at the
# end of a release_push run.
RELEASE_BUILD_COMPLETED_FLAG_FILE = TMP_DIR + "/release-build-completed"

# Every time a release is built the changes/tags are pushed here
# When a release is deployed all INTERM repos get pushed to LIVE_REPOS
INTERM_REPO_ROOT = TMP_DIR + "/interm"
INTERM_CHECKER_REPO = os.path.join(INTERM_REPO_ROOT, "checker-framework")
INTERM_ANNO_REPO = os.path.join(INTERM_REPO_ROOT, "annotation-tools")

# The central repositories for Checker Framework related projects
LIVE_ANNO_REPO = "git@github.com:typetools/annotation-tools.git"
LIVE_CHECKER_REPO = "git@github.com:typetools/checker-framework.git"
LIVE_PLUME_SCRIPTS = "https://github.com/plume-lib/plume-scripts"
LIVE_CHECKLINK = "https://github.com/plume-lib/checklink"
LIVE_PLUME_BIB = "https://github.com/mernst/plume-bib"
LIVE_STUBPARSER = "https://github.com/typetools/stubparser"

OPENJDK_RELEASE_SITE = 'http://jdk8.java.net/download.html'

EMAIL_TO = 'checker-framework-discuss@googlegroups.com'

# Location of the project directories in which we will build the actual projects.
# When we build these projects are pushed to the INTERM repositories.
BUILD_DIR = TMP_DIR + "/build/"
CHECKER_FRAMEWORK = os.path.join(BUILD_DIR, 'checker-framework')
CHECKER_FRAMEWORK_RELEASE = os.path.join(CHECKER_FRAMEWORK, 'docs/developer/release')
CHECKER_MANUAL = os.path.join(CHECKER_FRAMEWORK, "docs", "manual")
CHECKER_BIN_DIR = os.path.join(CHECKER_FRAMEWORK, 'checker', 'dist')
CFLOGO = os.path.join(CHECKER_FRAMEWORK, 'docs', 'logo', 'Logo', 'CFLogo.png')
CHECKER_TAG_PREFIXES = ["checker-framework-", "checkers-", "new release "]

# If a new Gradle wrapper was recently installed, the first ./gradlew command outputs:
#   Downloading https://services.gradle.org/distributions/gradle-6.6.1-bin.zip
CF_VERSION_WARMUP = execute("./gradlew version -q", True, True, TMP_DIR + "/checker-framework")
CF_VERSION = execute("./gradlew version -q", True, True, TMP_DIR + "/checker-framework").strip()

CHECKER_CHANGELOG = os.path.join(CHECKER_FRAMEWORK, 'changelog.txt')

ANNO_TOOLS = os.path.join(BUILD_DIR, 'annotation-tools')
ANNO_FILE_UTILITIES = os.path.join(ANNO_TOOLS, 'annotation-file-utilities')
AFU_CHANGELOG = os.path.join(ANNO_FILE_UTILITIES, 'changelog.html')
AFU_TAG_PREFIXES = [""]
AFU_MANUAL = os.path.join(ANNO_FILE_UTILITIES, 'annotation-file-utilities.html')

PLUME_SCRIPTS = os.path.join(BUILD_DIR, 'plume-scripts')
CHECKLINK = os.path.join(BUILD_DIR, 'checklink')
PLUME_BIB = os.path.join(BUILD_DIR, 'plume-bib')
STUBPARSER = os.path.join(BUILD_DIR, 'stubparser')

BUILD_REPOS = (CHECKER_FRAMEWORK, ANNO_TOOLS)
INTERM_REPOS = (INTERM_CHECKER_REPO, INTERM_ANNO_REPO)
LIVE_REPOS = (LIVE_CHECKER_REPO, LIVE_ANNO_REPO)

INTERM_TO_BUILD_REPOS = (
    (INTERM_CHECKER_REPO, CHECKER_FRAMEWORK),
    (INTERM_ANNO_REPO, ANNO_TOOLS)
)

LIVE_TO_INTERM_REPOS = (
    (LIVE_CHECKER_REPO, INTERM_CHECKER_REPO),
    (LIVE_ANNO_REPO, INTERM_ANNO_REPO)
)

AFU_INTERM_RELEASES_DIR = os.path.join(FILE_PATH_TO_DEV_SITE, "annotation-file-utilities", "releases")
CHECKER_INTERM_RELEASES_DIR = os.path.join(FILE_PATH_TO_DEV_SITE, "releases")

AFU_LIVE_SITE = os.path.join(FILE_PATH_TO_LIVE_SITE, "annotation-file-utilities")
AFU_LIVE_RELEASES_DIR = os.path.join(AFU_LIVE_SITE, "releases")

CHECKER_LIVE_SITE = FILE_PATH_TO_LIVE_SITE
CHECKER_LIVE_RELEASES_DIR = os.path.join(CHECKER_LIVE_SITE, "releases")
LIVE_CF_LOGO = os.path.join(CHECKER_LIVE_SITE, "CFLogo.png")

CURRENT_DATE = datetime.date.today()

os.environ['PARENT_DIR'] = BUILD_DIR
os.environ['CHECKERFRAMEWORK'] = CHECKER_FRAMEWORK
perl_libs = TMP_DIR + "/perl_lib:/homes/gws/mernst/bin/src/perl:/homes/gws/mernst/bin/src/perl/share/perl5:/homes/gws/mernst/bin/src/perl/lib/perl5/site_perl/5.10.0/:/homes/gws/mernst/bin/src/perl/lib64/perl5/:/homes/gws/mernst/research/steering/colony-2003/experiment-scripts:/usr/share/perl5/"
# Environment variables for tools needed during the build
os.environ['PLUME_SCRIPTS'] = PLUME_SCRIPTS
os.environ['CHECKLINK'] = CHECKLINK
os.environ['BIBINPUTS'] = '.:' + PLUME_BIB
os.environ['TEXINPUTS'] = '.:/scratch/secs-jenkins/tools/hevea-1.10/lib/hevea:/usr/share/texmf/tex/latex/hevea/:/homes/gws/mernst/tex/sty:/homes/gws/mernst/tex:..:'
os.environ['PERLLIB'] = getAndAppend('PERLLIB', ":")  + perl_libs
os.environ['PERL5LIB'] = getAndAppend('PERL5LIB', ":") + perl_libs
# Still needed for santiy checks
os.environ['JAVA_8_HOME'] = '/usr/lib/jvm/java-1.8.0-openjdk/'
os.environ['JAVA_HOME'] = os.environ['JAVA_8_HOME']

EDITOR = os.getenv('EDITOR')
if EDITOR is None:
    EDITOR = 'emacs'

PATH = os.environ['JAVA_HOME'] + "/bin:/scratch/secs-jenkins/tools/hevea-1.10/bin/:" + os.environ['PATH']
PATH = PATH + ":/usr/bin"
PATH = PATH + ":" + PLUME_SCRIPTS
PATH = PATH + ":" + CHECKLINK
PATH = PATH + ":/homes/gws/mernst/.local/bin" # for html5validator
PATH = PATH + ":."
os.environ['PATH'] = PATH

# Tools that must be on your PATH (besides common *nix ones like grep)
TOOLS = ['hevea', 'perl', 'java', 'latex', 'mvn', 'hg', 'git', 'html5validator', EDITOR]

# Script option constants
AFU_OPT = "annotation-file-utilities"
CF_OPT = "checker-framework"
ALL_OPT = "all"

PROJECTS_TO_SHORTNAMES = [(AFU_OPT, "afu"),
                          (CF_OPT, "cf")]
