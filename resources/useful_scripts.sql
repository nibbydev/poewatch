-- ---------------------------------------------------------------------------------------------------------------------
-- A list of somewhat useful queries for database management
-- ---------------------------------------------------------------------------------------------------------------------




-- ---------------------------------------------------------------------------------------------------------------------
-- Database migration
-- ---------------------------------------------------------------------------------------------------------------------

--
drop index `PRIMARY` ON league_entries;
alter table league_entries add id_item varchar(64) first;
update league_entries set id_item = concat(id, '_', account);
alter table league_entries drop column account;
alter table league_entries drop column id;
alter table league_entries add primary key(id_item);

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