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
