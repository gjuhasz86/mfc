FROM openjdk:8

RUN apt-get update
RUN apt-get install -y apt-transport-https
RUN apt-get install -y nodejs npm

RUN echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list
RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823
RUN apt-get update
RUN apt-get install -y sbt

RUN mkdir repo

WORKDIR ./repo
RUN git clone https://github.com/gjuhasz86/mfc.git .

RUN sbt compile
RUN sbt npmInstall

CMD ["bash"]