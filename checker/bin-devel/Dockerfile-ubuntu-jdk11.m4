# Create a Docker image that is ready to run the main Checker Framework tests,
# using JDK 11.

define(`UBUNTUVERSION', ubuntu)
include(`Dockerfile-ubuntu-base-contents.txt')

RUN export DEBIAN_FRONTEND=noninteractive \
&& apt -qqy update \
&& apt -y install \
  openjdk-11-jdk \
&& update-java-alternatives -s java-1.11.0-openjdk-amd64

RUN export DEBIAN_FRONTEND=noninteractive \
&& apt autoremove \
&& apt clean \
&& rm -rf /var/lib/apt/lists/*
