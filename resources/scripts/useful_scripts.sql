--
-- A list of somewhat useful queries for database management
--

-- ---------------------------------------------------------------------------------------------------------------------
-- Utility
-- ---------------------------------------------------------------------------------------------------------------------

-- Select all entries from database related to every item that should have a variant but for some reason don't
select did.*
from data_itemData as did
         join (
    select `name`, `type`, frame
    from data_itemData
    where var is not null
      and id_cat != 12
      and id_cat != 5
    order by data_itemData.id desc
) as tmp on tmp.name = did.name and tmp.type = did.type and tmp.frame = did.frame
where did.var is null
order by id desc;

-- ---------------------------------------------------------------------------------------------------------------------
-- Delete old entries
-- ---------------------------------------------------------------------------------------------------------------------

-- drop foreign keys
alter table league_entries
    drop foreign key fk_id_price,
    drop foreign key league_entries_ibfk_1,
    drop foreign key league_entries_ibfk_2,
    drop foreign key league_entries_ibfk_3;

-- drop indices
alter table league_entries
    drop index fk_id_price,
    drop index updated,
    drop index discovered,
    drop index id_d,
    algorithm = inplace;


-- delete rows
delete
from league_entries
where stash_crc is null
  and updated < subdate(now(), interval 7 day);

-- recreate indices
alter table league_entries
    add index discovered (discovered),
    add index updated (updated);

-- recreate foreign keys
alter table league_entries
    add foreign key (id_l) references data_leagues (id),
    add foreign key (id_d) references data_itemData (id),
    add foreign key (id_a) references league_accounts (id),
    add foreign key (id_price) references data_itemData (id);
