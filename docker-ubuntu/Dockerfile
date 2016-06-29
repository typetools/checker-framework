# Create a Docker image that is ready to run the Checker Framework tests.

# I will have multiple variants of this, for different JDK versions,
# with different names.
# Maybe put the apt-get install line for the JDK as a separate RUN command
# and therefore a separate layer??

# Good instructions appear at https://docs.docker.com/engine/tutorials/dockerimages/

# Put this Dockerfile in an empty directory!
# Otherwise all the files in the directory might get included in the Docker image??


# Consider having a smaller Dockerfile that leaves off all the parts
# required for the manual:
#   dia, hevea, librsvg2-bin, texlive-*
# That will leave a more svelte docker image for the main execution.
# (The full one is 2GB.  Is that excessive?)

# Run this command:
#   # No tag number; we'll just depend on the "latest" tag.
#   docker build -t mdernst/ubuntu-for-cf .
#   # List the available images
#   docker images
#   # Upload to Docker Hub
#   docker push mdernst/ubuntu-for-cf

FROM ubuntu
MAINTAINER Michael Ernst <mernst@cs.washington.edu>

# Each RUN command is a different layer, so minimize their number.

# According to
# https://docs.docker.com/engine/userguide/eng-image/dockerfile_best-practices/:
#  * Put "apt-get update" and "apt-get install" in the same RUN command.
#  * Do not run "apt-get upgrade"; instead get upstream to update.
RUN apt-get -qqy update && apt-get -qqy install \
  ant \
  dia \
  git \
  hevea \
  librsvg2-bin \
  make \
  mercurial \
  texlive-fonts-recommended \
  texlive-latex-base \
  texlive-latex-extra \
  texlive-latex-recommended \
  unzip \
  default-jdk \
&& apt-get clean \
&& rm -rf /var/lib/apt/lists/*
