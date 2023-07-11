sudo -i -u postgres
psql

sudo -u postgres psql

createdb sammy


sudo adduser sammy
sudo -i -u sammy
psql


# Rename column
ALTER TABLE articles RENAME COLUMN created TO created_at;

# Change column type
ALTER TABLE tab_name ALTER COLUMN col_1 TYPE new_data_type;

ALTER TABLE articles ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP