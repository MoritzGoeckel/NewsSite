DROP TABLE articles;
DROP TABLE originals;
DROP INDEX idx_articles_hash;

CREATE DATABASE news_site
    WITH
    OWNER = postgres
    ENCODING = 'UTF8'
    CONNECTION LIMIT = -1
    IS_TEMPLATE = False;

CREATE TABLE originals (
    id integer primary key generated always as identity,
    url VARCHAR (600) NOT NULL DEFAULT '',
    head VARCHAR (500) NOT NULL,
    teaser VARCHAR NOT NULL,
    content VARCHAR NOT NULL,
    media VARCHAR DEFAULT '',
    raw_in VARCHAR NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO originals (head, teaser, content, raw_in) VALUES ('', '', '', '');

CREATE TABLE articles (
	id integer primary key generated always as identity,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    -- base
	hash VARCHAR (351) UNIQUE NOT NULL,
	preview_head VARCHAR (503) NOT NULL,
	preview_content VARCHAR DEFAULT '',
	preview_url VARCHAR (458) UNIQUE NOT NULL,
	source VARCHAR (70) NOT NULL,
	-- originals
	original_id integer DEFAULT 1,
    CONSTRAINT fk_original
          FOREIGN KEY(original_id)
          REFERENCES originals(id),
    -- details
    head VARCHAR(501) DEFAULT '',
    description VARCHAR DEFAULT '',
    content VARCHAR DEFAULT '',
	url VARCHAR (456) DEFAULT '',
    image VARCHAR(802) DEFAULT '',
	image_metadata VARCHAR (502) DEFAULT '',
	published_at TIMESTAMP
);

CREATE UNIQUE INDEX idx_articles_hash ON articles(hash);
