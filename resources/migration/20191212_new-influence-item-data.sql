--
-- Alters table `data_item_data` to support new influence types
-- https://www.pathofexile.com/forum/view-thread/2687767
--

alter table data_item_data
    drop index idx_unique;

alter table data_item_data
    add column shaper BIT(1) DEFAULT NULL,
    add column elder BIT(1) DEFAULT NULL,
    add column crusader BIT(1) DEFAULT NULL,
    add column redeemer BIT(1) DEFAULT NULL,
    add column hunter BIT(1) DEFAULT NULL,
    add column warlord BIT(1) DEFAULT NULL;

UPDATE data_item_data
SET shaper = base_shaper,
    elder  = base_elder
WHERE base_shaper != null
  and base_elder != null;


alter table data_item_data
    drop column base_shaper,
    drop column base_elder;



