#!/usr/bin/env python
# encoding: utf-8
"""
release_vars.py

Created by Jonathan Burke on 2013-02-05.

Copyright (c) 2014 University of Washington. All rights reserved.
"""

# See release_development.html for an explanation of how the release process works
# it will be invaluable when trying to understand the scripts that drive the
# release process

#from release_utils import *
import os
import datetime
import pwd

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

def append_to_PATH(paths):
    """Retrieves the PATH environment variable, appends the given paths to it,
    and sets the PATH environment variable to the new value."""
    current_PATH = os.getenv('PATH')
    new_PATH = current_PATH + ':' + ':'.join(paths)
    os.environ['PATH'] = new_PATH

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

PGP_PASSPHRASE_FILE = "/projects/swlab1/checker-framework/hosting-info/release-private.password"
SONATYPE_OSS_URL = "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
SONATYPE_STAGING_REPO_ID = "sonatype-nexus-staging"
SONATYPE_CLOSING_DIRECTIONS_URL = "http://central.sonatype.org/pages/releasing-the-deployment.html"
SONATYPE_RELEASE_DIRECTIONS_URL = "http://central.sonatype.org/pages/releasing-the-deployment.html"
SONATYPE_DROPPING_DIRECTIONS_URL = "http://central.sonatype.org/pages/releasing-the-deployment.html"

# "USER = os.getlogin()" does not work; see http://bugs.python.org/issue584566
# Another alternative is: USER = os.getenv('USER')
USER = pwd.getpwuid(os.geteuid())[0]

# Per-user directory for the temporary files created by the release process
TMP_DIR = "/scratch/" + USER + "/jsr308-release"

# Location this and other release scripts are contained in
SCRIPTS_DIR = TMP_DIR + "/checker-framework/release"

# Location in which we will download files to run sanity checks
SANITY_DIR = TMP_DIR + "/sanity"
SANITY_TEST_CHECKER_FRAMEWORK_DIR = SANITY_DIR + "/test-checker-framework"
SANITY_TEST_JSR308_LANGTOOLS_DIR = SANITY_DIR + "/test-jsr308-langtools"

# The existence of this file indicates that release_build completed.
# It is deleted at the beginning of a release_build run, and at the
# end of a release_push run.
RELEASE_BUILD_COMPLETED_FLAG_FILE = TMP_DIR + "/release-build-completed"

# Every time a release is built the changes/tags are pushed here
# When a release is deployed all INTERM repos get pushed to LIVE_REPOS
INTERM_REPO_ROOT = TMP_DIR + "/interm"
INTERM_CHECKER_REPO = os.path.join(INTERM_REPO_ROOT, "checker-framework")
INTERM_JSR308_REPO = os.path.join(INTERM_REPO_ROOT, "jsr308-langtools")
INTERM_ANNO_REPO = os.path.join(INTERM_REPO_ROOT, "annotation-tools")

# The central repositories for Checker Framework related projects
LIVE_JSR308_REPO = "https://bitbucket.org/typetools/jsr308-langtools"
LIVE_ANNO_REPO = "git@github.com:typetools/annotation-tools.git"
LIVE_CHECKER_REPO = "git@github.com:typetools/checker-framework.git"
LIVE_PLUME_LIB = "https://github.com/mernst/plume-lib"
LIVE_PLUME_BIB = "https://github.com/mernst/plume-bib"

OPENJDK_RELEASE_SITE = 'http://jdk8.java.net/download.html'

EMAIL_TO = 'jsr308-discuss@googlegroups.com, checker-framework-discuss@googlegroups.com'

# Location of the project directories in which we will build the actual projects.
# When we build these projects are pushed to the INTERM repositories.
BUILD_DIR = TMP_DIR + "/build/"
CHECKER_FRAMEWORK = os.path.join(BUILD_DIR, 'checker-framework')
CHECKER_FRAMEWORK_RELEASE = os.path.join(CHECKER_FRAMEWORK, 'release')
CHECKER_BIN_DIR = os.path.join(CHECKER_FRAMEWORK, 'checker', 'dist')
CFLOGO = os.path.join(CHECKER_FRAMEWORK, 'docs', 'logo', 'Logo', 'CFLogo.png')
CHECKER_TAG_PREFIXES = ["checker-framework-", "checkers-", "new release "]

CHECKER_BINARY = os.path.join(CHECKER_BIN_DIR, 'checker.jar')
CHECKER_SOURCE = os.path.join(CHECKER_BIN_DIR, 'checker-source.jar')
CHECKER_JAVADOC = os.path.join(CHECKER_BIN_DIR, 'checker-javadoc.jar')

CHECKER_QUAL = os.path.join(CHECKER_BIN_DIR, 'checker-qual.jar')
CHECKER_QUAL_SOURCE = os.path.join(CHECKER_BIN_DIR, 'checker-qual-source.jar')
CHECKER_COMPAT_QUAL = os.path.join(CHECKER_BIN_DIR, 'checker-compat-qual.jar')
CHECKER_COMPAT_QUAL_SOURCE = os.path.join(CHECKER_BIN_DIR, 'checker-compat-qual-source.jar')
CHECKER_MANUAL = os.path.join(CHECKER_FRAMEWORK, "docs", "manual")

JAVAC_BINARY = os.path.join(CHECKER_BIN_DIR, 'javac.jar')
JDK8_BINARY = os.path.join(CHECKER_BIN_DIR, 'jdk8.jar')

JAVACUTIL_DIST_DIR = os.path.join(CHECKER_FRAMEWORK, "javacutil", "dist")
JAVACUTIL_BINARY = os.path.join(JAVACUTIL_DIST_DIR, "javacutil.jar")
JAVACUTIL_SOURCE_JAR = os.path.join(JAVACUTIL_DIST_DIR, "javacutil-source.jar")
JAVACUTIL_JAVADOC_JAR = os.path.join(JAVACUTIL_DIST_DIR, "javacutil-javadoc.jar")

DATAFLOW_DIST_DIR = os.path.join(CHECKER_FRAMEWORK, "dataflow", "dist")
DATAFLOW_BINARY = os.path.join(DATAFLOW_DIST_DIR, "dataflow.jar")
DATAFLOW_SOURCE_JAR = os.path.join(DATAFLOW_DIST_DIR, "dataflow-source.jar")
DATAFLOW_JAVADOC_JAR = os.path.join(DATAFLOW_DIST_DIR, "dataflow-javadoc.jar")

FRAMEWORK_BINARY = os.path.join(CHECKER_FRAMEWORK, 'framework', 'dist', 'framework.jar')

CHECKER_CHANGELOG = os.path.join(CHECKER_FRAMEWORK, 'changelog.txt')

JSR308_LANGTOOLS = os.path.join(BUILD_DIR, 'jsr308-langtools')
JSR308_LT_DOC = os.path.join(JSR308_LANGTOOLS, 'doc')
JSR308_CHANGELOG = os.path.join(JSR308_LANGTOOLS, 'doc', 'changelog-jsr308.txt')
JSR308_MAKE = os.path.join(JSR308_LANGTOOLS, 'make')
JSR308_TAG_PREFIXES = ["jsr308-"]

ANNO_TOOLS = os.path.join(BUILD_DIR, 'annotation-tools')
ANNO_FILE_UTILITIES = os.path.join(ANNO_TOOLS, 'annotation-file-utilities')
AFU_CHANGELOG = os.path.join(ANNO_FILE_UTILITIES, 'changelog.html')
AFU_TAG_PREFIXES = [""]
AFU_MANUAL = os.path.join(ANNO_FILE_UTILITIES, 'annotation-file-utilities.html')

PLUME_LIB = os.path.join(BUILD_DIR, 'plume-lib')
PLUME_BIB = os.path.join(BUILD_DIR, 'plume-bib')

MAVEN_ARTIFACTS_DIR = os.path.join(CHECKER_FRAMEWORK, 'maven-artifacts')
MAVEN_DEV_REPO = 'file:///cse/www2/types/dev/m2-repo'

MAVEN_POMS_DIR = os.path.join(MAVEN_ARTIFACTS_DIR, 'poms')
CHECKER_BINARY_POM = os.path.join(MAVEN_POMS_DIR, 'checkerPom.xml')
CHECKER_QUAL_POM = os.path.join(MAVEN_POMS_DIR, 'checkerQualPom.xml')
CHECKER_COMPAT_QUAL_POM = os.path.join(MAVEN_POMS_DIR, 'checkerCompatQualPom.xml')

JAVAC_BINARY_POM = os.path.join(MAVEN_POMS_DIR, 'compilerPom.xml')
JDK8_BINARY_POM = os.path.join(MAVEN_POMS_DIR, 'jdk8Pom.xml')

MAVEN_RELEASE_DIR = os.path.join(MAVEN_ARTIFACTS_DIR, 'release')
CHECKER_BINARY_RELEASE_POM = os.path.join(MAVEN_RELEASE_DIR, 'checkerReleasePom.xml')
CHECKER_QUAL_RELEASE_POM = os.path.join(MAVEN_RELEASE_DIR, 'checkerQualReleasePom.xml')
CHECKER_COMPAT_QUAL_RELEASE_POM = os.path.join(MAVEN_RELEASE_DIR, 'checkerCompatQualReleasePom.xml')

JAVAC_BINARY_RELEASE_POM = os.path.join(MAVEN_RELEASE_DIR, 'compilerReleasePom.xml')
JDK8_BINARY_RELEASE_POM = os.path.join(MAVEN_RELEASE_DIR, 'jdk8ReleasePom.xml')
JAVACUTIL_BINARY_RELEASE_POM = os.path.join(MAVEN_RELEASE_DIR, 'javacutilReleasePom.xml')
DATAFLOW_BINARY_RELEASE_POM = os.path.join(MAVEN_RELEASE_DIR, 'dataflowReleasePom.xml')

BUILD_REPOS = (CHECKER_FRAMEWORK, JSR308_LANGTOOLS, ANNO_TOOLS)
INTERM_REPOS = (INTERM_CHECKER_REPO, INTERM_JSR308_REPO, INTERM_ANNO_REPO)
LIVE_REPOS = (LIVE_CHECKER_REPO, LIVE_JSR308_REPO, LIVE_ANNO_REPO)

INTERM_TO_BUILD_REPOS = (
    (INTERM_CHECKER_REPO, CHECKER_FRAMEWORK),
    (INTERM_JSR308_REPO, JSR308_LANGTOOLS),
    (INTERM_ANNO_REPO, ANNO_TOOLS)
)

LIVE_TO_INTERM_REPOS = (
    (LIVE_CHECKER_REPO, INTERM_CHECKER_REPO),
    (LIVE_JSR308_REPO, INTERM_JSR308_REPO),
    (LIVE_ANNO_REPO, INTERM_ANNO_REPO)
)

JSR308_INTERM_RELEASES_DIR = os.path.join(FILE_PATH_TO_DEV_SITE, "jsr308", "releases")
AFU_INTERM_RELEASES_DIR = os.path.join(FILE_PATH_TO_DEV_SITE, "annotation-file-utilities", "releases")
CHECKER_INTERM_RELEASES_DIR = os.path.join(FILE_PATH_TO_DEV_SITE, "releases")

JSR308_LIVE_SITE = os.path.join(FILE_PATH_TO_LIVE_SITE, "jsr308")
JSR308_LIVE_RELEASES_DIR = os.path.join(JSR308_LIVE_SITE, "releases")

AFU_LIVE_SITE = os.path.join(FILE_PATH_TO_LIVE_SITE, "annotation-file-utilities")
AFU_LIVE_RELEASES_DIR = os.path.join(AFU_LIVE_SITE, "releases")

CHECKER_LIVE_SITE = FILE_PATH_TO_LIVE_SITE
CHECKER_LIVE_RELEASES_DIR = os.path.join(CHECKER_LIVE_SITE, "releases")
LIVE_CF_LOGO = os.path.join(CHECKER_LIVE_SITE, "CFLogo.png")

CURRENT_DATE = datetime.date.today()

os.environ['JSR308'] = BUILD_DIR
os.environ['CHECKERFRAMEWORK'] = CHECKER_FRAMEWORK
perl_libs = TMP_DIR + "/perl_lib:/homes/gws/mernst/bin/src/perl:/homes/gws/mernst/bin/src/perl/share/perl5:/homes/gws/mernst/bin/src/perl/lib/perl5/site_perl/5.10.0/:/homes/gws/mernst/bin/src/perl/lib64/perl5/:/homes/gws/mernst/research/steering/colony-2003/experiment-scripts:/usr/share/perl5/"
# Environment variables for tools needed during the build
os.environ['PLUME_LIB'] = PLUME_LIB
os.environ['BIBINPUTS'] = '.:' + PLUME_BIB
os.environ['TEXINPUTS'] = '.:/scratch/secs-jenkins/tools/hevea-1.10/lib/hevea:/usr/share/texmf/tex/latex/hevea/:/homes/gws/mernst/tex/sty:/homes/gws/mernst/tex:..:'
os.environ['PERLLIB'] = getAndAppend('PERLLIB', ":")  + perl_libs
os.environ['PERL5LIB'] = getAndAppend('PERL5LIB', ":") + perl_libs
# Still needed for santiy checks
os.environ['JAVA_7_HOME'] = '/scratch/secs-jenkins/java/jdk1.7.0'
os.environ['JAVA_8_HOME'] = '/scratch/secs-jenkins/java/jdk1.8.0'
os.environ['JAVA_HOME'] = os.environ['JAVA_8_HOME']

EDITOR = os.getenv('EDITOR')
if EDITOR is None:
    EDITOR = 'emacs'

PATH = os.environ['JAVA_HOME'] + "/bin:/scratch/secs-jenkins/tools/hevea-1.10/bin/:" + os.environ['PATH']
PATH = PATH + ":/usr/bin:"
PATH = PATH + ":" + PLUME_LIB + "/bin"
PATH = PATH + ":/homes/gws/mernst/.local/bin/:." # for html5validator
os.environ['PATH'] = PATH

# Tools that must be on your PATH (besides common *nix ones like grep)
TOOLS = ['hevea', 'perl', 'java', 'latex', 'mvn', 'hg', 'git', 'html5validator', EDITOR]

# Script option constants
LT_OPT = "langtools"
AFU_OPT = "annotation-file-utilities"
CF_OPT = "checker-framework"
ALL_OPT = "all"

PROJECTS_TO_SHORTNAMES = [(LT_OPT, "lt"),
                          (AFU_OPT, "afu"),
                          (CF_OPT, "cf")]
