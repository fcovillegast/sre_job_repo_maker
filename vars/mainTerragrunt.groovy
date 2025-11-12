#!/usr/bin/groovy

def call() {


    pipeline {

        parameters {
            choice(name:"JENKINS_AGENT_LABEL", description:"The agent label",choices:[env.JENKINS_AGENT_LABEL])
            choice(name:"ORGANIZATION", description:"Git organization", choices:[env.ORGANIZATION])
            string(name:"GIT_REPOSITORY_NAME", description:"Git repo name", defaultValue: env.GIT_REPOSITORY_NAME)
            text(name:"GIT_REPOSITORY_DESCRIPTION", description:"Git repo description", defaultValue: env.GIT_REPOSITORY_DESCRIPTION)
            string(name:"DEFAULT_BRANCH", description:"Git default branch", defaultValue: env.DEFAULT_BRANCH)
        }
      
        agent {node {label "${JENKINS_AGENT_LABEL}"}}

        options {
            disableConcurrentBuilds()
            buildDiscarder(logRotator(numToKeepStr: '5'))
        }

        stages {
            stage("Clone") {
                steps {
                    script {
                        cleanWs()

                        withCredentials([
                                usernamePassword([
                                        credentialsId: 'github',
                                        usernameVariable: 'GIT_USER',
                                        passwordVariable: 'GIT_PASS'
                                ])
                        ]) {
                            CURL_CMD = """
                                curl -L \
                                -X POST \
                                -H "Accept: application/vnd.github+json" \
                                -H "Authorization: Bearer ${GIT_PASS}" \
                                -H "X-GitHub-Api-Version: 2022-11-28" \
                                https://api.github.com/orgs/${ORGANIZATION}/repos \
                                -d '{"name":"${GIT_REPOSITORY_NAME}","description":"${GIT_REPOSITORY_DESCRIPTION}","private":true,"is_template":false}'    
                            """
    
                            CREATED_JSON = sh (script: CURL_CMD , returnStdout: true).trim()
                            CREATED_ID = sh (script: """echo "${CREATED_JSON} | jq '.id' """, returnStdout: true).trim()

                            if(!CREATED_ID.isNumber()) {
                                error("Repo creation failed!!! :( ")
                            }
                        }                      

                    }
                }
            }
        }
        post {
            always {
                node("${JENKINS_AGENT_LABEL}") {
                    sh("rm -f ~/.gitconfig")
                }
            }
        }

    }
}
