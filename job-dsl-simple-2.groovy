job(type: Maven) {
    name("${projectName}")
    triggers { scm("*/5 * * * *") }
    scm {
		git {
		    remote {
		        url("https://github.com/codecentric/spring-samples")
		    }
		}
	}
	goals("clean package")	
}