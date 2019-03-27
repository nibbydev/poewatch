--
-- A list of somewhat useful queries for database management
--

-- ---------------------------------------------------------------------------------------------------------------------
-- Database migration: create challenge flag in data_leagues
-- ---------------------------------------------------------------------------------------------------------------------

alter table data_leagues add challenge tinyint(1) unsigned not null default 0 after hardcore;
update data_leagues set challenge = 1 where id > 2 and event = 0;

-- ---------------------------------------------------------------------------------------------------------------------
-- Database migration: move over to CRC32 hashes
-- ---------------------------------------------------------------------------------------------------------------------

-- Add crc columns
alter table league_entries add accCrc int unsigned not null after id_d;
alter table league_entries add itmCrc int unsigned not null after accCrc;

-- Add values to crc columns
update league_entries set accCrc = CRC32(account);
update league_entries set itmCrc = CRC32(id_item) where id_item != "";

-- Recreate primary key
alter table league_entries drop primary key;
alter table league_entries add primary key(id_l, id_d, accCrc, itmCrc);

-- Drop redundant columns
alter table league_entries drop column account;
alter table league_entries drop column id_item;

-- ---------------------------------------------------------------------------------------------------------------------
-- Database migration 27.03.19: add shaper/elder/enchantMin/enchantMax fields
-- ---------------------------------------------------------------------------------------------------------------------

-- create new data fields
alter table data_itemData add shaper bit(1) default null after tier;
alter table data_itemData add elder bit(1) default null after shaper;
alter table data_itemData add enchantMin decimal(4,1) default null after elder;
alter table data_itemData add enchantMax decimal(4,1) default null after enchantMin;

-- move shaper/elder to new fields
update data_itemData set shaper = 1, var = null where var = "shaper";
update data_itemData set elder = 1, var = null where var = "elder";

-- move enchant values with decimal points to new fields
update data_itemData set enchantMin = var, enchantMax = var, var = null where id_cat = 5 and var like "%.%";

-- move enchant values with negative values to new fields
update data_itemData set enchantMin = var, enchantMax = var, var = null where id_cat = 5 and var like "-%";

-- move enchant values with ranges to new fields
update data_itemData set
  enchantMin = substring_index(var, '-', 1),
  enchantMax = substring_index(var, '-', -1),
  var = null
where id_cat = 5 and var like "%-%";

-- move enchant values that are just regular numbers to new fields
update data_itemData set enchantMin = var, enchantMax = var, var = null where id_cat = 5 and var is not null;

-- recreate the unique constraint
alter table data_itemdata drop index idx_unique;
alter table data_itemdata add constraint idx_unique UNIQUE (name, type, frame, tier, lvl, quality, corrupted, links, ilvl, var, shaper, elder, enchantMin, enchantMax);

-- ---------------------------------------------------------------------------------------------------------------------
-- Database migration 27.03.19: redirect group ids
-- ---------------------------------------------------------------------------------------------------------------------

alter table data_itemdata drop foreign key data_itemData_ibfk_2;
alter table data_groups drop foreign key data_groups_ibfk_1;
alter table data_groups drop column id_cat;
truncate table data_groups;
update data_itemdata set id_grp = id_grp + 100;

-- enchantments
update data_itemdata set id_grp = 4 where id_grp = 118;
update data_itemdata set id_grp = 6 where id_grp = 119;
update data_itemdata set id_grp = 7 where id_grp = 120;

-- flask
update data_itemdata set id_grp = 18 where id_grp = 121;

-- gem
update data_itemdata set id_grp = 19 where id_grp = 122;
update data_itemdata set id_grp = 20 where id_grp = 123;
update data_itemdata set id_grp = 21 where id_grp = 124;

-- jewel
update data_itemdata set id_grp = 22 where id_grp = 125;

-- map
update data_itemdata set id_grp = 23 where id_grp = 126;
update data_itemdata set id_grp = 24 where id_grp = 127;
update data_itemdata set id_grp = 25 where id_grp = 128;
update data_itemdata set id_grp = 26 where id_grp = 165;

-- prophecy
update data_itemdata set id_grp = 27 where id_grp = 129;

-- weapon
update data_itemdata set id_grp = id_grp - 102
where id_grp >= 130 and id_grp <= 142;

-- bases
update data_itemdata set id_grp = 3 where id_grp = 143;
update data_itemdata set id_grp = 2 where id_grp = 144;
update data_itemdata set id_grp = 1 where id_grp = 145;
update data_itemdata set id_grp = 7 where id_grp = 146;
update data_itemdata set id_grp = 5 where id_grp = 147;
update data_itemdata set id_grp = 6 where id_grp = 148;
update data_itemdata set id_grp = 4 where id_grp = 149;
update data_itemdata set id_grp = 32 where id_grp = 150;
update data_itemdata set id_grp = 35 where id_grp = 151;
update data_itemdata set id_grp = 28 where id_grp = 152;
update data_itemdata set id_grp = 40 where id_grp = 153;
update data_itemdata set id_grp = 33 where id_grp = 154;
update data_itemdata set id_grp = 29 where id_grp = 155;
update data_itemdata set id_grp = 9 where id_grp = 156;
update data_itemdata set id_grp = 30 where id_grp = 157;
update data_itemdata set id_grp = 39 where id_grp = 158;
update data_itemdata set id_grp = 36 where id_grp = 159;
update data_itemdata set id_grp = 31 where id_grp = 160;
update data_itemdata set id_grp = 8 where id_grp = 161;
update data_itemdata set id_grp = 37 where id_grp = 162;
update data_itemdata set id_grp = 38 where id_grp = 163;
update data_itemdata set id_grp = 22 where id_grp = 164;
update data_itemdata set id_grp = 34 where id_grp = 167;

-- net
update data_itemdata set id_grp = 17 where id_grp = 117;

-- leaguestone
delete from data_itemdata where id_grp = 166;

-- the rest
update data_itemdata set id_grp = id_grp - 100
where id_grp > 100;

-- recreate foreign key
ALTER TABLE data_itemdata ADD CONSTRAINT data_itemData_ibfk_2 FOREIGN KEY (id_grp) REFERENCES data_groups(id) ON DELETE CASCADE;

-- ---------------------------------------------------------------------------------------------------------------------
-- Utility
-- ---------------------------------------------------------------------------------------------------------------------

-- Select all entries from database related to every item that should have a variant but for some reason don't
select did.* from data_itemdata as did
join (
  select `name`, `type`, frame from data_itemdata
  where var is not null and id_cat != 12 and id_cat != 5
  order by data_itemdata.id desc
) as tmp on tmp.name = did.name and tmp.type = did.type and tmp.frame = did.frame
where did.var is null
order by id desc

-- ---------------------------------------------------------------------------------------------------------------------
-- Development
-- ---------------------------------------------------------------------------------------------------------------------

--
-- Update outlier flag for all items in the `league_entries` table that have had entries added in the past cycle
--

-- get ids that have been added in the past cycle
select distinct id_l, id_d
from league_entries
where time > date_sub(now(), interval 60 second)

-- get trimming medians for all items
select id_l, id_d,
  median(price) as median,
  median(price) / 1.5 as minMed,
  median(price) * 1.2 as maxMed
from league_entries
group by id_l, id_d
having median > 0

-- get trimming medians of items that have new entries in the past cycle
select e1.id_l, e1.id_d,
  median(e1.price) as median,
  median(e1.price) / 1.5 as minMed,
  median(e1.price) * 1.2 as maxMed
from league_entries as e1
join (
  -- get ids that have been added in the past cycle
  select distinct id_l, id_d
  from league_entries
  where time > date_sub(now(), interval 60 second)
) as e2 on e2.id_l = e1.id_l and e2.id_d = e1.id_d
group by e1.id_l, e1.id_d
having median > 0

-- update outlier state for items that have new entries in the past cycle
update league_entries as e4
join (
  -- get trimming medians of items that have new entries in the past cycle
  select e1.id_l, e1.id_d,
    median(e1.price) / 1.5 as minMed,
    median(e1.price) * 1.2 as maxMed
  from league_entries as e1
  join (
    -- get ids that have been added in the past cycle
    select distinct id_l, id_d
    from league_entries
    where time > date_sub(now(), interval 60 second)
  ) as e2 on e2.id_l = e1.id_l and e2.id_d = e1.id_d
  group by e1.id_l, e1.id_d
  having median(e1.price) > 0
) as e3 on e4.id_l = e3.id_l and e4.id_d = e3.id_d
set e4.outlier = IF(e4.price between e3.minMed and e3.maxMed, 0, 1)
