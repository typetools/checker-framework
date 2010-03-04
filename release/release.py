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

def site_copy():
    execute('ant -f release.xml site-copy')

def site_copy_if_needed():
    dry_path = os.path.join(os.environ['HOME'], 'www', 'jsr308test')
    if not os.path.exists(dry_path):
        return site_copy()
    return True

def check_command(command):
    p = execute(['which', command], halt_if_fail=False)
    if p:
        raise AssertionError('command not found: %s' % command)

DEFAULT_PATHS = (
    '/homes/gws/mernst/research/invariants/scripts',
    '/homes/gws/mernst/bin/share',
    '/homes/gws/mernst/bin/Linux-i686',
    '/uns/bin',
    '.',
)

PERL_PATHS = (
    '/homes/gws/mernst/research/invariants/scripts',
    '/homes/gws/mernst/bin/src/plume-lib/bin',
)

def append_to_PATH(paths=DEFAULT_PATHS, perl_paths=PERL_PATHS):
    current_PATH = os.getenv('PATH')
    new_PATH = current_PATH + ':' + ':'.join(paths)
    os.environ['PATH'] = new_PATH

    current_PERL = os.getenv('PERL5LIB')
    if current_PERL:
        new_PERL = current_PERL + ':' + ':'.join(perl_paths)
    else:
        new_PERL = ':'.join(perl_paths)
    os.environ['PERL5LIB'] = new_PERL

REPO_ROOT = os.path.dirname(os.path.dirname(__file__))
PROJECT_ROOTS = (
    REPO_ROOT,
    os.path.join(REPO_ROOT, 'jsr308-langtools'),
)
def update_projects(paths=PROJECT_ROOTS):
    for path in PROJECT_ROOTS:
        execute('hg -R %s pull -u' % path)
        print("Checking changes")
        execute('hg -R %s outgoing')

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

EDITOR = 'vim'
CHECKERS_CHANGELOG = os.path.join(REPO_ROOT, 'checkers', 'changelog-checkers.txt')
def edit_checkers_changelog(version, path=CHECKERS_CHANGELOG):
    raw_input("About to edit the Checker Framework changelog.  OK?")
    if not file_contains(path, version):
        import datetime
        today = datetime.datetime.now().strftime("%d %b %Y")
        file_prepend(path,"""Version %s, %s

----------------------------------------------------------------------
""" % (version, today))

    execute([EDITOR, path])

LANGTOOLS_CHANGELOG = os.path.join(REPO_ROOT, 'jsr308-langtools', 'doc', 'changelog-jsr308.txt')
def edit_langtools_changelog(version, path=LANGTOOLS_CHANGELOG):
    latest_jdk = latest_openjdk()
    print("Latest OpenJDK release is b%s" % latest_jdk)
    raw_input("About to edit the JSR308 langtools changelog.  OK?")
    if not file_contains(path, version):
        import datetime
        today = datetime.datetime.now().strftime("%d %b %Y")
        file_prepend(path, """Version %s, %s

Base build
  Updated to OpenJDK langtools build b%s

----------------------------------------------------------------------
""" % (version, today, latest_jdk))

    execute([EDITOR, path])

def make_release(version, real=False, sanitycheck=True):
    command = 'ant -f release.xml %s -Drelease.ver=%s clean web %s' % (
        '-Drelease.is.real=true' if real else '',
        version,
        'sanitycheck' if sanitycheck else '',
    )
    print("Actually making the release")
    return execute(command)

def checklinks(site_url=None):
    return execute('make -f %s checklinks' %
        os.path.join(REPO_ROOT, 'jsr308-langtools', 'doc', 'Makefile'),
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

CHECKERS_QUALS = os.path.join(REPO_ROOT, 'checkers', 'checkers', 'checkers-quals.jar')
def mvn_deploy_quals(version, binary=CHECKERS_QUALS, dest_repo=MAVEN_REPO):
    return mvn_deploy('checkers-quals', binary, version, dest_repo)

def execute(command_args, halt_if_fail=True):
    import shlex
    if isinstance(command_args, str):
        arg = shlex.split(command_args)
        r = subprocess.call(arg)
    else:
        r = subprocess.call(command_args)
    if halt_if_fail and r:
        raise Exception('Found an error: %s' % r)
    return r

class Usage(Exception):
    def __init__(self, msg):
        self.msg = msg

USER = os.getlogin()
DRY_RUN_LINK = 'http://www.cs.washington.edu/homes/%s/jsr308test/jsr308/' % USER

def main(argv=None):
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
    site_copy_if_needed()
    make_release(next_version)
    checklinks(DRY_RUN_LINK)

    print("Pushed to %s" % DRY_RUN_LINK)
    raw_input("Please check the site.  DONE?")

    # Making the real release
    make_release(next_version, real=True)

    # Make maven release
    mvn_deploy_jsr308_all(next_version)
    mvn_deploy_quals(next_version)

    checklinks(DEFAULT_SITE)

    print("Pushed to %s" % DEFAULT_SITE)
    raw_input("Please check the site.  DONE?")

    commit_and_push()
    print("You have just made the release.  Please announce it to the world")

if __name__ == "__main__":
    sys.exit(main())

