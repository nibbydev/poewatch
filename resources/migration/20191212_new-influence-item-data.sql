--
-- Alters table `data_item_data` to support new influence types
-- https://www.pathofexile.com/forum/view-thread/2687767
--

alter table data_item_data
    drop index idx_unique;

alter table data_item_data
    add column shaper BIT(1) DEFAULT NULL after base_elder,
    add column elder BIT(1) DEFAULT NULL after shaper,
    add column crusader BIT(1) DEFAULT NULL after elder,
    add column redeemer BIT(1) DEFAULT NULL after crusader,
    add column hunter BIT(1) DEFAULT NULL after redeemer,
    add column warlord BIT(1) DEFAULT NULL after hunter;

UPDATE data_item_data
SET shaper = base_shaper,
    elder  = base_elder
WHERE base_shaper is not null
  or base_elder is not null;


alter table data_item_data
    drop column base_shaper,
    drop column base_elder;



