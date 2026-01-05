pipeline {
    agent { label 'oracle-3' }
    
    environment {
        IMAGE_NAME = "sudarshanuprety/esewa"
        IMAGE_TAG  = "latest"
        NAMESPACE = "esewa-namespace"
        APP_NAME = "esewa-app"
        IMAGE_BUILD_NUMBER = "${env.BUILD_NUMBER}"
    }
    
    stages {
        stage('Checkout Code') {
            steps {
                checkout scm
            }
        }

        stage('Setup Kubernetes Context') {
            steps {
                sh """
                    kubectl cluster-info
                    kubectl get nodes
                """
            }
        }
        
        stage('Create Namespace if Not Exists') {
            steps {
                sh """
                    if ! kubectl get namespace ${NAMESPACE} > /dev/null 2>&1; then
                        echo "Namespace ${NAMESPACE} does not exist. Creating..."
                        kubectl create namespace ${NAMESPACE}
                        echo "Namespace created"
                    else
                        echo "Namespace ${NAMESPACE} already exists"
                    fi
                """
            }
        }
        
        stage('Create Docker Registry Secret') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'DOCKER',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )
                ]) {
                    sh """
                        kubectl create secret docker-registry dockerhub-secret \\
                            --docker-server=https://index.docker.io/v1/ \\
                            --docker-username=\$DOCKER_USER \\
                            --docker-password=\$DOCKER_PASS \\
                            --docker-email=your-email@example.com \\
                            -n ${NAMESPACE} \\
                            --dry-run=client -o yaml | kubectl apply -f -
                        
                        echo "✅ Docker registry secret configured"
                    """
                }
            }
        }
        
        // stage('Pull Latest Image') {
        //     steps {
        //         withCredentials([
        //             usernamePassword(
        //                 credentialsId: 'DOCKERHUB_CREDENTIALS',
        //                 usernameVariable: 'DOCKER_USER',
        //                 passwordVariable: 'DOCKER_PASS'
        //             )
        //         ]) {
        //             sh """
        //                 echo \$DOCKER_PASS | docker login -u \$DOCKER_USER --password-stdin
        //                 docker pull ${IMAGE_NAME}:${IMAGE_TAG}
                        
        //                 echo "Image pulled"
        //                 docker images | grep ${IMAGE_NAME}
        //             """
        //         }
        //     }
        // }
        
         stage('Deploy to Kubernetes') {
             steps {
                 sh """                    
                     if [ -d "k8s" ]; then
                         echo "Found k8s directory"
                         kubectl apply -f k8s/ -n ${NAMESPACE}
                     elif [ -d "kubernetes" ]; then
                         echo "Found kubernetes directory"
                         kubectl apply -f kubernetes/ -n ${NAMESPACE}
                     elif [ -f "deployment.yaml" ]; then
                         echo "Found deployment.yaml in root"
                         kubectl apply -f deployment.yaml -n ${NAMESPACE}
                         [ -f "service.yaml" ] && kubectl apply -f service.yaml -n ${NAMESPACE}
                     else
                         echo "Please add YAML files in one of these locations:"
                         exit 1
                     fi
                 """
             }
         }
        
         stage('Wait for Rollout') {
             steps {
                 sh """
                     # Wait for deployment to complete
                     kubectl rollout status deployment/${APP_NAME} -n ${NAMESPACE} --timeout=300s
                    
                     echo "Rollout completed"
                 """
             }
         }
        
         stage('Verify Deployment') {
             steps {
                 sh """
                     echo "=== Verifying deployment ==="
                    
                     # Get deployment status
                     kubectl get deployment ${APP_NAME} -n ${NAMESPACE}
                    
                     # Get pods
                     kubectl get pods -n ${NAMESPACE} -l app=${APP_NAME}
                    
                     RUNNING_PODS=\$(kubectl get pods -n ${NAMESPACE} -l app=${APP_NAME} --field-selector=status.phase=Running --no-headers | wc -l)
                    
                     if [ "\$RUNNING_PODS" -ge "1" ]; then
                         echo ""
                         echo "\$RUNNING_PODS pod(s) running"
                        
                         # Get pod logs (last 20 lines)
                         echo ""
                         echo "=== Recent logs ==="
                         POD_NAME=\$(kubectl get pods -n ${NAMESPACE} -l app=${APP_NAME} -o jsonpath='{.items[0].metadata.name}')
                         kubectl logs \$POD_NAME -n ${NAMESPACE} --tail=20 || echo "No logs available yet"
                     else
                         echo ""
                         echo "No pods are running!"
                        
                         # Show pod details for debugging
                         kubectl describe pods -n ${NAMESPACE} -l app=${APP_NAME}
                         exit 1
                     fi
                    
                     # Get service
                     kubectl get service -n ${NAMESPACE} -l app=${APP_NAME}
                    
                     # kubectl get ingress -n ${NAMESPACE} 2>/dev/null || echo "No ingress found"
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
                     echo "======================================"
                     echo ""
                     echo "Useful commands:"
                     echo "  View pods:    kubectl get pods -n ${NAMESPACE}"
                     echo "  View logs:    kubectl logs -f deployment/${APP_NAME} -n ${NAMESPACE}"
                     echo "  Port forward: kubectl port-forward svc/${APP_NAME} 8080:80 -n ${NAMESPACE}"
                     echo "  Describe:     kubectl describe deployment ${APP_NAME} -n ${NAMESPACE}"
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
                     echo "======================================"
                 """
             }
         }
     }
}