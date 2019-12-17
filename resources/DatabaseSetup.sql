-- --------------------------------------------------------------------------------------------------------------------
-- Base values
-- --------------------------------------------------------------------------------------------------------------------

--
-- Base values for data_leagues
--

INSERT INTO data_leagues
    (id, active, hardcore, name, display)
VALUES (1, 1, 1, 'Hardcore', 'Hardcore'),
       (2, 1, 0, 'Standard', 'Standard'),
       (3, 0, 1, 'Hardcore Breach', 'HC Breach'),
       (4, 0, 0, 'Breach', 'Breach'),
       (5, 0, 1, 'Hardcore Legacy', 'HC Legacy'),
       (6, 0, 0, 'Legacy', 'Legacy'),
       (7, 0, 1, 'Hardcore Harbinger', 'HC Harbinger'),
       (8, 0, 0, 'Harbinger', 'Harbinger'),
       (9, 0, 1, 'Hardcore Abyss', 'HC Abyss'),
       (10, 0, 0, 'Abyss', 'Abyss'),
       (11, 0, 1, 'Hardcore Bestiary', 'HC Bestiary'),
       (12, 0, 0, 'Bestiary', 'Bestiary'),
       (13, 0, 1, 'Hardcore Incursion', 'HC Incursion'),
       (14, 0, 0, 'Incursion', 'Incursion'),
       (15, 0, 1, 'Incursion Event HC (IRE002)', 'HC Incursion Event'),
       (16, 0, 0, 'Incursion Event (IRE001)', 'Incursion Event'),
       (17, 0, 1, 'Hardcore Delve', 'HC Delve'),
       (18, 0, 0, 'Delve', 'Delve'),
       (19, 0, 1, 'Hardcore Betrayal', 'HC Betrayal'),
       (20, 0, 0, 'Betrayal', 'Betrayal'),
       (21, 0, 1, 'Hardcore Synthesis', 'HC Synthesis'),
       (22, 0, 0, 'Synthesis', 'Synthesis');

--
-- Base value for data_change_id
--

INSERT INTO data_change_id
    (change_id)
VALUES ('0-0-0-0-0');

--
-- Base value for data_groups
--

INSERT INTO data_groups (id, name, display)
VALUES (1, 'amulet', 'Amulets'),
       (2, 'belt', 'Belts'),
       (3, 'ring', 'Rings'),
       (4, 'boots', 'Boots'),
       (5, 'chest', 'Body Armours'),
       (6, 'gloves', 'Gloves'),
       (7, 'helmet', 'Helmets'),
       (8, 'quiver', 'Quivers'),
       (9, 'shield', 'Shields'),
       (10, 'card', 'Divination cards'),
       (11, 'currency', 'Currency'),
       (12, 'essence', 'Essences'),
       (13, 'piece', 'Harbinger pieces'),
       (14, 'fossil', 'Fossils'),
       (15, 'resonator', 'Resonators'),
       (16, 'vial', 'Vials'),
       (17, 'net', 'Nets'),
       (18, 'flask', 'Flasks'),
       (19, 'skill', 'Skill Gems'),
       (20, 'support', 'Support Gems'),
       (21, 'vaal', 'Vaal Gems'),
       (22, 'jewel', 'Jewels'),
       (23, 'map', 'Regular Maps'),
       (24, 'fragment', 'Fragments'),
       (25, 'unique', 'Unique Maps'),
       (26, 'scarab', 'Scarabs'),
       (27, 'prophecy', 'Prophecies'),
       (28, 'bow', 'Bows'),
       (29, 'claw', 'Claws'),
       (30, 'dagger', 'Daggers'),
       (31, 'oneaxe', '1H Axes'),
       (32, 'onemace', '1H Maces'),
       (33, 'onesword', '1H Swords'),
       (34, 'rod', 'Rods'),
       (35, 'sceptre', 'Sceptres'),
       (36, 'staff', 'Staves'),
       (37, 'twoaxe', '2H Axes'),
       (38, 'twomace', '2H Maces'),
       (39, 'twosword', '2H Swords'),
       (40, 'wand', 'Wands'),
       (41, 'incubator', 'Incubators'),
       (42, 'splinter', 'Splinters'),
       (43, 'runedagger', 'Runedaggers'),
       (44, 'warstaff', 'Warstaves'),
       (45, 'oil', 'Oils'),
       (46, 'beast', 'Beasts'),
       (47, 'sample', 'Samples'),
       (48, 'catalyst', 'Catalysts'),
       (49, 'influence', 'Influence');

--
-- Base value for data_categories
--

INSERT INTO data_categories (id, name, display)
VALUES (1, 'accessory', 'Accessories'),
       (2, 'armour', 'Armour'),
       (3, 'card', 'Divination cards'),
       (4, 'currency', 'Currency'),
       (5, 'enchantment', 'Enchants'),
       (6, 'flask', 'Flasks'),
       (7, 'gem', 'Gems'),
       (8, 'jewel', 'Jewels'),
       (9, 'map', 'Maps'),
       (10, 'prophecy', 'Prophecy'),
       (11, 'weapon', 'Weapons'),
       (12, 'base', 'Crafting Bases'),
       (13, 'beast', 'Beasts');

-- --------------------------------------------------------------------------------------------------------------------
-- User accounts
-- --------------------------------------------------------------------------------------------------------------------

DROP USER IF EXISTS 'pw_app'@'localhost';
CREATE USER 'pw_app'@'localhost' IDENTIFIED BY 'password goes here';
GRANT ALL PRIVILEGES ON pw.* TO 'pw_app'@'localhost';

DROP USER IF EXISTS 'pw_web'@'localhost';
CREATE USER 'pw_web'@'localhost' IDENTIFIED BY 'password goes here';
GRANT SELECT ON pw.* TO 'pw_web'@'localhost';
GRANT INSERT ON pw.web_feedback TO 'pw_web'@'localhost';

FLUSH PRIVILEGES;
