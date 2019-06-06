--
-- Migration script to remove redundant account table and to replace account_crc with id_a in league_entries
-- I should really look into database versioning, huh
--

-- create computed crc column (15 sec)
alter table account_accounts
    add crc int unsigned as (crc32(name)) stored,
    add index crc (crc);

-- create a new column for account id (6 min)
alter table league_entries
    add id_a bigint unsigned not null after id_d,
    add index id_a (id_a);

-- fill new entry id field (7 min)
update league_entries as le
    join account_accounts as aa
    on le.account_crc = aa.crc
set le.id_a = aa.id;

-- remove any that didn't match (20 sec)
delete from league_entries where id_a = 0;

-- link entries to their accounts via crc (10 min)
alter table league_entries
    add foreign key (id_a) references account_accounts(id);

-- create id_l index so we can drop PK (2 min)
alter table league_entries
    add index id_l (id_l);

-- drop pk so we can replace account crc with id in pk (~10 min)
alter table league_entries drop primary key;

-- recreate primary key with id (8 min)
alter table league_entries
    add primary key (id_l, id_d, id_a, item_crc) using btree;

-- drop the unused crc column (10 min)
alter table league_entries drop account_crc;
-- drop the unused crc column (5 sec)
alter table account_accounts drop crc;

-- won't be using this table anymore
drop table league_accounts;

-- rename tables
rename table account_accounts to league_accounts;
rename table account_characters to league_characters;
