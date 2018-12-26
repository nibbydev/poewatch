--
-- A list of somewhat useful queries for database management
--



-- ---------------------------------------------------------------------------------------------------------------------
-- Database migration
-- ---------------------------------------------------------------------------------------------------------------------

--
-- Half-arsed migration script for databases created before 4 Dec 2018
--

-- delete duplicate listings from `league_entries`
delete e2 from league_entries as e2
join (
  -- get groups that have listed more than n of the same item
  select id_l, id_d, account
  from league_entries
  group by id_l, id_d, account
  having count(*) > 1
) as e1 on e1.id_l = e2.id_l and e1.id_d = e2.id_d and e1.account = e2.account

drop index `PRIMARY` ON league_entries;
alter table league_entries drop column id;
alter table league_entries modify account varchar(32) not null after id_d;
alter table league_entries add listings int unsigned not null default 1;
alter table league_entries add id_item varchar(64) not null default '';
alter table league_entries add constraint pk primary key (id_l, id_d, account, id_item);
alter table league_entries modify `time` timestamp not null default current_timestamp;

alter table league_entries add outlier bit(1) not null default 0 after approved;
drop index approved_time on league_entries;
create index outlier_time on league_entries (outlier, `time`);
alter table league_entries drop column approved;

alter table league_items drop column volatile;
alter table league_items drop column multiplier;
alter table league_items drop column `dec`;

alter table data_changeId modify `time` timestamp not null default current_timestamp on update current_timestamp;

--
-- Create separate group for Incursion vials and Bestiary nets
--

BEGIN;
  insert into data_groups(id_cat, `name`, display)
  values (4, 'vial', 'Vials'), (4, 'net', 'Nets');

  update `data_itemData`
  set id_grp = (select id from data_groups where `name` = 'net' limit 1)
  where id_grp = 11 and `name` like '% Net';

  update `data_itemData`
  set id_grp = (select id from data_groups where `name` = 'vial' limit 1)
  where id_grp = 11 and `name` like 'Vial %';
COMMIT;

--
-- Refactor count to total and quantity to daily
--

alter table league_items change quantity daily int(8) unsigned not null default 0;
alter table league_history_daily change quantity daily int(8) unsigned not null default 0;
alter table league_items change `count` total int(16) unsigned not null default 0;
alter table league_history_daily change `count` total int(16) unsigned not null default 0;

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
