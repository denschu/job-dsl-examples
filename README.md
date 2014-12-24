

# Installation

## Build and run Docker image

```shell
docker build -t denschu/jenkins .
docker run -p 8080:8080 -u root -v /opt/jenkins_home:/var/jenkins_home denschu/jenkins
docker run -p 8080:8080 -u root -i -t -v /opt/jenkins_home:/var/jenkins_home denschu/jenkins bash
```

## Open Jenkins UI in webbrowser

On a Mac you can get the ip address with the following command:

```shell
boot2docker ip
```

```shell
http://192.168.59.103:8080/
```

## Add Maven path in Jenkins configuration

```shell
/usr/share/maven/
```

## Create Generator Job

1. Create a freestyle jenkins job and call it "job-generator"
2. Add build step "Process Job DSLs"
3. Select the radio button "Use the provided DSL script"
4. Copy the contents of "job-dsl-example.groovy" into the text field
5. Save
6. Run


# Job Steps

## Build

1. Jenkins triggers build process on SCM commit
2. Execute normal build process

```shell
mvn clean package
```

## Release

1. User triggers Release-Build
2. Replace SNAPSHOT-Version in pom.xml

```shell
mvn build-helper:parse-version versions:set -DnewVersion=${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.incrementalVersion}-${BUILD_NUMBER}
```

2. Execute normal build process

```shell
mvn clean package
```

3. Deploy artifact to artifact repository

```shell
mvn deploy
```

4. Tag version in SCM

```shell
mvn scm:tag
```

## Deploy

1. Select a build/release
2. Promote the build to a defined promotion level
3. The corresponding deploy-job will be executed

# Job DSL Examples

## Build/Release Job

```shell
def repository = 'codecentric/spring-samples'
def contentApi = new URL("https://api.github.com/repos/${repository}/contents")
def projects = new groovy.json.JsonSlurper().parse(contentApi.newReader())
projects.each { 
    def projectName = it.name
    job(type: Maven) {
        name("${projectName}")
        triggers { scm("*/5 * * * *") }
        scm {
			git {
			    remote {
			        url("https://github.com/codecentric/spring-samples")
			    }
			    createTag(false)
			}
		}
		rootPOM("${projectName}/pom.xml")
		goals("clean package")
		wrappers {
			preBuildCleanup()
			release {
				preBuildSteps {
					maven {
						mavenInstallation("Maven 3.0.4")
						goals("build-helper:parse-version")
						goals("versions:set")
						property("newVersion", "\${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.incrementalVersion}-\${BUILD_NUMBER}")
					}
				}
				postSuccessfulBuildSteps {
					maven {
						goals("deploy")
					}
					maven {
						goals("scm:tag")
					}
					downstreamParameterized {
						trigger("deploy-application") {
							predefinedProp("STAGE", "development")
						}
					}
				}
			}
		}		
		publishers {
			groovyPostBuild("manager.addShortText(manager.build.getEnvironment(manager.listener)[\'POM_VERSION\'])")
		}
		promotions {
			promotion("Development") {
				icon("star-red")
				conditions {
					manual('')
				}
				actions {
					downstreamParameterized {
						trigger("deploy-application","SUCCESS",false,["buildStepFailure": "FAILURE","failure":"FAILURE","unstable":"UNSTABLE"]) {
							predefinedProp("ENVIRONMENT","test-server")
							predefinedProp("APPLICATION_NAME", "\${PROMOTED_JOB_FULL_NAME}")
							predefinedProp("BUILD_ID","\${PROMOTED_NUMBER}")
						}
					}
				}
			}
			promotion("QA") {
				icon("star-yellow")
				conditions {
					manual('')
					upstream("Development")
				}
				actions {
					downstreamParameterized {
						trigger("deploy-application","SUCCESS",false,["buildStepFailure": "FAILURE","failure":"FAILURE","unstable":"UNSTABLE"]) {
							predefinedProp("ENVIRONMENT","qa-server")
							predefinedProp("APPLICATION_NAME", "\${PROMOTED_JOB_FULL_NAME}")
							predefinedProp("BUILD_ID","\${PROMOTED_NUMBER}")
						}
					}
				}
			}	
			promotion("Production") {
				icon("star-green")
				conditions {
					manual('')
					upstream("QA")
				}
				actions {
					downstreamParameterized {
						trigger("deploy-application","SUCCESS",false,["buildStepFailure": "FAILURE","failure":"FAILURE","unstable":"UNSTABLE"]) {
							predefinedProp("ENVIRONMENT","prod-server")
							predefinedProp("APPLICATION_NAME", "\${PROMOTED_JOB_FULL_NAME}")
							predefinedProp("BUILD_ID","\${PROMOTED_NUMBER}")
						}
					}
				}
			}							
		}		
	}	
}

## Deploy Job

job{
    name("deploy-application")
}
```

# Used Plugins

## Jenkins Plugins

https://wiki.jenkins-ci.org/display/JENKINS/Release+Plugin
https://wiki.jenkins-ci.org/display/JENKINS/Groovy+Postbuild+Plugin
https://wiki.jenkins-ci.org/display/JENKINS/Promoted+Builds+Plugin
https://wiki.jenkins-ci.org/display/JENKINS/Job+DSL+Plugin

## Maven Plugins

http://mojo.codehaus.org/build-helper-maven-plugin/parse-version-mojo.html
http://mojo.codehaus.org/versions-maven-plugin/set-mojo.html
http://maven.apache.org/scm-archives/scm-LATEST/maven-scm-plugin/tag-mojo.html






