-- ------------------------------------------------------------------------------------------------------
-- Migration script to remove redundant account table and to replace account_crc with id_a in league_entries
-- I should really look into database versioning, huh
-- ------------------------------------------------------------------------------------------------------

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


-- ------------------------------------------------------------------------------------------------------
-- Migration script to fix stats camelCase to camel_case
-- ------------------------------------------------------------------------------------------------------

-- rename invalid casing
alter table data_statistics
    change statType type varchar(32) not null,
    drop index statType,
    add index type (type);
alter table data_statistics_tmp
    change statType type varchar(32) not null;


-- ------------------------------------------------------------------------------------------------------
-- Migration script to fix change_id camelCase to camel_case
-- ------------------------------------------------------------------------------------------------------

-- rename table
rename table data_changeId to data_change_id;

-- rename column
alter table data_change_id
    change changeId change_id varchar(64) not null unique;


-- ------------------------------------------------------------------------------------------------------
-- Migration script to fix data_item_data camelCase to camel_case
-- ------------------------------------------------------------------------------------------------------

-- rename table
rename table data_itemData to data_item_data;

-- change cases
alter table data_item_data
    change tier map_tier TINYINT(1) UNSIGNED DEFAULT NULL after stack,
    change series map_series TINYINT(1) UNSIGNED DEFAULT NULL after map_tier,

    change shaper base_shaper BIT(1) DEFAULT NULL after map_series,
    change elder base_elder BIT(1) DEFAULT NULL after base_shaper,
    change ilvl base_level TINYINT(1) UNSIGNED DEFAULT NULL after base_elder,

    change enchantMin enchant_min DECIMAL(4,1) DEFAULT NULL after base_level,
    change enchantMax enchant_max DECIMAL(4,1) DEFAULT NULL after enchant_min,

    change lvl gem_lvl TINYINT(1) UNSIGNED DEFAULT NULL after enchant_max,
    change quality gem_quality TINYINT(1) UNSIGNED DEFAULT NULL after gem_lvl,
    change corrupted gem_corrupted TINYINT(1) UNSIGNED DEFAULT NULL after gem_quality;

-- ------------------------------------------------------------------------------------------------------
-- Migration script to fix account table not returning IDs
-- ------------------------------------------------------------------------------------------------------

alter table league_accounts
    add updates int unsigned not null default 0 after name;

-- ------------------------------------------------------------------------------------------------------
-- Rename stats in database
-- ------------------------------------------------------------------------------------------------------

update data_statistics set type = "COUNT_API_ERRORS_4XX" where type = "COUNT_API_ERRORS_429";
update data_statistics_tmp set type = "COUNT_API_ERRORS_4XX" where type = "COUNT_API_ERRORS_429";
