#!/usr/bin/env python
# encoding: utf-8
"""
release.py

Created by Mahmood Ali on 2010-02-25.
Copyright (c) 2010 Jude LLC. All rights reserved.
"""

import sys
import getopt
import urllib2
import re
import subprocess
import os
import pwd

help_message = '''
The help message goes here.
'''

DEFAULT_SITE = "http://types.cs.washington.edu/jsr308/"
OPENJDK_RELEASE_SITE = 'http://download.java.net/openjdk/jdk7/'

def current_distribution(site=DEFAULT_SITE):
    """
    Returns the version of the current release of the checker framework

    """
    ver_re = re.compile(r"<!-- checkers-version -->(.*),")
    text = urllib2.urlopen(url=site).read()
    result = ver_re.search(text)
    return result.group(1)

def latest_openjdk(site=OPENJDK_RELEASE_SITE):
    ver_re = re.compile(r"Build b(\d+)")
    text = urllib2.urlopen(url=site).read()
    result = ver_re.search(text)
    return result.group(1)

def increment_version(version):
    """
    Returns a recommendation of the next incremental version based on the
    passed one.

    >>> increment_version('1.0.3')
    '1.0.3'
    >>> increment_version('1.0.9')
    '1.1.0'
    >>> increment_version('1.1.9')
    '1.2.0'
    >>> increment_version('1.3.1')
    '1.3.2'

    """
    parts = [int(x) for x in version.split('.')]
    parts[2] += 1
    if parts[2] > 9:
        parts[2] -= 10
        parts[1] += 1
    if parts[1] > 9:
        parts[1] -= 10
        parts[0] += 1
    return ".".join([str(x) for x in parts])

def site_copy(ant_args):
    execute("ant -f release.xml %s site-copy" % ant_args)

def site_copy_if_needed(ant_args):
    if not os.path.exists(DRY_PATH):
        return site_copy(ant_args)
    return True

def check_command(command):
    p = execute(['which', command], halt_if_fail=False)
    if p:
        raise AssertionError('command not found: %s' % command)

DEFAULT_PATHS = (
#    '/homes/gws/mernst/research/invariants/scripts',
    '/homes/gws/mernst/bin/share',
#    '/homes/gws/mernst/bin/share-plume',
    '/homes/gws/mernst/bin/Linux-i686',
    '/uns/bin',
    '.',
)

def append_to_PATH(paths=DEFAULT_PATHS):
    current_PATH = os.getenv('PATH')
    new_PATH = current_PATH + ':' + ':'.join(paths)
    os.environ['PATH'] = new_PATH

REPO_ROOT = os.path.dirname(os.path.dirname(__file__))
JSR308_LANGTOOLS = os.path.join(REPO_ROOT, '..', 'jsr308-langtools')
# Is PLUME_LIB even necessary?
PLUME_LIB = os.path.join(os.getenv('HOME'), 'plume-lib')
# Do not include PLUME_LIB in PROJECT_ROOTS, because this script
# commits to all repositories in PROJECT_ROOTS.
PROJECT_ROOTS = (
    REPO_ROOT,
    JSR308_LANGTOOLS,
)
def update_projects(paths=PROJECT_ROOTS):
    for path in PROJECT_ROOTS:
        execute('hg -R %s pull' % path)
	execute('hg -R %s update' % path)
        print("Checking changes")
        # execute('hg -R %s outgoing' % path)

def commit_and_push(version, paths=PROJECT_ROOTS):
    for path in PROJECT_ROOTS:
        execute('hg -R %s commit -m "new release %s"' % (path, version))
        execute('hg -R %s push' % path)

def file_contains(path, text):
    f = open(path, 'r')
    contents = f.read()
    f.close()
    return text in contents

def file_prepend(path, text):
    f = open(path)
    contents = f.read()
    f.close()

    f = open(path, 'w')
    f.write(text)
    f.write(contents)
    f.close()

EDITOR = os.getenv('EDITOR')
if EDITOR == None:
    raise Exception('EDITOR environment variable is not set')
CHECKERS_CHANGELOG = os.path.join(REPO_ROOT, 'checkers', 'changelog-checkers.txt')
def edit_checkers_changelog(version, path=CHECKERS_CHANGELOG):
    edit = raw_input("Edit the Checker Framework changelog? [Y/n] ")
    if not (edit == "n"):
        if not file_contains(path, version):
            import datetime
            today = datetime.datetime.now().strftime("%d %b %Y")
            file_prepend(path,"""Version %s, %s

----------------------------------------------------------------------
""" % (version, today))
    execute([EDITOR, path])

def changelog_header_checkers(file=CHECKERS_CHANGELOG):
    return changelog_header(file)

LANGTOOLS_CHANGELOG = os.path.join(JSR308_LANGTOOLS, 'doc', 'changelog-jsr308.txt')
def edit_langtools_changelog(version, path=LANGTOOLS_CHANGELOG):
    latest_jdk = latest_openjdk()
    print("Latest OpenJDK release is b%s" % latest_jdk)
    edit = raw_input("Edit the JSR308 langtools changelog? [Y/n] ")
    if not (edit == "n"):
        if not file_contains(path, version):
            import datetime
            today = datetime.datetime.now().strftime("%d %b %Y")
            file_prepend(path, """Version %s, %s

Base build
  Updated to OpenJDK langtools build b%s

----------------------------------------------------------------------
""" % (version, today, latest_jdk))

        execute([EDITOR, path])

def changelog_header_langtools(file=LANGTOOLS_CHANGELOG):
    return changelog_header(file)

def make_release(version, ant_args, real=False, sanitycheck=True):
    command = 'ant -f release.xml %s -Drelease.ver=%s %s clean web %s' % (
        '-Drelease.is.real=true' if real else '',
        version,
        ant_args,
        'sanitycheck' if sanitycheck else '',
    )
    print("Actually making the release")
    return execute(command)

def checklinks(site_url=None):
    return execute('make -f %s checklinks' %
        os.path.join(JSR308_LANGTOOLS, 'doc', 'Makefile'),
        halt_if_fail=False)

MAVEN_GROUP_ID = 'types.checkers'
MAVEN_REPO = 'file:///cse/www2/types/m2-repo'

def mvn_deploy(name, binary, version, dest_repo=MAVEN_REPO, ):
    command = """
    mvn deploy:deploy-file
        -DartifactId=%s
        -Dfile=%s
        -Dversion=%s
        -Durl=%s
        -DgroupId=%s
        -Dpackaging=jar
        -DgeneratePom=true
    """ % (name, binary, version, dest_repo, MAVEN_GROUP_ID)
    return execute(command)

CHECKERS_BINARY = os.path.join(REPO_ROOT, 'checkers', 'binary', 'jsr308-all.jar')
def mvn_deploy_jsr308_all(version, binary=CHECKERS_BINARY, dest_repo=MAVEN_REPO):
    return mvn_deploy('jsr308-all', binary, version, dest_repo)

CHECKERS_QUALS = os.path.join(REPO_ROOT, 'checkers', 'checkers-quals.jar')
def mvn_deploy_quals(version, binary=CHECKERS_QUALS, dest_repo=MAVEN_REPO):
    return mvn_deploy('checkers-quals', binary, version, dest_repo)

def execute(command_args, halt_if_fail=True):
    print("Executing: %s" % (command_args))
    import shlex
    if isinstance(command_args, str):
        arg = shlex.split(command_args)
        r = subprocess.call(arg)
    else:
        r = subprocess.call(command_args)
    if halt_if_fail and r:
        raise Exception('Error %s while executing %s' % (r, command_args))
    return r

def changelog_header(filename):
    f = open(filename, 'r')
    header = []

    for line in f:
        if '-------' in line:
            break
        header.append(line)

    return ''.join(header)

class Usage(Exception):
    def __init__(self, msg):
        self.msg = msg

## Define DRY_RUN_LINK (All this is not used, I think; I need to pass in -Dsanitycheck.dry.url=... which is similar but independently defined.)
# "USER = os.getlogin()" does not work; see http://bugs.python.org/issue584566
# Another alternative is: USER = os.getenv('USER')
USER = pwd.getpwuid(os.geteuid())[0]
DRY_RUN_LINK_HTTP = "http://www.cs.washington.edu/homes/%s/jsr308test/jsr308/" % USER
DRY_PATH = os.path.join(os.environ['HOME'], 'www', 'jsr308test')
DRY_RUN_LINK_FILE = "file://%s/jsr308/" % DRY_PATH
DRY_RUN_LINK = DRY_RUN_LINK_HTTP

TO = 'jsr308-discuss@googlegroups.com, checker-framework-discuss@googlegroups.com'
def format_email(version, checkers_header=None, langtools_header=None, to=TO):
    if checkers_header == None:
        checkers_header = changelog_header_checkers()
    if langtools_header == None:
        langtools_header = changelog_header_langtools()

    template = """
=================== BEGINING OF EMAIL =====================

To:  %s
Subject: Release %s of the Checker Framework and Type Annotations compiler

We have released a new version of the Type Annotations (JSR 308) compiler
and of the Checker Framework.  The Type Annotations compiler supports the
Java 7 annotation syntax.  The Checker Framework lets you create and/or run
pluggable type-checkers, in order to detect and prevent bugs in your code.

Notable changes include: [[ FILL ME HERE ]]

Changes for Checker Framework
%s
Changes for Type Annotations Compiler
%s

=================== END OF EMAIL ==========================
    """ % (to, version, checkers_header, langtools_header,)
    return template

def main(argv):
    append_to_PATH()
    print("Making a new release of the Checker Framework!")

    # Infer version
    curr_version = current_distribution()
    print("Current release is %s" % curr_version)
    suggested_version = increment_version(curr_version)
    next_version = raw_input("Suggested next release: (%s): " % suggested_version)
    if not next_version:
        next_version = suggested_version
    print next_version

    # Update repositories
    update_projects()

    edit_checkers_changelog(version=next_version)
    edit_langtools_changelog(version=next_version)

    # Making the first release
    ant_args = ""
    for arg in argv[1:]:      # everything but the first element
        ant_args = ant_args + " '" + arg + "'"
    site_copy_if_needed(ant_args)
    make_release(next_version, ant_args)
    checklinks(DRY_RUN_LINK)

    print("Pushed to %s" % DRY_RUN_LINK)
    raw_input("Please check the site.  Press ENTER to continue.")
    print("\n\n\n\n\n")

    # Making the real release
    make_release(next_version, argv, real=True)

    # Make Maven release
    mvn_deploy_jsr308_all(next_version)
    mvn_deploy_quals(next_version)

    checklinks(DEFAULT_SITE)

    print("Pushed to %s" % DEFAULT_SITE)
    raw_input("Please check the site.  DONE?  Press ENTER to continue.")
    print("\n\n\n\n\n")

    commit_and_push(next_version)

    print("You have just made the release.  Please announce it to the world")
    print("Here is an email template:")
    print format_email(next_version)

# The entry point to the Python script.
if __name__ == "__main__":
    sys.exit(main(sys.argv))
