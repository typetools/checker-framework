#Git Guide For checker-framework

Want to contribute to checker-framework, but never used git before? Or want to know how the checker-framework leaders want things done? Then this guide is for you!

##Create GitHub account
First, set up a GitHub account if you don't already have one at https://github.com/signup/free/.

##Set Up Git
Here are instructions for setting up git for [Linux](http://help.github.com/linux-set-up-git/), [OSX](http://help.github.com/mac-set-up-git/), and [Windows](http://help.github.com/win-set-up-git/).

The Linux page covers installing with Synaptic (Debian/Ubuntu), but also includes important set up instructions for any distribution. Use whatever package manager your distribution uses (Gentoo example: `$ sudo emerge -av dev-vcs/git`).

##Set Up Your Fork

Fork checker-framework by going to https://github.com/typetools/checker-framework/ and clicking on "Fork" towards the top right. In a few seconds you'll have your own repo, but it's all still just online.

To clone your repo locally, enter

    $ git clone git@github.com:USERNAME/checker-framework.git

This will create a directory called "checker-framework" in your current directory. `USERNAME` is your github username (most occurances of code in CAPS in this guide is meant as a variable, with the exceptions of HEAD which is git-specific). In order for your code to keep up with checker-framework's development, you need to configure another remote. The default remote named "origin" points to your fork on GitHub. To keep track of the original repo, add a remote named "upstream":

    $ cd checker-framework
    $ git remote add upstream https://github.com/typetools/checker-framework/
    $ git fetch upstream

Everything is now set up!

See http://help.github.com/fork-a-repo/ for more information.

###Working with branches

Git starts out in the master branch, but this should be left alone in order to be updated with checker-framework's master branch. Each
feature or bug fix you do should get it's own branch. As said by many experts: "If in doubt, create a branch :)". So, to create a new branch named `NEWFEATURE` enter

    $ git branch NEWFEATURE  

and to switch to that branch enter

    $ git checkout NEWFEATURE

You can also combine the above with

    $ git checkout -b NEWFEATURE

Now any changes you make will be isolated to this branch. Running `$ git branch` will show you what branches you have, and which one is active.

###Installing

Check the [Installation](https://checkerframework.org/manual/#installation).

###Committing changes

Once you're happy, you need to add and commit your changes.

To add changed files enter

    $ git add --interactive

The staged column shows how many changes have been added. The unstaged column shows how many changes have not been added. Enter `u` to update, then to add files 1, 2 and 4 enter `1 2 4`, leaving 3 unstaged.
Press enter again, then `q` to quit.

A shortcut to add all _changed_ files is

    $ git add -u

If you need to add new files enter

    $ git add FILE1 FILE2 ...

Once you're ready to commit these changes enter

    $ git commit

and enter a descriptive summary of your modifications and save the file.

You can also combine "git add --interactive" and "git commit" with

    $ git commit --interactive

Once a commit is done (i.e. you should be really happy with it) you can push it to your fork with

    $ git push origin NEWFEATURE

Now everyone has access to your new feature or bug fix! But if you actually want it included with checker-framework and not just your fork, you need to send a pull request.

###Send A Pull Request
Sending a pull request lets the main developers of checker-framework know that you think you have something useful to add. On your forked project's main page you need to change the current branch to "NEWFEATURE", then click on "Pull Request". Give a descriptive title to the request, and any more necessary information in the body. Click on "Commits" and "Files Changed" to review what is being sent, and then back to "Preview Discussion". When you're satisfied, click on "Send pull request" and the people to the right will be notified of your contribution. Someone might comment on things that need to be changed, or clarified, or anything else. You'd then go back to your branch, make the changes, commit it (with a new note on what this commit changes) and push it to your fork. This new commit will automatically show up on the pull request.

###Updating your master to checker-framework's master

So, you've forked checker-framework and cloned your fork, maybe have your own branches, but you haven't changed
your master, and some time later checker-framework is ahead of your master. Now you need your own master
to be up to date so you can make changes where code has been changed by others (or even a pull   
request of yours that was merged!).

First, make sure you're in master:

    $ git checkout master

Next, fetch it:

    $ git fetch upstream

Now, merge the changes from upstream/master to your local master:

    $ git merge upstream/master

You can also combine "git fetch upstream" and "git merge upstream/master" with

    $ git pull upstream master

Finally, update your repo's master:

    $ git push origin master

Now you can create new branches from the updated master. Yay!

##Some Other Useful Commands

To merge branch `NEWFEATURE` with branch `OTHER`:

    $ git checkout OTHER
    $ git merge NEWFEATURE

To delete a branch:

    $ git branch -d NEWFEATURE

To delete a branch from GitHub:

    $ git push origin :NEWFEATURE

To rebase your commits on top of other developers commits(while syncing a fork):

    $ git pull --rebase upstream master

To merge the last two commits together (only if you haven't pushed your commit, or [BadThingsWillHappen](http://help.github.com/rebase/)):

    $ git rebase -i HEAD~2

or to update a commit (same warning):

    $ git commit --amend  

To save your work when you're not ready to make a commit:

    $ git stash save

See http://ariejan.net/2008/04/23/git-using-the-stash/ on what to do with the stash.

##Notice
This is just a small Git Guide to help get you started on working with Git and Github. To get detailed knowledge and become an expert see https://www.atlassian.com/git/tutorials/
