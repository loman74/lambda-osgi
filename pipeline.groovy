import org.finra.appeng.f3.builders.*
import org.finra.appeng.f3.common.*
/*

F3 DOCS: https://bitbucket.finra.org/projects/DO/repos/f3/browse/docs
JenkinsJob Configuration: https://bitbucket.finra.org/projects/DO/repos/f3/browse/docs/jenkinsjob.md

*/

interface Constants {
    final String AGS = "CRISP"
    final List<String> EMAIL = ["dl-crisp@finra.org"]
}

class Pipeline extends CommonImpl {

    // Component reusable declarations
    private static COMPONENT = "lambda-java"
    private static final Map SDLCS = [
            "dev"  	   : "465257512377",
			"dev-int"  : "465257512377",
            "qa"   	 : "142248000760",
			"qa-int" : "142248000760",
            "prod"	 : "510199193688"
    ]
	private static final Map  clusterSDLCS = [
										"dev"		: "DEV",
										"dev-int"	: "DEV",
										"qa"		: "QA",
										"qa-int"	: "QA",
										"preprod"	: "QA",
										"prod"		: "PROD"
								]

    // Please verify these and update if necessary
    private static String JOBS_FOLDER = Constants.AGS + '/' + COMPONENT + '/'
    private static rootJob = JOBS_FOLDER + COMPONENT

    // Component Build/Start/Pipeline-Initial Job parameters
    BppView buildPipeline() {
        Logger.log("FJDSL : Creating pipeline view " + rootJob)
        BppView.newInstance()
                .withSelectedJob(rootJob)
                .withDisplayedBuilds(9)
                .withViewFolderLocation(Constants.AGS)
                .withStartJob(start())
    }

    // Pipeline Initial Job
    JenkinsJob start() {
        JenkinsJob.newInstance()
                .withName(rootJob)
                .withComponent(COMPONENT)
                .withTemplate(Template.BUILD)
                .withJobShell("\${WORKSPACE}/checkout/java/build.sh")
                .withDownstream(buildImageJob())
                .withDownstream(scan())
                .withAgs(Constants.AGS)
                .withDisableDeployDashboard(true)
                .withRelease('$projectVersion')
                .withEcsLabel(ECSLabel.newInstance()
                        .withImage('570164370074.dkr.ecr.us-east-1.amazonaws.com/jenkins/j1851m3njsg46:latest')
                        .withCluster('DEV')
                        .withRole('arn:aws:iam::465257512377:role/JENKINS_CRISP')
                        .withMemory('3.0'))
                .withInputChoiceParam([
                        InputChoiceParam.newInstance()
                                .withKey("COMMAND_ENV")
                                .withValue(["stack-update", "fresh", "stack-delete"])
                                .withDescription("Deployment command"),
                        InputChoiceParam.newInstance()
                                .withKey("SKIP_BUILD")
                                .withValue(["no", "yes"])
                                .withDescription("Used in script to skip app build")])
                .withDryRun(false)
                .withRepos([Repo.newInstance()
                                    .withUrl("ssh://git@bitbucket.finra.org:7999/crisp/crisp-lambda.git")
                                    .withSub_directory("checkout")
                                    .withDescription("application url")
                                    .withBranchVariable("GIT_BRANCH")
                                    .withDefaultValue("master")])
                 .withScanLocations(
                        ScanLocations.newInstance()
                                .withArchiveType(ArchiveType.GZIP)
                                .withOs(OS.LINUX)
                                .withLocations([
                                        Location.newInstance().withDirLocation('.').withArchiveName('workspace.tar.gz')
                                ])
                )
    }
    
        JenkinsJob scan(boolean prod=false) {

        	def job = JenkinsJob.newInstance()
                .withName(Constants.AGS + '/' + COMPONENT +"/z${prod ? 'prod_' : ''}bdh_scan")
                .withTemplate(Template.SCAN)
                .withS3CopyJob(rootJob)
                .withSourcePattern("workspace.tar.gz")
                .withJobShell("-c 'tar -xzvf workspace.tar.gz; rm -rf workspace.tar.gz'")
                .withBdhScan(BdhScan.newInstance().withPropertyFileLocation('${WORKSPACE}/checkout/java/bdhub.properties'))

	        if (prod) {
	            // Marks this is a PROD-RC scan in blackduck.
	            job.withAdditionalEnvVars([AdditionalEnvVars.newInstance().withValue("PROD-RC").withKey("GIT_BRANCH")])
	        }
	
	        return job
        
    }
  
  
      private JenkinsJob buildImageJob() {
        JenkinsJob.newInstance()
                .withName(Constants.AGS + '/' + COMPONENT + '/build-image')
                .withEcsLabel(ECSLabel.newInstance()
                        .withImage('570164370074.dkr.ecr.us-east-1.amazonaws.com/jenkins/build_app:latest')
                        .withCluster('DEV')
                        .withRole('arn:aws:iam::465257512377:role/JENKINS_CRISP')
                        .withMemory('3.0'))
                .withTemplate(Template.DOWNSTREAMBUILD)
                .withS3CopyJob(rootJob)
                .withCredentialBinding(CredentialBinding.newInstance()
                        .withUsername("APRO_USER")
                        .withPassword("APRO_PASSWORD")
                        .withCredential("4feb1062-373b-4ba5-ad68-762986bef4fc"))
                .withDisableDeployDashboard(true)
                .withJobShell('release/build_image.sh' )
                .withResetBuildNumber(true)
                .withDownstream(push("dev"))
                .withDownstream(push("qa"))

    }
  
  
      private JenkinsJob push(String sdlc) {
        def job = JenkinsJob.newInstance()
                .withName(Constants.AGS + '/' + COMPONENT + '/deployment_jobs/' + "push-image-${sdlc}")
                .withEcsLabel(ECSLabel.newInstance()
                        .withImage('570164370074.dkr.ecr.us-east-1.amazonaws.com/jenkins/build_app:latest')
                        .withCluster("${sdlc}".toUpperCase())
                        .withRole("arn:aws:iam::${SDLCS[sdlc]}:role/JENKINS_CRISP")
                        .withMemory('3.0'))
                .withTemplate(Template.DEPLOY)
                .withJobShell('release/push_image.sh')                
                .withS3CopyJob(Constants.AGS + '/' + COMPONENT + '/build-image')
            
        switch(sdlc) {
            case 'dev':
                job.withDownstream(deploy("dev"))
                job.withDownstream(deploy("dev-int"))
            break
            case 'qa':
                job.withDownstream(deploy("qa"))
                job.withDownstream(deploy("qa-int"))
                job.withDownstream(deploy("preprod")) 
            break
            case 'prod':
                job.withAutoTriggerDownstream(deploy("prod"))
            break
        }
        return job
    }

    private JenkinsJob deploy(String sdlc){
        def job = JenkinsJob.newInstance()
                .withName(Constants.AGS + '/' + COMPONENT + '/deployment_jobs/' + "deploy-$sdlc")
                .withEcsLabel(ECSLabel.newInstance()
                        .withImage('570164370074.dkr.ecr.us-east-1.amazonaws.com/jenkins/provision:13.6.0')
                        .withCluster(clusterSDLCS[sdlc])
                        .withRole("arn:aws:iam::${SDLCS[sdlc]}:role/JENKINS_CRISP")
                        .withMemory('1.0'))
                .withAdditionalEnvVars([AdditionalEnvVars.newInstance()
                                                .withKey("SDLC")
                                                .withValue("${sdlc}")])
                .withTemplate(Template.DEPLOY)
                .withS3CopyJob(Constants.AGS + '/' + COMPONENT + '/build-image')
                .withJobShell('release/deploy.sh')       
        switch(sdlc) {
            case 'qa-int':
                   job.withDownstream(push("prod"))
                   job.withDownstream(scan(true))         
            break
        }
        return job
    }
}
Pipeline.newInstance().createPipeline(this, Pipeline.newInstance().buildPipeline())
