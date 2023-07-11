CREATE DATABASE news_site
    WITH
    OWNER = postgres
    ENCODING = 'UTF8'
    CONNECTION LIMIT = -1
    IS_TEMPLATE = False;

CREATE TABLE articles (
	hash VARCHAR (300) PRIMARY KEY,
	head VARCHAR (500) NOT NULL,
	content VARCHAR,
	url VARCHAR (255) UNIQUE NOT NULL,
	source VARCHAR (70) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE article_details (
    article_url VARCHAR (300) PRIMARY KEY,
    url VARCHAR (300),
	title VARCHAR (500) NOT NULL,
	description VARCHAR,
	content VARCHAR,
	summary VARCHAR,
	image VARCHAR (300),
	image_metadata VARCHAR (500),
	published_at TIMESTAMP,
	created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT fk_articles FOREIGN KEY(article_url) REFERENCES articles(url)
);