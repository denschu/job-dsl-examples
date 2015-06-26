def repository = 'codecentric/crossfire';
// See also https://developer.github.com/v3/#authentication
def authString = "<USER>:<PASSWORD>".getBytes().encodeBase64().toString();

URLConnection connBranches = new URL("https://api.github.com/repos/${repository}/branches").openConnection();
connBranches.setRequestProperty("Authorization", "Basic ${authString}");
def branches = new groovy.json.JsonSlurper().parse(new BufferedReader(new InputStreamReader(connBranches.getInputStream())));

//Iterate over each Branch
branches.each { 
	def branchName = it.name

	URLConnection connProjects = new URL("https://api.github.com/repos/${repository}/contents?ref=${branchName}").openConnection();
	connProjects.setRequestProperty("Authorization", "Basic ${authString}");
	def projects = new groovy.json.JsonSlurper().parse(new BufferedReader(new InputStreamReader(connProjects.getInputStream())));

	//Iterate over each Project inside the actual branch
	projects.each { 
	    def projectName = it.name
	    def contentType = it.type
	    //Include only directories
	    if(contentType == "dir"){
	    	//Search for Dockerfile
	    	boolean containsDockerfile = false;
			URLConnection connProject = new URL("https://api.github.com/repos/${repository}/contents/${projectName}?ref=${branchName}").openConnection();
			connProject.setRequestProperty("Authorization", "Basic ${authString}");
			def projectContents = new groovy.json.JsonSlurper().parse(new BufferedReader(new InputStreamReader(connProject.getInputStream())));
			projectContents.each { 
				if(it.name == "Dockerfile"){
					containsDockerfile = true;
				}
			}
			//Generate project only for Docker projects
			if(containsDockerfile){
				job(type: Maven) {
			        name("${projectName}-${branchName}")
			        triggers { scm("*/5 * * * *") }
			        scm {
						git {
						    remote {
						        url("https://github.com/${repository}")
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
							}
						}
					}		
					publishers {
						groovyPostBuild("manager.addShortText(manager.build.getEnvironment(manager.listener)[\'POM_VERSION\'])")
					}		
				}					
			}
			    
	    }
    	
	}	
}