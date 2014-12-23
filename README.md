

# Voraussetzungen

# Installation

## Run Docker image

```shell
docker build -t denschu/jenkins .
docker run -p 8080:8080 -u root -v /tmp/jenkins_home:/var/jenkins_home denschu/jenkins
boot2docker ip
```

## Open URL in browser

```shell
http://192.168.59.103:8080/
```

## Add Maven path in http://192.168.59.103:8080/configure

```shell
/usr/share/maven/
```

# Steps

## Build

1. Jenkins triggers build process on SCM commit
2. Execute normal build process ("mvn clean package")

## Release

1. User triggers Release-Build aus (Release Button)
2. Pre Release Step: Replace SNAPSHOT-Version in pom.xml ("mvn build-helper:parse-version versions:set -DnewVersion=${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.incrementalVersion}-${BUILD_NUMBER}")
2. Execute normal build process ("mvn clean package")
3. Post Release Step: Deploy artifact to Maven repository (mvn deploy)
4. Tag version in SCM (mvn scm:tag)
5. Groovy Postbuild: manager.addShortText(manager.build.getEnvironment(manager.listener)['POM_VERSION'])
6. Trigger Deploy Job

## Deploy


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

## Maven Plugins

http://mojo.codehaus.org/build-helper-maven-plugin/parse-version-mojo.html
http://mojo.codehaus.org/versions-maven-plugin/set-mojo.html
http://maven.apache.org/scm-archives/scm-LATEST/maven-scm-plugin/tag-mojo.html

# Release-Job DSL für Jenkins
https://github.com/jenkinsci/job-dsl-plugin/wiki/Tutorial---Using-the-Jenkins-Job-DSL





