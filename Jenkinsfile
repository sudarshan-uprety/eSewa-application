pipeline {
    agent { label 'ankit-server' }
    
    environment {
        IMAGE_NAME = "sudarshanuprety/esewa"
        IMAGE_TAG  = "latest"
        APP_NAME = "esewa-app"
        NAMESPACE = "esewa-namespace"
        IMAGE_BUILD_NUMBER = "${env.BUILD_NUMBER}"
        DOCKERHUB_CREDENTIALS = credentials('DOCKER')
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
                    echo "=== Building Spring Boot Application ==="
                    mvn clean package -DskipTests
                    
                    echo "=== Checking WAR file ==="
                    ls -lh target/*.war
                '''
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    echo "Building Docker image: ${IMAGE_NAME}:${IMAGE_TAG}"
                    sh """
                        docker build -t ${IMAGE_NAME}:${IMAGE_TAG} .
                        docker tag ${IMAGE_NAME}:${IMAGE_TAG} ${IMAGE_NAME}:build-${IMAGE_BUILD_NUMBER}
                    """
                }
            }
        }
        
        stage('Build and Push Docker Image') {
            steps {
                script {
                    echo "Building and pushing Docker image: ${IMAGE_NAME}:${IMAGE_TAG}"
                    sh """
                        echo \$DOCKERHUB_CREDENTIALS_PSW | docker login -u \$DOCKERHUB_CREDENTIALS_USR --password-stdin
                        
                        docker buildx build \\
                            --platform linux/amd64 \\
                            -t ${IMAGE_NAME}:${IMAGE_TAG} \\
                            -t ${IMAGE_NAME}:${IMAGE_BUILD_NUMBER} \\
                            --push .
                        
                        docker logout
                    """
                }
            }
        }
        
        // stage('Create Namespace') {
        //     steps {
        //         sh """
        //             kubectl apply -f k8s/namespace.yaml || true
        //         """
        //     }
        // }
        
        // stage('Create Docker Registry Secret') {
        //     steps {
        //         sh """
        //             kubectl create secret docker-registry dockerhub-secret \\
        //                 --docker-server=https://index.docker.io/v1/ \\
        //                 --docker-username=\$DOCKERHUB_CREDENTIALS_USR \\
        //                 --docker-password=\$DOCKERHUB_CREDENTIALS_PSW \\
        //                 --docker-email=your-email@example.com \\
        //                 -n ${NAMESPACE} \\
        //                 --dry-run=client -o yaml | kubectl apply -f -
        //         """
        //     }
        // }
        
        // stage('Deploy to Kubernetes') {
        //     steps {
        //         sh """
        //             echo "Deploying to Kubernetes namespace: ${NAMESPACE}"
                    
        //             # Apply all k8s configurations
        //             kubectl apply -f k8s/deployment.yaml
        //             kubectl apply -f k8s/service.yaml
        //             kubectl apply -f k8s/ingress.yaml
                    
        //             # Force pods to pull new image
        //             kubectl rollout restart deployment/${APP_NAME} -n ${NAMESPACE}
        //         """
        //     }
        // }
        
        // stage('Wait for Rollout') {
        //     steps {
        //         sh """
        //             echo "Waiting for deployment to complete..."
        //             kubectl rollout status deployment/${APP_NAME} -n ${NAMESPACE} --timeout=300s
                    
        //             echo "Rollout completed successfully"
        //         """
        //     }
        // }
        
        // stage('Verify Deployment') {
        //     steps {
        //         sh """
        //             echo "=== Verifying deployment ==="
                    
        //             # Get deployment status
        //             kubectl get deployment ${APP_NAME} -n ${NAMESPACE}
                    
        //             # Get pods
        //             echo ""
        //             echo "=== Pods ==="
        //             kubectl get pods -n ${NAMESPACE} -l app=${APP_NAME}
                    
        //             # Check running pods
        //             RUNNING_PODS=\$(kubectl get pods -n ${NAMESPACE} -l app=${APP_NAME} --field-selector=status.phase=Running --no-headers | wc -l)
                    
        //             if [ "\$RUNNING_PODS" -ge "1" ]; then
        //                 echo ""
        //                 echo "✅ \$RUNNING_PODS pod(s) running successfully"
                        
        //                 # Get pod logs
        //                 echo ""
        //                 echo "=== Recent logs ==="
        //                 POD_NAME=\$(kubectl get pods -n ${NAMESPACE} -l app=${APP_NAME} -o jsonpath='{.items[0].metadata.name}')
        //                 kubectl logs \$POD_NAME -n ${NAMESPACE} --tail=20 || echo "No logs available yet"
        //             else
        //                 echo ""
        //                 echo "❌ No pods are running!"
                        
        //                 # Show pod details for debugging
        //                 kubectl describe pods -n ${NAMESPACE} -l app=${APP_NAME}
        //                 exit 1
        //             fi
                    
        //             # Get service
        //             echo ""
        //             echo "=== Service ==="
        //             kubectl get service -n ${NAMESPACE} -l app=${APP_NAME}
                    
        //             # Get ingress
        //             echo ""
        //             echo "=== Ingress ==="
        //             kubectl get ingress -n ${NAMESPACE}
        //         """
        //     }
        // }
    }
    
    post {
        success {
            script {
                sh """
                    echo ""
                    echo "======================================"
                    echo "✅ DEPLOYMENT SUCCESSFUL!"
                    echo "======================================"
                    echo "Application: ${APP_NAME}"
                    echo "Namespace: ${NAMESPACE}"
                    echo "Image: ${IMAGE_NAME}:${IMAGE_TAG}"
                    echo "Build: ${IMAGE_BUILD_NUMBER}"
                    echo "======================================"
                    echo ""
                    echo "🌐 Access your application at:"
                    echo "   http://esewaprod.sudarshan-uprety.com.np"
                    echo ""
                    echo "Useful commands:"
                    echo "  View pods:    kubectl get pods -n ${NAMESPACE}"
                    echo "  View logs:    kubectl logs -f deployment/${APP_NAME} -n ${NAMESPACE}"
                    echo "  Port forward: kubectl port-forward svc/esewa-service 8080:80 -n ${NAMESPACE}"
                    echo "  Describe:     kubectl describe deployment ${APP_NAME} -n ${NAMESPACE}"
                    echo "  Get ingress:  kubectl get ingress -n ${NAMESPACE}"
                    echo "======================================"
                """
            }
        }
        failure {
            script {
                sh """
                    echo ""
                    echo "======================================"
                    echo "❌ DEPLOYMENT FAILED"
                    echo "======================================"
                    echo ""
                    echo "Debug commands:"
                    echo "  kubectl get pods -n ${NAMESPACE}"
                    echo "  kubectl describe deployment ${APP_NAME} -n ${NAMESPACE}"
                    echo "  kubectl logs deployment/${APP_NAME} -n ${NAMESPACE}"
                    echo "  kubectl get events -n ${NAMESPACE} --sort-by='.lastTimestamp'"
                    echo "  kubectl describe ingress -n ${NAMESPACE}"
                    echo "======================================"
                """
            }
        }
        always {
            script {
                // Clean up local Docker images to save space
                sh """
                    docker rmi ${IMAGE_NAME}:${IMAGE_TAG} || true
                    docker rmi ${IMAGE_NAME}:build-${IMAGE_BUILD_NUMBER} || true
                """
            }
        }
    }
}