--
-- A list of somewhat useful queries for database management
--

-- ---------------------------------------------------------------------------------------------------------------------
-- Database migration: simplify account tables
-- ---------------------------------------------------------------------------------------------------------------------

alter table account_characters add column id_a BIGINT UNSIGNED NOT NULL after id;
alter table account_characters add column id_l SMALLINT UNSIGNED NOT NULL after id;

update account_characters as ac
  join account_relations as ar
  on ac.id = ar.id_c
set ac.id_l = ar.id_l, ac.id_a = ar.id_a;

delete from account_characters where id_a = 0 or id_l = 0;

ALTER TABLE account_characters ADD CONSTRAINT fk_id_l FOREIGN KEY (id_l) REFERENCES data_leagues(id) ON DELETE CASCADE;
ALTER TABLE account_characters ADD CONSTRAINT fk_id_a FOREIGN KEY (id_a) REFERENCES account_accounts(id) ON DELETE CASCADE;

drop table account_relations;
drop table account_history;

-- ---------------------------------------------------------------------------------------------------------------------
-- Database migration: rename league entries columns
-- ---------------------------------------------------------------------------------------------------------------------

alter table league_entries change found discovered TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
alter table league_entries change seen updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- ---------------------------------------------------------------------------------------------------------------------
-- Utility
-- ---------------------------------------------------------------------------------------------------------------------

-- Select all entries from database related to every item that should have a variant but for some reason don't
select did.* from data_itemData as did
join (
  select `name`, `type`, frame from data_itemData
  where var is not null and id_cat != 12 and id_cat != 5
  order by data_itemData.id desc
) as tmp on tmp.name = did.name and tmp.type = did.type and tmp.frame = did.frame
where did.var is null
order by id desc;

-- ---------------------------------------------------------------------------------------------------------------------
-- Development
-- ---------------------------------------------------------------------------------------------------------------------

--
-- Update outlier flag for all items in the `league_entries` table that have had entries added in the past cycle
--

-- get ids that have been added in the past cycle
select distinct id_l, id_d
from league_entries
where time > date_sub(now(), interval 60 second);

-- get trimming medians for all items
select id_l, id_d,
  median(price) as median,
  median(price) / 1.5 as minMed,
  median(price) * 1.2 as maxMed
from league_entries
group by id_l, id_d
having median > 0;

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
having median > 0;

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
set e4.outlier = IF(e4.price between e3.minMed and e3.maxMed, 0, 1);
