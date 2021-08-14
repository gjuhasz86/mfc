FROM openjdk:8

RUN apt-get update
RUN apt-get install -y apt-transport-https

RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list
RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | tee /etc/apt/sources.list.d/sbt_old.list
RUN curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add

RUN apt-get update
RUN apt-get install -y sbt
RUN sbt --version

RUN mkdir /nodeinst /repo
WORKDIR /nodeinst
RUN wget https://nodejs.org/download/release/v7.9.0/node-v7.9.0-linux-x64.tar.xz
RUN tar -xvf node-v7.9.0-linux-x64.tar.xz
RUN cp -r node-v7.9.0-linux-x64/bin node-v7.9.0-linux-x64/include node-v7.9.0-linux-x64/lib node-v7.9.0-linux-x64/share /usr/

WORKDIR /repo
ADD . /repo

RUN node -v && npm -version
RUN sbt compile
RUN sbt npmInstall

CMD ["bash"]
