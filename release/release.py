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

def check_command(command):
    p = execute(['which', command])
    if p:
        raise AssertionError('command not found: %s' % command)    

DEFAULT_PATHS = (
    '/homes/gws/mernst/research/invariants/scripts',
    '/homes/gws/mernst/bin/share',
    '/homes/gws/mernst/bin/Linux-i686',
    '/uns/bin',
)

def append_to_PATH(paths=DEFAULT_PATHS):
    current_PATH = os.getenv('PATH')
    new_PATH = current_PATH + ':' + ':'.join(paths)
    os.environ['PATH'] = new_PATH

REPO_ROOT = os.path.dirname(os.path.dirname(__file__))
PROJECT_ROOTS = (
    REPO_ROOT,
    os.path.join(REPO_ROOT, 'jsr308-langtools'),
)
def update_projects(paths=PROJECT_ROOTS):
    for path in PROJECT_ROOTS:
        execute('hg -R %s pull -u' % path)

EDITOR = 'vim'
CHECKERS_CHANGELOG = os.path.join(REPO_ROOT, 'checkers', 'changelog-checkers.txt')
def edit_checkers_changelog(path=CHECKERS_CHANGELOG):
    raw_input("About to edit the Checker Framework changelog.  OK?")
    execute([EDITOR, path])

LANGTOOLS_CHANGELOG = os.path.join(REPO_ROOT, 'jsr308-langtools', 'doc', 'changelog-jsr308.txt')
def edit_langtools_changelog(path=LANGTOOLS_CHANGELOG):
    latest_jdk = latest_openjdk()
    print("Latest OpenJDK release is b%s" % latest_jdk)
    raw_input("About to edit the JSR308 langtools changelog.  OK?")
    execute([EDITOR, path])

def make_release(version, real=False, sanitycheck=True):
    command = 'ant -f release.xml %s -Drelease.ver=%s clean web %s' % (
        '-Drelease.is.real=true' if real else '',
        version,
        'sanitycheck' if sanitycheck else '',
    )
    print("Actually making the release")
    execute(command)

def execute(command_args):
    import shlex
    if isinstance(command_args, str):
        arg = shlex.split(command_args)
        subprocess.call(arg)
    else:
        subprocess.call(command_args)


class Usage(Exception):
    def __init__(self, msg):
        self.msg = msg

def main(argv=None):
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
    
    edit_checkers_changelog()
    edit_langtools_changelog()

    # Making the first release
    make_release(next_version)

if __name__ == "__main__":
    sys.exit(main())
