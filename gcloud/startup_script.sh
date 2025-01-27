#! /bin/bash

if [ ! -f "/etc/initialized_on_startup" ]; then
    echo "Launching the VM for the first time."

    sudo apt-get update
    sudo apt-get --yes --force-yes install zip unzip

    export SDKMAN_DIR="/usr/local/sdkman" && curl -s "https://get.sdkman.io" | bash
    source "/usr/local/sdkman/bin/sdkman-init.sh" 
    sdk install java 17.0.4-zulu
    sdk use java 17.0.4-zulu

    wget https://dlcdn.apache.org/maven/maven-3/3.8.6/binaries/apache-maven-3.8.6-bin.zip
    sudo mkdir /etc/apache-maven
    sudo unzip apache-maven-3.8.6-bin.zip -d /etc/apache-maven
    export PATH=/etc/apache-maven/apache-maven-3.8.6/bin:$PATH
    rm apache-maven-3.8.6-bin.zip

    sudo mkdir /opt/messenger
    sudo chmod -R 777 /opt/messenger 
    git clone https://github.com/YugabyteDB-Samples/geo-distributed-messenger-spring-cloud.git /opt/messenger

    #Build config and discovery servers
    sudo mkdir /opt/messenger-config
    cd /opt/messenger/config-server
    mvn clean package -Pprod
    
    cd /opt/messenger/discovery-server
    mvn clean package

    #Create application executables
    cd /opt/messenger/messenger
    mvn clean package -Pprod
    cd /opt/messenger/attachments
    mvn clean package -Pprod

    sudo touch /etc/initialized_on_startup
else
# Executed on restarts
export PATH=/etc/apache-maven/apache-maven-3.8.6/bin:$PATH
export SDKMAN_DIR="/usr/local/sdkman"
source "/usr/local/sdkman/bin/sdkman-init.sh"
fi

export PROJECT_ID=$(curl http://metadata.google.internal/computeMetadata/v1/instance/attributes/PROJECT_ID -H "Metadata-Flavor: Google")
export REGION=$(curl http://metadata.google.internal/computeMetadata/v1/instance/attributes/REGION -H "Metadata-Flavor: Google")

# Configuring env variable for the Messaging microservice
export DB_PRIMARY_ENDPOINT=$(curl http://metadata.google.internal/computeMetadata/v1/instance/attributes/DB_PRIMARY_ENDPOINT -H "Metadata-Flavor: Google")
export DB_ADDITIONAL_ENDPOINTS=$(curl http://metadata.google.internal/computeMetadata/v1/instance/attributes/DB_ADDITIONAL_ENDPOINTS -H "Metadata-Flavor: Google")
export DB_USER=$(curl http://metadata.google.internal/computeMetadata/v1/instance/attributes/DB_USER -H "Metadata-Flavor: Google")
export DB_PWD=$(curl http://metadata.google.internal/computeMetadata/v1/instance/attributes/DB_PWD -H "Metadata-Flavor: Google")
export DB_MODE=$(curl http://metadata.google.internal/computeMetadata/v1/instance/attributes/DB_MODE -H "Metadata-Flavor: Google")
export DB_SCHEMA_FILE=$(curl http://metadata.google.internal/computeMetadata/v1/instance/attributes/DB_SCHEMA_FILE -H "Metadata-Flavor: Google")


nohup java -jar /opt/messenger/discovery-server/target/discovery-server-1.0.0-SNAPSHOT.jar &
nohup java -jar /opt/messenger/config-server/target/config-server-1.0.0-SNAPSHOT.jar &

sleep 15

nohup java -jar /opt/messenger/messenger/target/messenger-1.0.0-SNAPSHOT.jar &
nohup java -jar /opt/messenger/attachments/target/attachments-1.0.0-SNAPSHOT.jar &


