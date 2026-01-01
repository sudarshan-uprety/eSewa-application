pipeline {
    agent { label 'oracle-3' }

    environment {
        IMAGE_NAME = "sudarshanuprety/esewa"
        IMAGE_TAG  = "latest"
    }

    stages {

        stage('Checkout Code') {
            steps {
                checkout scm
            }
        }

        stage('Build WAR') {
            steps {
                sh '''
                    mvn clean package -DskipTests
                '''
            }
        }

        stage('Build Docker Image') {
            steps {
                sh """
                    docker buildx rm mybuilder || true
                    
                    docker buildx create --name mybuilder --use --bootstrap
                    
                    # Build for AMD64
                    docker buildx build \\
                        --platform linux/amd64 \\
                        -t ${IMAGE_NAME}:${IMAGE_TAG} \\
                        --push .
                """
            }
        }

        stage('Push Docker Image') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'DOCKER', 
                                                  usernameVariable: 'DOCKER_USER', 
                                                  passwordVariable: 'DOCKER_PASS')]) {
                    sh '''
                        # Log in to Docker Hub securely
                        echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                        
                        # Push image with tag
                        docker push ${IMAGE_NAME}:${IMAGE_TAG}
                    '''
                }
            }
        }

    }

    post {
        success {
            echo "Docker image ${IMAGE_NAME}:${IMAGE_TAG} pushed successfully 🚀"
        }
        failure {
            echo "Build or push failed ❌"
        }
    }
}
