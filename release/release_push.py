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
    if version_to_integer( previous_release ) >= version_to_integer( new_release ):
        raise Exception( "Previous release version ( " + previous_release + " ) should be less than " +
                         "the new release version( " + new_release + " )" )

def run_sanity_check( new_checkers_release_zip, release_version ):
    cmd = "mkdir -p " + SANITY_DIR
    execute( cmd )

    sanity_zip = os.path.join( SANITY_DIR, "checkers.zip" )

    print( "Attempting to download %s to %s" % ( new_checkers_release_zip, sanity_zip ) )
    download_binary( new_checkers_release_zip, sanity_zip, MAX_DOWNLOAD_SIZE )

    deploy_dir = os.path.join( SANITY_DIR, "checker-framework-" + release_version )

    if os.path.exists( deploy_dir ):
        print( "Deleting existing path: " + deploy_dir )
        delete_path(deploy_dir)

    with zipfile.ZipFile(sanity_zip, "r") as z:
        z.extractall( SANITY_DIR )

    cmd = "chmod -R u+rwx " + deploy_dir
    execute( cmd )

    sanity_javac = os.path.join( deploy_dir, "binary", "javac" )
    nullness_example = os.path.join( deploy_dir, "examples", "NullnessExampleWithWarnings.java" )
    nullness_output  = os.path.join( deploy_dir, "output.log" )

    cmd = sanity_javac + " -processor checkers.nullness.NullnessChecker " + nullness_example + " -Anomsgtext &> " + nullness_output
    execute( cmd, False )

    expected_errors = [ "NullnessExampleWithWarnings.java:25: error: (assignment.type.incompatible)",
                        "NullnessExampleWithWarnings.java:36: error: (argument.type.incompatible)"  ]
    found_errors = are_in_file( nullness_output, expected_errors )

    if not found_errors:
        raise Exception( "Sanity check did not work!\n" +
                         "File: " + nullness_output + "\n" +
                         "should contain the following errors: [ " + ", ".join(expected_errors) )
    else:
        print( "\nsanity check - zip file: passed!\n" )

def copy_release_dir( path_to_dev, path_to_live, release_version ):
    source_location = os.path.join( path_to_dev, release_version )
    dest_location = os.path.join( path_to_live, release_version )

    if os.path.exists( dest_location ):
        prompt_to_delete( dest_location )

    if os.path.exists( dest_location ):
        raise Exception( "Destination location exists: " + dest_location )

    cmd = "cp -r %s %s" % ( source_location, dest_location )
    execute( cmd )

    return dest_location

def copy_releases_to_live_site( checker_version, afu_version):
    copy_release_dir( JSR308_INTERM_RELEASES_DIR,  JSR308_LIVE_RELEASES_DIR,  checker_version )
    copy_release_dir( CHECKER_INTERM_RELEASES_DIR, CHECKER_LIVE_RELEASES_DIR, checker_version )
    copy_release_dir( AFU_INTERM_RELEASES_DIR, AFU_LIVE_RELEASES_DIR, afu_version )

def update_release_symlinks( checker_version, afu_version ):
    force_symlink( os.path.join( JSR308_LIVE_RELEASES_DIR,  checker_version ), os.path.join( JSR308_LIVE_SITE,  "current" ) )
    force_symlink( os.path.join( CHECKER_LIVE_RELEASES_DIR, checker_version ), os.path.join( CHECKER_LIVE_SITE, "current" ) )
    force_symlink( os.path.join( AFU_LIVE_RELEASES_DIR,     afu_version ),     os.path.join( AFU_LIVE_SITE,     "current" ) )

def ensure_group_access_to_releases():
    ensure_group_access( JSR308_LIVE_RELEASES_DIR )
    ensure_group_access( AFU_LIVE_RELEASES_DIR )
    ensure_group_access( CHECKER_LIVE_RELEASES_DIR )

def push_maven_artifacts_to_release_repo( version ):
    mvn_deploy_mvn_plugin( MAVEN_PLUGIN_DIR, MAVEN_PLUGIN_POM, version, MAVEN_LIVE_REPO )

    # Deploy jsr308 and checker-qual jars to maven repo
    mvn_deploy( CHECKERS_BINARY, CHECKERS_BINARY_POM, MAVEN_LIVE_REPO )
    mvn_deploy( CHECKERS_QUALS,  CHECKERS_QUALS_POM,  MAVEN_LIVE_REPO )
    mvn_deploy( JAVAC_BINARY,    JAVAC_BINARY_POM,    MAVEN_LIVE_REPO )
    mvn_deploy( JDK7_BINARY,     JDK7_BINARY_POM,     MAVEN_LIVE_REPO )
    mvn_deploy( JDK8_BINARY,     JDK8_BINARY_POM,     MAVEN_LIVE_REPO )

def push_interm_to_release_repos():
    hg_push_or_fail( INTERM_JSR308_REPO )
    hg_push_or_fail( INTERM_ANNO_REPO )
    hg_push_or_fail( INTERM_CHECKER_REPO )

def continue_or_exit( msg ):
    continue_script = prompt_w_suggestion(msg + " Continue?", "yes", "^(Yes|yes|No|no)$")
    if continue_script == "no" or continue_script == "No":
                raise Exception( "User elected NOT to continue at prompt: " + msg )

def main(argv):
    dev_checker_website  = os.path.join( HTTP_PATH_TO_DEV_SITE, "checker-framework" )
    live_checker_website = os.path.join( HTTP_PATH_TO_LIVE_SITE, "checker-framework" )
    current_checker_version = current_distribution_by_website( live_checker_website )
    new_checker_version     = current_distribution( CHECKER_FRAMEWORK )
    check_release_version( current_checker_version, new_checker_version )

    #note, get_afu_version_from_html uses the file path not the web url
    dev_afu_website  = os.path.join( FILE_PATH_TO_DEV_SITE, "annotation-file-utilities", "index.html" )
    live_afu_website = os.path.join( FILE_PATH_TO_LIVE_SITE, "annotation-file-utilities", "index.html" )
    current_afu_version = get_afu_version_from_html( live_afu_website )
    new_afu_version     = get_afu_version_from_html( dev_afu_website )
    check_release_version( current_afu_version, new_afu_version )

    new_checkers_release_zip = os.path.join( dev_checker_website, "current", "checkers.zip" )

    continue_or_exit(
        "Please do one of the following:\n" +
        " 1) Check that the Jenkins release_build job has run since the most recent Mercurial pushes (<<add Jenkins URL>>).\n" +
        " 2) If release_build was not run recently, in /scratch/jsr308-release/scripts, exit this script and run release_build.py\n"
    )

    continue_or_exit( "Please check the link checker results at <add location>" )
    continue_or_exit( "Please check that the tutorial works (using the new release instead of current)." )

    run_santity = prompt_w_suggestion(" Run a sanity check?", "yes", "^(Yes|yes|No|no)$")
    if run_santity == "yes" or run_santity == "Yes":
        run_sanity_check( new_checkers_release_zip, new_checker_version )

    copy_releases_to_live_site( new_checker_version, new_afu_version )
    ensure_group_access_to_releases()
    update_release_symlinks( new_checker_version, new_afu_version )

    continue_script = prompt_w_suggestion("Push the release to Google code repositories?  This is irreversible.", "no", "^(Yes|yes|No|no)$")
    if continue_script == "yes" or continue_script == "Yes":
        push_maven_artifacts_to_release_repo( new_checker_version )

    #run maven sanity check

    #push_interm_to_release_repos()

    continue_or_exit( "Please follow these instructions to release the Eclipse plugin:\n<path to Eclipse release instructions>\n" )
    continue_or_exit( "Please log in to google code and mark all issues that were 'pushed' to 'fixed'." )
    continue_or_exit( "Please announce the release using the email structure below:\n" +
                       get_announcement_email( new_checker_version ) )

if __name__ == "__main__":
    sys.exit(main(sys.argv))