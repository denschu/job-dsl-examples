#!/bin/bash

# The Base URL to detect the projects from
REPOSITORY=https://github.com/codecentric/spring-samples

# Check access
svn info ${REPOSITORY}
if [ "$?" != "0" ]; then
  echo "Failed to connect to SCM"
  exit 1
fi


echo "Detecting projects in SCM Repository: " ${REPOSITORY}
for project in $(svn ls ${REPOSITORY}/trunk); do
	echo "project: " ${project}
done