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
                    echo "=== Building Spring Boot Application ==="
                    mvn clean package -DskipTests
                    
                    echo "=== Checking WAR file ==="
                    ls -lh target/*.war
                '''
            }
        }

        stage('Build Docker Image') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'DOCKER',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )
                ]) {
                    sh """
                        echo "=== Logging into Docker Hub ==="
                        echo "\$DOCKER_PASS" | docker login -u "\$DOCKER_USER" --password-stdin
                        
                        echo "=== Building Docker image ==="
                        docker buildx rm mybuilder || true
                        docker buildx create --name mybuilder --use --bootstrap

                        docker buildx build \\
                            --platform linux/amd64 \\
                            -t ${IMAGE_NAME}:${IMAGE_TAG} \\
                            --push .


                        docker push ${IMAGE_NAME}:${IMAGE_TAG}
                        docker push ${IMAGE_NAME}:latest
                        
                        echo "✅ Images pushed:"
                        echo "  - ${IMAGE_NAME}:latest"
                    """
                }
            }
        }

        stage('Deploy to AKS') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'AZURE_SP',
                        usernameVariable: 'AZURE_CLIENT_ID',
                        passwordVariable: 'AZURE_CLIENT_SECRET'
                    )
                ]) {
                    sh """
                        echo "=== Logging into Azure ==="
                        az login --service-principal \\
                            -u $AZURE_CLIENT_ID \\
                            -p $AZURE_CLIENT_SECRET \\
                            --tenant $AZURE_TENANT_ID
                        
                        az account set --subscription $AZURE_SUBSCRIPTION_ID
                        
                        echo "=== Deploying to AKS ==="
                        az aks get-credentials \\
                            --resource-group esewa-resources \\
                            --name esewa-cluster \\
                            --overwrite-existing
                        
                        # Update Kubernetes deployment with new image
                        kubectl set image deployment/esewa-app \\
                            esewa-app=${IMAGE_NAME}:${IMAGE_TAG} \\
                            -n esewans
                        
                        echo "=== Waiting for rollout ==="
                        kubectl rollout status deployment/esewa-app -n esewans --timeout=300s
                        
                        echo "✅ Deployment completed!"
                        echo "New image: ${IMAGE_NAME}:${IMAGE_TAG}"
                    """
                }
            }
        }

        stage('Verify Deployment') {
            steps {
                sh """
                    echo "=== Verifying deployment ==="
                    
                    az aks get-credentials \\
                        --resource-group esewa-resources \\
                        --name esewa-cluster \\
                        --overwrite-existing
                    
                    echo "--- Pods status ---"
                    kubectl get pods -n esewans -o wide
                    
                    echo "--- Deployment details ---"
                    kubectl get deployment esewa-app -n esewans -o yaml | grep -A2 "image:"
                    
                    echo "--- Testing application ---"
                    # Get LoadBalancer IP
                    IP=\$(kubectl get svc esewa-service -n esewans -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
                    if [ ! -z "\$IP" ]; then
                        echo "Testing: http://\$IP/"
                        curl -s -o /dev/null -w "HTTP Status: %{http_code}\\n" http://\$IP/ || true
                    else
                        echo "No external IP found, using port-forward"
                        timeout 10 kubectl port-forward svc/esewa-service -n esewans 8080:8080 &
                        sleep 3
                        curl -s -o /dev/null -w "HTTP Status: %{http_code}\\n" http://localhost:8080/ || true
                        kill %1 2>/dev/null || true
                    fi
                """
            }
        }
    }

    post {
        success {
            echo "🚀 Deployment successful! Image: ${IMAGE_NAME}:${IMAGE_TAG}"
            // You can add Slack/Teams notification here
        }
        failure {
            echo "❌ Deployment failed"
            // You can add notification here
        }
    }
}