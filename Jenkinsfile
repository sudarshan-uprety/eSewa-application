pipeline {
    agent { label 'ankit-server' }
    
    environment {
        IMAGE_NAME = "sudarshanuprety/esewa"
        IMAGE_TAG  = "latest"
        APP_NAME = "esewa-app"
        NAMESPACE = "esewa-namespace"
        IMAGE_BUILD_NUMBER = "${env.BUILD_NUMBER}"
        DOCKERHUB_CREDENTIALS = credentials('DOCKER')
        K8S_SERVER = "https://144.24.96.24:6443"
    }
    
    stages {
        stage('Checkout Code') {
            steps {
                checkout scm
            }
        }
        
        stage('Build Maven Project') {
            steps {
                script {
                    echo "Building Maven project..."
                    sh """
                        mvn clean package -DskipTests
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
                        
                        # Build for both amd64 and arm64 platforms
                        docker buildx build \\
                            --builder multiarch-builder \\
                            --platform linux/amd64,linux/arm64 \\
                            -t ${IMAGE_NAME}:${IMAGE_TAG} \\
                            -t ${IMAGE_NAME}:${IMAGE_BUILD_NUMBER} \\
                            --push .
                        
                        echo "✅ Multi-platform image pushed successfully"
                        
                        docker logout
                    """
                }
            }
        }
        
        stage('Deploy to Kubernetes') {
            environment {
                KUBECONFIG = credentials('K8-SECRETS')
            }
            steps {
                sh """
                    echo "=== Verifying cluster access ==="
                    kubectl --server=${K8S_SERVER} --insecure-skip-tls-verify=true get nodes
                    
                    echo "=== Creating namespace ==="
                    kubectl --server=${K8S_SERVER} --insecure-skip-tls-verify=true apply -f k8s/namespace.yaml || true
                    
                    echo "=== Creating Docker Registry Secret ==="
                    kubectl --server=${K8S_SERVER} --insecure-skip-tls-verify=true create secret docker-registry dockerhub-secret \\
                        --docker-server=https://index.docker.io/v1/ \\
                        --docker-username=\$DOCKERHUB_CREDENTIALS_USR \\
                        --docker-password=\$DOCKERHUB_CREDENTIALS_PSW \\
                        --docker-email=your-email@example.com \\
                        -n ${NAMESPACE} \\
                        --dry-run=client -o yaml | kubectl --server=${K8S_SERVER} --insecure-skip-tls-verify=true apply -f -
                    
                    echo "=== Deploying to Kubernetes namespace: ${NAMESPACE} ==="
                    kubectl --server=${K8S_SERVER} --insecure-skip-tls-verify=true apply -f k8s/deployment.yaml
                    kubectl --server=${K8S_SERVER} --insecure-skip-tls-verify=true apply -f k8s/service.yaml
                    kubectl --server=${K8S_SERVER} --insecure-skip-tls-verify=true apply -f k8s/ingress.yaml
                    
                    echo "=== Restarting deployment to pull new image ==="
                    kubectl --server=${K8S_SERVER} --insecure-skip-tls-verify=true rollout restart deployment/${APP_NAME} -n ${NAMESPACE}
                """
            }
        }
        
        stage('Wait for Rollout') {
            environment {
                KUBECONFIG = credentials('K8-SECRETS')
            }
            steps {
                sh """
                    echo "Waiting for deployment to complete..."
                    kubectl --server=${K8S_SERVER} --insecure-skip-tls-verify=true rollout status deployment/${APP_NAME} -n ${NAMESPACE} --timeout=300s
                    
                    echo "Rollout completed successfully"
                """
            }
        }
        
        stage('Verify Deployment') {
            environment {
                KUBECONFIG = credentials('K8-SECRETS')
            }
            steps {
                sh """
                    echo "=== Verifying deployment ==="
                    
                    # Get deployment status
                    kubectl --server=${K8S_SERVER} --insecure-skip-tls-verify=true get deployment ${APP_NAME} -n ${NAMESPACE}
                    
                    # Get pods
                    echo ""
                    echo "=== Pods ==="
                    kubectl --server=${K8S_SERVER} --insecure-skip-tls-verify=true get pods -n ${NAMESPACE} -l app=${APP_NAME}
                    
                    # Check running pods
                    RUNNING_PODS=\$(kubectl --server=${K8S_SERVER} --insecure-skip-tls-verify=true get pods -n ${NAMESPACE} -l app=${APP_NAME} --field-selector=status.phase=Running --no-headers | wc -l)
                    
                    if [ "\$RUNNING_PODS" -ge "1" ]; then
                        echo ""
                        echo "✅ \$RUNNING_PODS pod(s) running successfully"
                        
                        # Get pod logs
                        echo ""
                        echo "=== Recent logs ==="
                        POD_NAME=\$(kubectl --server=${K8S_SERVER} --insecure-skip-tls-verify=true get pods -n ${NAMESPACE} -l app=${APP_NAME} -o jsonpath='{.items[0].metadata.name}')
                        kubectl --server=${K8S_SERVER} --insecure-skip-tls-verify=true logs \$POD_NAME -n ${NAMESPACE} --tail=20 || echo "No logs available yet"
                    else
                        echo ""
                        echo "❌ No pods are running!"
                        
                        # Show pod details for debugging
                        kubectl --server=${K8S_SERVER} --insecure-skip-tls-verify=true describe pods -n ${NAMESPACE} -l app=${APP_NAME}
                        exit 1
                    fi
                    
                    # Get service
                    echo ""
                    echo "=== Service ==="
                    kubectl --server=${K8S_SERVER} --insecure-skip-tls-verify=true get service -n ${NAMESPACE} -l app=${APP_NAME}
                    
                    # Get ingress
                    echo ""
                    echo "=== Ingress ==="
                    kubectl --server=${K8S_SERVER} --insecure-skip-tls-verify=true get ingress -n ${NAMESPACE}
                """
            }
        }
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
                // Clean up - not needed since buildx pushes directly
                sh """
                    echo "Build completed using docker buildx"
                """
            }
        }
    }
}