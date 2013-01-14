#!/usr/bin/env python
# encoding: utf-8
"""
releaseutils.py

Python Utils for releasing the Checker Framework
This contains no main method only utility functions 
Created by Jonathan Burke 11/21/2012

Copyright (c) 2012 University of Washington
"""

from xml.dom import minidom
import sys
import getopt
import urllib2
import re
import subprocess
from subprocess import Popen, PIPE
import os
import pwd

#=========================================================================================
# Command utils

#Execute the given command
def execute(command_args, halt_if_fail=True, capture_output=False):
    print("Executing: %s" % (command_args))
    import shlex
    args = shlex.split(command_args) if isinstance(command_args, str) else command_args

    if capture_output:
        return subprocess.Popen(args, stdout=subprocess.PIPE).communicate()[0]
    else:
        r = subprocess.call(args)
        if halt_if_fail and r:
            raise Exception('Error %s while executing %s' % (r, command_args))
        return r

def check_command(command):
    p = execute(['which', command], False)
    if p:
        raise AssertionError('command not found: %s' % command)
        
#=========================================================================================
# Version Utils
def current_distribution(site):
    """
    Reads the checker framework version from the checker framework website and
    returns the version of the current release
    """
    ver_re = re.compile(r"<!-- checkers-version -->(.*),")
    text = urllib2.urlopen(url=site).read()
    result = ver_re.search(text)
    return result.group(1)
    
def latest_openjdk(site):
    ver_re = re.compile(r"Build b(\d+)")
    text = urllib2.urlopen(url=site).read()
    result = ver_re.search(text)
    return result.group(1)
    
def increment_version(version):
    """
    Returns a recommendation of the next incremental version based on the
    passed one.

    >>> increment_version('1.0.3')
    '1.0.4'
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
    
#=========================================================================================
# Mercurial Utils

#Pull the latest changes and update
def update_projects(paths):
    for path in paths:
        execute('hg -R %s pull' % path)
        execute('hg -R %s update' % path)
        print("Checking changes")
        # execute('hg -R %s outgoing' % path)

#Commit the changes we made for this release
#Then add a tag for this release
#And push these changes 
def commit_tag_and_push(version, path, tag_prefix):
    execute('hg -R %s commit -m "new release %s"' % (path, version))
    execute('hg -R %s tag %s%s' % (path, tag_prefix, version))
    execute('hg -R %s push' % path)

#Give group access to the specified path
def ensure_group_access(path):
    # Errs for any file not owned by this user.
    # But, the point is to set group writeability of any *new* files.
    execute('chmod -f -R g+w %s' % path, halt_if_fail=False)
    
# Retrive the changes since the tag (prefix + prev_version)
def retrieve_changes(root, prev_version, prefix):
    return execute(
            "hg -R %s log -r %s%s:tip --template ' * {desc}\n'" %
                (root, prefix, prev_version),
                capture_output=True)

#=========================================================================================
# File Utils
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

def first_line_containing(value, file):
    p1 = Popen(["grep", "-m", "1", "-n", value, file], stdout=PIPE)
    p2 = Popen(["sed", "-n", 's/^\\([0-9]*\\)[:].*/\\1/p'], stdin=p1.stdout, stdout=PIPE)
    return int(p2.communicate()[0])
                    
#=========================================================================================
# Change Log utils

#Read every line in the file until we reach a line with at least 8 dashes("-") in it
def changelog_header(filename):
    f = open(filename, 'r')
    header = []

    for line in f:
        if '-------' in line:
            break
        header.append(line)

    return ''.join(header)
    
def get_changelog_date():
    import datetime
    return datetime.datetime.now().strftime("%d %b %Y")

#Ask the user whether they want to edit the changelog
#Prepend a changelog message to the changelog and then open the passed in editor
def edit_changelog(projectName, path, version, description, editor):
    edit = raw_input("Edit the %s changelog? [Y/n]" % projectName)
    if not (edit == "n"):
        if not file_contains(path, version):
            file_prepend(path, description)
            execute([editor, path])


#Create a message to add to the current checker framework change log
def make_checkers_change_desc(version, changes):
    """Version %s, %s


%s

----------------------------------------------------------------------
"""  % (version, get_changelog_date(), changes)


#Create a message to add to the current JSR308 change log
def make_jsr308_change_desc(version, changes, latest_jdk):
    """Version %s, %s

Base build
  Updated to OpenJDK langtools build b%s

%s

----------------------------------------------------------------------
""" % (version, get_changelog_date(), latest_jdk, changes)
    
#Checker Framework Specific Change log method
#Queries whether or not the user wants to update the checker framework changelog
#then opens the changelog in the supplied editor
def edit_checkers_changelog(version, path, editor, changes=""):
    desc = make_checkers_change_desc(version, changes)
    edit_changelog("Checker Framework", path, version, desc, editor)
    

#JSR308 Specific Change log method
#Queries whether or not the user wants to update the checker framework changelog
#then opens the changelog in the supplied editor
def edit_langtools_changelog(version, openJdkReleaseSite, path, editor, changes=""):
    latest_jdk = latest_openjdk(openJdkReleaseSite)
    print("Latest OpenJDK release is b%s" % latest_jdk)
    desc = make_jsr308_change_desc(version, changes, latest_jdk)
    edit_changelog("JSR308 Langtools", path, version, desc, editor)

#=========================================================================================
# Maven Utils

def mvn_deploy_file(name, binary, version, dest_repo, group_id, pom=None):
    pomOps = "" 
    if pom is None:
        pomOps= "-DgeneratePom=true"
    else:
        pomOps="""-DgeneratePom=false -DpomFile=%s""" % (pom)

    command = """
    mvn deploy:deploy-file
        -DartifactId=%s
        -Dfile=%s
        -Dversion=%s
        -Durl=%s
        -DgroupId=%s
        -Dpackaging=jar
        """ % (name, binary, version, dest_repo, group_id) + pomOps
    return execute(command)

def mvn_deploy(binary, pom, url):
    command = """
    mvn deploy:deploy-file
        -Dfile=%s
        -DpomFile=%s
        -DgeneratePom=false
        -Durl=%s
        """ % (binary, pom, url)
    return execute(command)

def mvn_install(pluginDir):
    pom=pluginDirToPom(pluginDir)
    execute("mvn -f %s clean package" % pom)
    execute("mvn -f %s install" % pom)

def pluginDirToPom(pluginDir):
    return os.path.join(pluginDir, 'pom.xml')

def mvn_plugin_version(pluginDir):
    "Extract the version number from pom.xml in pluginDir/pom.xml"
    pomLoc = pluginDirToPom(pluginDir) 
    dom = minidom.parse(pomLoc)
    version = ""
    project = dom.getElementsByTagName("project").item(0)
    for i in range( 0, project.childNodes.length):
        childNode = project.childNodes.item(i)
        if childNode.nodeName == 'version':
            if childNode.hasChildNodes():
                version = childNode.firstChild.nodeValue
            else: 
                print "Empty Maven plugin version!"
                sys.exit(1)
            break
    else:
        print "Could not find Maven plugin version!"
        sys.exit(1)
    return version

def mvn_deploy_mvn_plugin(pluginDir, pom, version, mavenRepo):
    jarFile = "%s/target/checkers-maven-plugin-%s.jar" % (pluginDir, version)
    return mvn_deploy(jarFile, pom, mavenRepo)

#=========================================================================================
# Ant utils

def site_copy(ant_args):
    execute("ant -e -f release.xml %s site-copy" % ant_args)

def make_release(version, ant_args, real=False, sanitycheck=True):
    command = 'ant -e -f release.xml %s -Drelease.ver=%s %s clean web %s' % (
        '-Drelease.is.real=true' if real else '',
        version,
        ant_args,
        'sanitycheck' if sanitycheck else '',
    )
    print("Actually making the release")
    return execute(command)


#=========================================================================================
# Misc. Utils

def getAndAppend(name, append):
    if os.environ.has_key(name):
        return os.environ[name] + append

    else:
        return ""

def append_to_PATH(paths):
    current_PATH = os.getenv('PATH')
    new_PATH = current_PATH + ':' + ':'.join(paths)
    os.environ['PATH'] = new_PATH

def checklinks(makeFile, site_url=None):
    os.putenv('jsr308_www_online', site_url) # set environment var for subshell
    return execute('make -f %s checklinks' % makeFile, halt_if_fail=False)
    
#Output email announcement template
def format_email(version, to, checkersLog, jsr308Log, checkers_header=None, langtools_header=None):
    if checkers_header == None:
        checkers_header = changelog_header(checkersLog)
    if langtools_header == None:
        langtools_header = changelog_header(jsr308Log)

    template = """
=================== BEGINING OF EMAIL =====================

To:  %s
Subject: Release %s of the Checker Framework and Type Annotations compiler

We have released a new version of the Type Annotations (JSR 308) compiler,
the Checker Framework, and the Eclipse plugin for the Checker Framework.
 * The Type Annotations compiler supports the type annotation syntax that is
   planned for a future version of the Java language.
 * The Checker Framework lets you create and/or run pluggable type-checkers,
   in order to detect and prevent bugs in your code.  
 * The Eclipse plugin makes it more convenient to run the Checker Framework.

You can find documentation and download links for these projects at:
http://types.cs.washington.edu/jsr308/

Notable changes include:
[[ FILL ME HERE ]]

Changes for the Checker Framework
%s
Changes for the Type Annotations Compiler
%s

=================== END OF EMAIL ==========================
    """ % (to, version, checkers_header, langtools_header)
    return template
