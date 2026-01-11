# Dependencies for developing the Checker Framework.
RUN export DEBIAN_FRONTEND=noninteractive \
&& apt -qqy update \
&& apt -qqy install \
  autoconf \
  devscripts \
  dia \
  graphviz \
  hevea \
  imagemagick \
  junit \
  latexmk \
  librsvg2-bin \
  libasound2-dev libcups2-dev libfontconfig1-dev \
  libx11-dev libxext-dev libxrender-dev libxrandr-dev libxtst-dev libxt-dev \
  pdf2svg \
  rsync \
  shellcheck \
  shfmt \
  texlive-font-utils \
  texlive-fonts-recommended \
  texlive-latex-base \
  texlive-latex-extra \
  texlive-latex-recommended \
  yamllint

RUN export DEBIAN_FRONTEND=noninteractive \
&& apt -qqy update \
&& apt -qqy install \
  npm
RUN export DEBIAN_FRONTEND=noninteractive \
&& npm install markdownlint-cli2 --global

# Install uv (manages Python dependencies).
RUN export DEBIAN_FRONTEND=noninteractive \
&& wget -qO- https://astral.sh/uv/install.sh | sh \
&& chmod +rx /root \
&& find /root/.local -exec chmod +r {} \; \
&& find /root/.local -type d -exec chmod +x {} \; \
&& find /root/.local/bin -type f -exec chmod +x {} \;
ENV PATH="/root/.local/bin:$PATH"
