#!/usr/bin/groovy

library identifier: 'sre_job_repo_maker@main', retriever: modernSCM(
        [$class: 'GitSCMSource',
         remote: 'https://github.com/fcovillegast/sre_job_repo_maker',
         credentialsId: 'github'
        ])

mainTerragrunt()
