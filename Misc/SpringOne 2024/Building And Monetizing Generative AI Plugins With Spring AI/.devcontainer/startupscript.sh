echo "Starting script"

mkdir ~/postgres-volume

sudo apt update

# Install nvm
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.5/install.sh | bash
source ~/.bashrc
nvm install 18
nvm use 18

# Install npm
sudo apt install -y npm

# Install java/spring dependencies
mvn clean install -f openai-gpt-plugin-pgvector/backend
