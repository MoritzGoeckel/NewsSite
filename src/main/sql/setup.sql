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
	created TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX hash_idx ON articles (hash);