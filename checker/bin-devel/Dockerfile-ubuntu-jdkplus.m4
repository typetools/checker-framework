# This Dockerfile creates a layer that later Dockerfiles can use.

# "ubuntu" is the latest LTS release.  "ubuntu:rolling" is the latest release.
# Both might lag behind; as of 2024-11-16, ubuntu:rolling was still 24.04 rather than 24.10.
FROM ubuntu
include(`Dockerfile-contents-ubuntu-base.txt')

include(`Dockerfile-contents-ubuntu-plus.txt')
