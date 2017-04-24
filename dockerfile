FROM dockerfile/java

RUN \
  apt-get install -y sbt npm

CMD ["bash"]