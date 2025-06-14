# Create a Docker image that is ready to run the main Checker Framework tests,
# using JDK 21.

define(`UBUNTUVERSION', ubuntu:rolling)
include(`Dockerfile-ubuntu-base-contents.txt')

RUN export DEBIAN_FRONTEND=noninteractive \
&& apt -qqy update \
&& apt -y install \
  openjdk-21-jdk

RUN export DEBIAN_FRONTEND=noninteractive \
&& apt autoremove \
&& apt clean \
&& rm -rf /var/lib/apt/lists/*
