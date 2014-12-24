FROM jenkins
MAINTAINER Dennis Schulte mail@dennis-schulte.de


USER root
RUN apt-get update 
RUN apt-get install -y maven
USER jenkins

COPY plugins.txt /plugins.txt
#ADD jobs/ /var/jenkins_home/jobs/

RUN /usr/local/bin/plugins.sh /plugins.txt
RUN curl -L https://github.com/steve-jansen/job-dsl-plugin/releases/download/JENKINS-21750-pre-release/job-dsl-jenkins-21750.hpi -o /usr/share/jenkins/ref/plugins/job-dsl-jenkins.hpi;

