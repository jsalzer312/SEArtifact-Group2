### =========================================== ###
### Shell script to setup the users environment ###
### =========================================== ###

# Determines the differe SDK version the user can install
echo "\n \t\t~~~Determining available SDK installations~~~"
apt search openjdk | grep -E 'openjdk-.*-jdk/'

# Installs SDK version 11.0.*
echo "\n \t \t~~~Installing OpenJDK 11~~~"
sudo apt install openjdk-11-jdk

echo "\n \t \t~~~Checking Java Version~~~"
java --version

# Installs python version 3.10.*
echo "\n \t \t~~~Installing Python3.10.12~~~"
sudo apt install python3.10
echo "\n \t \t~~~Checking Python3 Version~~~"
python3 --version

# Download and install Maven 3.6.3
echo "\n \t \t~~~Downloading and Installing Maven 3.6.3~~~"
sudo apt install maven -y

# Setup up Maven with the current path variables
echo "\n \t \t~~~Setting M2_HOME and Path Variables~~~"
echo "export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64\nexport PATH=\$PATH:\$JAVA_HOME/bin\nexport MAVEN_HOME=/opt/maven\nexport PATH=\$PATH:\$MAVEN_HOME/bin" >> ~/.bashrc
source ~/.bashrc

# Display Maven version
echo "\n \t \t~~~Checking Maven Version~~~"
mvn -version

# Installs latest version of pip
echo "\n \t \t~~~Installing pip~~~"
sudo apt install python3-pip -y
echo "\n \t \t~~~Checking pip3 Version~~~"
pip3  --version

# Installs required packages listed in requirements.txt
echo "\n \t \t~~~Installing required packages~~~"
pip install -r requirements.txt