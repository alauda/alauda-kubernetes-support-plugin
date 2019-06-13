// https://jenkins.io/doc/book/pipeline/syntax/
// Multi-branch discovery pattern: PR-.*
@Library('alauda-cicd') _

// global variables for pipeline
def GIT_BRANCH
def FOLDER = "."
def DEBUG = false
def release
def IMAGE
def RELEASE_VERSION
def RELEASE_BUILD
pipeline {
	// 运行node条件
	// 为了扩容jenkins的功能一般情况会分开一些功能到不同的node上面
	// 这样每个node作用比较清晰，并可以并行处理更多的任务量
	agent { label 'golang && java' }

	// (optional) 流水线全局设置
	options {
		// 保留多少流水线记录（建议不放在jenkinsfile里面）
		buildDiscarder(logRotator(numToKeepStr: '10'))

		// 不允许并行执行
		disableConcurrentBuilds()
	}

	parameters {
		booleanParam defaultValue: false, description: 'Rebuild and archive artifacts if this flag is true.', name: 'forceReBuild'
		booleanParam defaultValue: false, description: 'Force execute sonar scan if this flag is true.', name: 'forceSonarScan'
	}

	//(optional) 环境变量
	environment {
		// for building an scanning
		REPOSITORY = "alauda-kubernetes-support-plugin"
		OWNER = "alauda"
		// sonar feedback user
		// needs to change together with the credentialsID
		SCM_FEEDBACK_ACCOUNT = "alaudabot"
		SONARQUBE_SCM_CREDENTIALS = "alaudabot"
		DEPLOYMENT = "alauda-kubernetes-support-plugin"
		IMAGE_REPOSITORY = "index.alauda.cn/alaudak8s/alauda-kubernetes-support"
		IMAGE_CREDENTIALS = "alaudak8s"
		DINGDING_BOT = "devops-chat-bot"
		TAG_CREDENTIALS = "alaudabot-github"
		IN_K8S = "true"

		// charts pipeline name
		CHARTS_PIPELINE = "/devops/devops-alauda-jenkins"
		CHART_COMPONENT = "alauda-kubernetes-support-plugin"
	}
	// stages
	stages {
		stage('Checkout') {
			steps {
				script {
					// checkout code
					withCredentials([
							usernamePassword(credentialsId: PROXY_CREDENTIALS_ID, passwordVariable: 'PROXY_ADDRESS', usernameVariable: 'PROXY_ADDRESS_PASS')
					]) { PROXY_CREDENTIALS = "${PROXY_ADDRESS}" }
					sh "git config --global http.proxy ${PROXY_CREDENTIALS}"
					def scmVars = checkout scm

					release = deploy.release(scmVars)

					RELEASE_BUILD = release.version
					RELEASE_VERSION = release.majorVersion
					// echo "release ${RELEASE_VERSION} - release build ${RELEASE_BUILD}"
					echo """
						release ${RELEASE_VERSION}
						version ${release.version}
						is_release ${release.is_release}
						is_build ${release.is_build}
						is_master ${release.is_master}
						deploy_env ${release.environment}
						auto_test ${release.auto_test}
						environment ${release.environment}
						majorVersion ${release.majorVersion}
					"""
				}
			}
		}
		stage('Build') {
			when {
				anyOf {
					changeset '**/**/*.java'
					changeset '**/**/*.xml'
					changeset '**/**/*.jelly'
					changeset '**/**/*.properties'
					changeset '**/**/*.png'
					expression {
						return params.forceReBuild
					}
				}
			}
			steps {
				script {
					container('java'){
						sh """
                            mvn clean install -U findbugs:findbugs -Dmaven.test.skip=true
                        """
					}

					archiveArtifacts 'target/*.hpi'

					IMAGE = deploy.dockerBuild(
							"./build/docker/alauda-devops-support.Dockerfile", //Dockerfile
							".", // build context
							IMAGE_REPOSITORY, // repo address
							RELEASE_BUILD, // tag
							IMAGE_CREDENTIALS, // credentials for pushing
					)
					// start and push
					IMAGE.start().push()
				}
			}
		}
		// sonar scan
		stage('Sonar') {
			when {
				anyOf {
					changeset '**/**/*.java'
					expression {
						return params.forceSonarScan
					}
				}
			}
			steps {
				script {
					deploy.scan(
							REPOSITORY,
							GIT_BRANCH,
							SONARQUBE_SCM_CREDENTIALS,
							FOLDER,
							DEBUG,
							OWNER,
							SCM_FEEDBACK_ACCOUNT).startToSonar()
				}
			}
		}

		stage('Tag git') {
			when {
				expression {
					release.shouldTag()
				}
			}
			steps {
				script {
					dir(FOLDER) {
						container('tools') {
							deploy.gitTag(
									TAG_CREDENTIALS,
									RELEASE_BUILD,
									OWNER,
									REPOSITORY
							)
						}
					}
				}
			}
		}

		stage('Chart Update') {
			when {
				expression {
					// TODO: Change when charts are ready
					release.shouldUpdateChart()
				}
			}
			steps {
				script {
					echo "will trigger charts-pipeline using branch ${release.chartBranch}"

					build job: CHARTS_PIPELINE, parameters: [
							[$class: 'StringParameterValue', name: 'PLUGIN', value: IMAGE_REPOSITORY+":"+RELEASE_BUILD],
					], wait: false
				}
			}
		}
	}

	// (optional)
	// happens at the end of the pipeline
	post {
		// 成功
		success {
			echo "Horay!"
			script {
				deploy.notificationSuccess(DEPLOYMENT, DINGDING_BOT, "流水线完成了", RELEASE_BUILD)
			}
		}
		// 失败
		failure {
			// check the npm log
			// fails lets check if it
			script {
				echo "damn!"
				deploy.notificationFailed(DEPLOYMENT, DINGDING_BOT, "流水线失败了", RELEASE_BUILD)
			}
		}
		always { junit allowEmptyResults: true, testResults: '**/target/surefire-reports/**/*.xml' }
	}
}

