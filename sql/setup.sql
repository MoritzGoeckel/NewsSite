CREATE DATABASE news_site
    WITH
    OWNER = postgres
    ENCODING = 'UTF8'
    CONNECTION LIMIT = -1
    IS_TEMPLATE = False;

CREATE TABLE articles (
	id serial PRIMARY KEY,
	hash VARCHAR (300) UNIQUE NOT NULL,
	head VARCHAR (500) NOT NULL,
	content VARCHAR,
	url VARCHAR (255) NOT NULL,
	source VARCHAR (70) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX hash_idx ON articles (hash);

CREATE TABLE article_details (
	id serial PRIMARY KEY,
	article_id INT,
	title VARCHAR (500) NOT NULL,
	description VARCHAR,
	content VARCHAR,
	summary VARCHAR,
	image VARCHAR (300) NOT NULL,
    url VARCHAR (300) NOT NULL,
	published_at TIMESTAMP NOT NULL,
	created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT fk_articles FOREIGN KEY(article_id) REFERENCES articles(id)
);