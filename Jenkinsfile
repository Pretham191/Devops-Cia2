// BeforeYouGo CI/CD pipeline
// GitHub push -> webhook (via ngrok) -> this pipeline:
// checkout -> compile -> test -> package -> docker build -> push to ACR
// -> deploy to AKS with Ansible -> verify
//
// One-time setup on the Jenkins host (see docs/RUNBOOK.md):
//   - the jenkins user is logged in to Azure (`sudo -u jenkins az login ...`)
//   - Jenkins credentials: byg-db-password (Secret text), byg-admin (Username with password)

pipeline {
    agent any

    environment {
        RESOURCE_GROUP = 'rg-beforeyougo'
        AKS_CLUSTER    = 'aks-beforeyougo'
        ACR_NAME       = 'beforeyougoacr'              // change if your registry name differs
        ACR_SERVER     = "${ACR_NAME}.azurecr.io"
        IMAGE_REPO     = "${ACR_SERVER}/beforeyougo"
    }

    triggers {
        githubPush()                                   // fired by the GitHub webhook
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '15'))
        disableConcurrentBuilds()                      // never run two deploys at once
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_SHORT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    env.IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_SHORT}"
                }
                echo "Building commit ${env.GIT_SHORT} as image tag ${env.IMAGE_TAG}"
            }
        }

        stage('Compile') {
            steps {
                sh 'mvn -B -ntp clean compile'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn -B -ntp test'
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: false
                    recordCoverage tools: [[parser: 'JACOCO', pattern: 'target/site/jacoco/jacoco.xml']],
                                   sourceDirectories: [[path: 'src/main/java']]
                }
            }
        }

        stage('Package') {
            steps {
                sh 'mvn -B -ntp -DskipTests package'
                archiveArtifacts artifacts: 'target/app.jar', fingerprint: true
            }
        }

        stage('Docker Build') {
            steps {
                sh '''
                    docker build \
                      --build-arg APP_VERSION=${IMAGE_TAG} \
                      --build-arg GIT_COMMIT=${GIT_SHORT} \
                      -t ${IMAGE_REPO}:${IMAGE_TAG} \
                      -t ${IMAGE_REPO}:latest \
                      .
                '''
            }
        }

        stage('Push to ACR') {
            steps {
                sh '''
                    az account show --query name -o tsv
                    az acr login --name ${ACR_NAME}
                    docker push ${IMAGE_REPO}:${IMAGE_TAG}
                    docker push ${IMAGE_REPO}:latest
                '''
            }
        }

        stage('Deploy to AKS') {
            steps {
                sh 'az aks get-credentials --resource-group ${RESOURCE_GROUP} --name ${AKS_CLUSTER} --overwrite-existing'
                withCredentials([
                    string(credentialsId: 'byg-db-password', variable: 'DB_PASSWORD'),
                    usernamePassword(credentialsId: 'byg-admin',
                                     usernameVariable: 'ADMIN_USERNAME',
                                     passwordVariable: 'ADMIN_PASSWORD')
                ]) {
                    sh '''
                        ansible-playbook -i ansible/inventory.ini ansible/deploy.yml \
                          -e app_image=${IMAGE_REPO}:${IMAGE_TAG} \
                          -e app_version=${IMAGE_TAG}
                    '''
                }
            }
        }

        stage('Verify') {
            steps {
                sh '''
                    kubectl -n beforeyougo rollout status deployment/beforeyougo --timeout=120s
                    kubectl -n beforeyougo get deployment,pods,svc -o wide
                    APP_IP=$(kubectl -n beforeyougo get svc beforeyougo -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
                    echo "Health check: http://${APP_IP}/actuator/health"
                    curl -fsS "http://${APP_IP}/actuator/health"
                    echo
                    curl -fsS "http://${APP_IP}/" | grep -o "build ${IMAGE_TAG}"
                    echo "Application URL: http://${APP_IP}/"
                '''
            }
        }
    }

    post {
        success {
            echo "Deployed build ${env.IMAGE_TAG} to AKS."
        }
        failure {
            echo 'Pipeline failed — check the stage logs above.'
        }
        always {
            // Keep the local Docker cache from filling the laptop's disk.
            sh 'docker image prune -f || true'
        }
    }
}
