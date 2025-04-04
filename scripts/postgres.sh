sudo -i -u postgres
psql

sudo -u postgres psql

createdb sammy


sudo adduser sammy
sudo -i -u sammy
psql

# Port is 5432
psql -U posgres -W postgres

# Rename column
ALTER TABLE articles RENAME COLUMN created TO created_at;

# Change column type
ALTER TABLE tab_name ALTER COLUMN col_1 TYPE new_data_type;

ALTER TABLE articles ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP

ALTER TABLE tbl_name DROP COLUMN col_name;

ALTER TABLE articles ADD UNIQUE (url);

DELETE FROM articles WHERE url='https://www.faz.net/aktuell/politik/ukraine-liveticker-russischer-geheimdienst-anschlag-auf-krim-chef-vereitelt-faz-18495964.html'

# Delete duplicates
DELETE FROM articles a using articles b WHERE a.hash > b.hash AND a.url = b.url;