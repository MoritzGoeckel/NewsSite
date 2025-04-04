sudo apt update
sudo apt install postgresql postgresql-contrib -y
sudo systemctl start postgresql.service

sudo apt install default-jre


# Mac
# Brew
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Postgres
brew install postgresql
brew services start postgresql
brew services stop postgresql

brew install ollama