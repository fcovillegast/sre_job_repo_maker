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
            stage("Creation") {
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
                                curl -s -L \
                                -X POST \
                                -H "Accept: application/vnd.github+json" \
                                -H "Authorization: Bearer ${GIT_PASS}" \
                                -H "X-GitHub-Api-Version: 2022-11-28" \
                                https://api.github.com/orgs/${ORGANIZATION}/repos \
                                -d '{"name":"${GIT_REPOSITORY_NAME}","description":"${GIT_REPOSITORY_DESCRIPTION}","private":true,"is_template":false}' \
                                | jq '.id'
                            """

  
                            CREATED_ID = sh (script: "${CURL_CMD}",  returnStdout: true).trim()
                            
                            println "CREATED_ID:${CREATED_ID}"
                            if(!CREATED_ID.isNumber()) {
                                error("Repo creation failed!!! :( ")
                            }
                        }                      

                    }
                }
            }

            stage("Init repository") {
                steps {
                    script {
                        withCredentials([
                                usernamePassword([
                                        credentialsId: 'github',
                                        usernameVariable: 'GIT_USER',
                                        passwordVariable: 'GIT_PASS'
                                ])
                        ]) {
                            sh """
                                git clone http://${GIT_USER}:${GIT_PASS}@github.com/${ORGANIZATION}/${GIT_REPOSITORY_NAME}
                            """

                            dir(GIT_RESPOSITORY_NAME) {
                                sh """
                                    echo "# ${GIT_REPOSITORY_NAME}" >> README.md
                                    echo " ${GIT_REPOSITORY_DESCRIPTION}" >> README.md
                                    git init
                                    git add README.md
                                    git commit -m "First commit"
                                    git branch -M ${DEFAULT_BRANCH}
                                    git remote add origin https://github.com/${ORGANIZATION}/${GIT_REPOSITORY_NAME}.git
                                    git push -u origin ${DEFAULT_BRANCH}
                                """
                            }    
                        }   
                        
                    }
                }


                stage("Protect default branch") {
                steps {
                    script {
                        withCredentials([
                                usernamePassword([
                                        credentialsId: 'github',
                                        usernameVariable: 'GIT_USER',
                                        passwordVariable: 'GIT_PASS'
                                ])
                        ]) {
                            sh """
                                curl -L \
                                  -X PUT \
                                  -H "Accept: application/vnd.github+json" \
                                  -H "Authorization: Bearer ${GIT_PASS}" \
                                  -H "X-GitHub-Api-Version: 2022-11-28" \
                                  https://api.github.com/repos/${ORGANIZATION}/${GIT_REPOSITORY_NAME}/branches/${DEFAULT_BRANCH}/protection \
                                  -d '{}'
                              """
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
