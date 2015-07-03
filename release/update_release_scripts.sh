#!/bin/sh

curl https://raw.githubusercontent.com/typetools/checker-framework/master/release/release_build.py &> release_build.py
curl https://raw.githubusercontent.com/typetools/checker-framework/master/release/release_push.py  &> release_push.py
curl https://raw.githubusercontent.com/typetools/checker-framework/master/release/release_utils.py &> release_utils.py
curl https://raw.githubusercontent.com/typetools/checker-framework/master/release/release_vars.py  &> release_vars.py