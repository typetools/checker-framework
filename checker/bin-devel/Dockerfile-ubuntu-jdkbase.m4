# This Dockerfile creates a layer that later Dockerfiles can use.

define(`UBUNTUVERSION', ubuntu)
include(`Dockerfile-ubuntu-base-contents.txt')
