#!/bin/sh

curl https://checker-framework.googlecode.com/hg/release/release_build.py &> release_build.py
curl https://checker-framework.googlecode.com/hg/release/release_push.py  &> release_push.py
curl https://checker-framework.googlecode.com/hg/release/release_utils.py &> release_utils.py
curl https://checker-framework.googlecode.com/hg/release/release_vars.py  &> release_vars.py