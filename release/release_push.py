#!/usr/bin/env python
# encoding: utf-8
"""
release_push.py

Created by Jonathan Burke on 2013-12-30.

Copyright (c) 2013 University of Washington. All rights reserved.
"""

from release_vars  import *
from release_utils import *
import urllib
import zipfile

#ensure that the latest built version is
def check_release_version( previous_release, new_release ):
    if version_to_integer( previous_release ) >= version_to_integer( current_release ):
        raise Exception( "Previous release version ( " + previous_release + " ) should be less than " +
                         "the new release version( " + new_release + )" )

def run_sanity_check( new_checkers_release_zip ):
    cmd = "mkdir -p " + SANITY_DIR
    execute( cmd )

    sanity_zip = os.path.join( SANITY_DIR, "checkers.zip" )

    sanity_checkers_dir = os.path.join( SANITY_DIR, "checkers" )
    delete_path("checkers")

    download_binary( new_checkers_release_zip, sanity_zip, MAX_DOWNLOAD_SIZE )

    with zipfile.ZipFile(sanity_zip, "r") as z:
        z.extractall(sanity_checkers_dir)

    sanity_javac = os.path.join( sanity_checkers_dir, "binary", "javac" )
    nullness_example = os.path.join( sanity_checkers_dir, "examples", "NullnessExamplesWithWarnings.java" )
    nullness_output  = os.path.join( sanity_checkers_dir, "output.log" )

    cmd = sanity_javac + " --processor checkers.nullness.NullnessChecker " + nullness_example + " -Anomsgtext &> " + nullness_output
    execute( cmd, false )

    expected_errors = [ "NullnessExampleWithWarnings.java:25: error: (assignment.type.incompatible)",
                        "NullnessExampleWithWarnings.java:36: error: (argument.type.incompatible)"  ]
    found_errors = are_in_file( nullness_output, expected_errors )

    if not found_errors:
        raise Exception( "Sanity check did not work!\n" +
                         "File: " + nullness_output + "\n" +
                         "should contain the following errors: [ " + ", ".join(expected_errors) )

def copy_releases_to_live_site():


def update_release_symlinks():


def ensure_group_access_to_releases():


def push_maven_plugin_to_release_repo():

def push_interm_to_release_repos():


def continue_or_exit( msg ):
    continue_script = prompt_w_suggestion(msg + " Continue?", "yes", "^(Yes|yes|No|no)$")
    if continue_script == "no" or continue_script == "No":
                raise Exception( "User elected NOT to continue at prompt: " + msg )

def main(argv):
    dev_checker_website  = os.path.join( HTTP_PATH_TO_DEV_SITE, "checker-framework" )
    live_checker_website = os.path.join( HTTP_PATH_TO_LIVE_SITE, "checker-framework" )
    current_version = current_distribution_by_website( live_checker_website )
    new_version     = current_distribution( CHECKER_FRAMEWORK )
    check_release_versions( current_version, new_version )

    new_checkers_release_zip = os.path.join( dev_checker_website, "current", "checkers.zip" )

    continue_or_exit( "Please do one of the following:\n " +
                      " 1) Check that the Jenkins release_build job has run since the most recent Mercurial pushes (<<add Jenkins URL>>).\n" +
                      " 2) If release_build was not run recently, in /scratch/jsr308-release/scripts, exit this script and run release_build.py\n"
    )
    continue_or_exit( "Please check the link checker results at <add location>" )
    continue_or_exit( "Please check that the tutorial works (using the new release instead of current)." )

    continue_script = prompt_w_suggestion(" Run a sanity check?", "yes", "^(Yes|yes|No|no)$")
    if continue_script == "yes" or continue_script == "Yes":
        run_sanity_check( new_checkers_release_zip )

    copy_releases_to_live_site()
    ensure_group_access_to_releases()
    #update_release_symlinks()

    #push_maven_artifacts_to_release_repo()


    #push_interm_to_release_repos()

    continue_or_exit( "Please follow these instructions to release the Eclipse plugin:\n<path to Eclipse release instructions>\n" )
    continue_or_exit( "Please log in to google code and mark all issues that were 'pushed' to 'fixed'." )
    continue_or_exit( "Please announce the release using the email structure below:\n" +
                       get_announcement_email( jsr308_version ) )

    #ask the user to release the Eclipse plugin to the appropriate directory
    #ask the user to send email to the appropriate email lists

if __name__ == "__main__":
    sys.exit(main(sys.argv))