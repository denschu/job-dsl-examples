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
						rootPOM("${projectName}/pom.xml")
						goals("build-helper:parse-version")
						goals("versions:set")
						property("newVersion", "\${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.incrementalVersion}-\${BUILD_NUMBER}")
					}
				}
				postSuccessfulBuildSteps {
					maven {
						rootPOM("${projectName}/pom.xml")
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


job{
    name("deploy-application")
}