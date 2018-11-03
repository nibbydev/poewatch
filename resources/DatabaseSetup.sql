-- --------------------------------------------------------------------------------------------------------------------
-- Initial configuration
-- --------------------------------------------------------------------------------------------------------------------

SET time_zone = "+00:00";

--
-- Database: pw
--
DROP DATABASE IF EXISTS pw;
CREATE DATABASE IF NOT EXISTS pw DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;

USE pw;

-- --------------------------------------------------------------------------------------------------------------------
-- Data tables
-- --------------------------------------------------------------------------------------------------------------------

--
-- Table structure data_categories
--

CREATE TABLE data_categories (
    id       INT          UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    name     VARCHAR(32)  NOT NULL UNIQUE,
    display  VARCHAR(32)  DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure data_groups
--

CREATE TABLE data_groups (
    id       INT          UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    id_cat   INT          UNSIGNED NOT NULL,
    name     VARCHAR(32)  NOT NULL,
    display  VARCHAR(32)  DEFAULT NULL,

    FOREIGN KEY (id_cat) REFERENCES data_categories (id) ON DELETE CASCADE,

    INDEX name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure data_leagues
--

CREATE TABLE data_leagues (
    id       SMALLINT     UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    active   TINYINT(1)   UNSIGNED NOT NULL DEFAULT 1,
    upcoming TINYINT(1)   UNSIGNED NOT NULL DEFAULT 0,
    event    TINYINT(1)   UNSIGNED NOT NULL DEFAULT 0,
    hardcore TINYINT(1)   UNSIGNED NOT NULL DEFAULT 0,
    name     VARCHAR(64)  NOT NULL UNIQUE,
    display  VARCHAR(64)  DEFAULT NULL,
    start    VARCHAR(32)  DEFAULT NULL,
    end      VARCHAR(32)  DEFAULT NULL,

    INDEX active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure data_changeId
--

CREATE TABLE data_changeId (
    changeId  VARCHAR(64)  NOT NULL UNIQUE,
    time      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure data_currencyItems
--

CREATE TABLE data_currencyItems (
    id    INT          UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    name  VARCHAR(64)  NOT NULL,

    INDEX name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure data_currencyAliases
--

CREATE TABLE data_currencyAliases (
    id     INT          UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    id_ci  INT          UNSIGNED NOT NULL,
    name   VARCHAR(32)  NOT NULL,

    FOREIGN KEY (id_ci) REFERENCES data_currencyItems (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure data_timers
--

CREATE TABLE data_timers (
    `key`   VARCHAR(32)  NOT NULL,
    type    TINYINT(1)   UNSIGNED DEFAULT NULL,
    time    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delay   BIGINT       UNSIGNED NOT NULL,

    CONSTRAINT pk PRIMARY KEY (`key`, time),

    INDEX `key` (`key`),
    INDEX time  (time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure data_baseNames
--

CREATE TABLE data_baseNames (
    category  VARCHAR(32)  NOT NULL,
    base      VARCHAR(32)  NOT NULL UNIQUE,

    FOREIGN KEY (category) REFERENCES data_groups (name) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------------------------------------------------------
-- Item data
-- --------------------------------------------------------------------------------------------------------------------

--
-- Table structure data_itemData
--

CREATE TABLE data_itemData (
    id         INT           UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    id_cat     INT           UNSIGNED NOT NULL,
    id_grp     INT           UNSIGNED DEFAULT NULL,
    name       VARCHAR(128)  NOT NULL,
    type       VARCHAR(64)   DEFAULT NULL,
    frame      TINYINT(1)    NOT NULL,
    tier       TINYINT(1)    UNSIGNED DEFAULT NULL,
    lvl        TINYINT(1)    UNSIGNED DEFAULT NULL,
    quality    TINYINT(1)    UNSIGNED DEFAULT NULL,
    corrupted  TINYINT(1)    UNSIGNED DEFAULT NULL,
    links      TINYINT(1)    UNSIGNED DEFAULT NULL,
    ilvl       TINYINT(1)    UNSIGNED DEFAULT NULL,
    var        VARCHAR(32)   DEFAULT NULL,
    icon       VARCHAR(256)  NOT NULL,

    FOREIGN KEY (id_cat) REFERENCES data_categories (id) ON DELETE CASCADE,
    FOREIGN KEY (id_grp) REFERENCES data_groups     (id) ON DELETE CASCADE,

    INDEX frame  (frame),
    INDEX name   (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------------------------------------------------------
-- League tables
-- --------------------------------------------------------------------------------------------------------------------

--
-- Table structure for table league_items_rolling
--

CREATE TABLE league_items_rolling (
    id_l        SMALLINT       UNSIGNED NOT NULL,
    id_d        INT            UNSIGNED NOT NULL,
    time        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    volatile    TINYINT(1)     UNSIGNED NOT NULL DEFAULT 0,
    multiplier  DECIMAL(6,4)   UNSIGNED NOT NULL DEFAULT 2.0,
    mean        DECIMAL(14,8)  UNSIGNED NOT NULL DEFAULT 0.0,
    median      DECIMAL(14,8)  UNSIGNED NOT NULL DEFAULT 0.0,
    mode        DECIMAL(14,8)  UNSIGNED NOT NULL DEFAULT 0.0,
    min         DECIMAL(14,8)  UNSIGNED NOT NULL DEFAULT 0.0,
    max         DECIMAL(14,8)  UNSIGNED NOT NULL DEFAULT 0.0,
    exalted     DECIMAL(14,8)  UNSIGNED NOT NULL DEFAULT 0.0,
    count       INT(16)        UNSIGNED NOT NULL DEFAULT 0,
    quantity    INT(8)         UNSIGNED NOT NULL DEFAULT 0,
    inc         INT(8)         UNSIGNED NOT NULL DEFAULT 0,
    `dec`       INT(8)         UNSIGNED NOT NULL DEFAULT 0,
    spark       VARCHAR(128)   DEFAULT NULL,

    FOREIGN KEY (id_l) REFERENCES data_leagues  (id) ON DELETE RESTRICT,
    FOREIGN KEY (id_d) REFERENCES data_itemData (id) ON DELETE CASCADE,
    CONSTRAINT pk PRIMARY KEY (id_l, id_d),

    INDEX volatile (volatile),
    INDEX `count`  (`count`),
    INDEX median   (median),
    INDEX inc      (inc)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure for table league_items_inactive
--

CREATE TABLE league_items_inactive (
    id_l        SMALLINT       UNSIGNED NOT NULL,
    id_d        INT            UNSIGNED NOT NULL,
    time        TIMESTAMP      NOT NULL,
    mean        DECIMAL(14,8)  UNSIGNED NOT NULL DEFAULT 0.0,
    median      DECIMAL(14,8)  UNSIGNED NOT NULL DEFAULT 0.0,
    mode        DECIMAL(14,8)  UNSIGNED NOT NULL DEFAULT 0.0,
    min         DECIMAL(14,8)  UNSIGNED NOT NULL DEFAULT 0.0,
    max         DECIMAL(14,8)  UNSIGNED NOT NULL DEFAULT 0.0,
    exalted     DECIMAL(14,8)  UNSIGNED NOT NULL DEFAULT 0.0,
    count       INT(16)        UNSIGNED NOT NULL DEFAULT 0,
    quantity    INT(8)         UNSIGNED NOT NULL DEFAULT 0,

    FOREIGN KEY (id_l) REFERENCES data_leagues  (id) ON DELETE RESTRICT,
    FOREIGN KEY (id_d) REFERENCES data_itemData (id) ON DELETE CASCADE,
    CONSTRAINT pk PRIMARY KEY (id_l, id_d)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure league_entries
--

CREATE TABLE league_entries (
    id         INT            UNSIGNED NOT NULL,
    id_l       SMALLINT       UNSIGNED NOT NULL,
    id_d       INT            UNSIGNED NOT NULL,
    time       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved   TINYINT(1)     UNSIGNED NOT NULL DEFAULT 0,
    price      DECIMAL(14,8)  UNSIGNED NOT NULL,
    account    VARCHAR(32)    NOT NULL,

    FOREIGN KEY (id_l) REFERENCES  data_leagues         (id)   ON DELETE RESTRICT,
    FOREIGN KEY (id_d) REFERENCES  league_items_rolling (id_d) ON DELETE CASCADE,

    CONSTRAINT pk PRIMARY KEY (id, account),
    INDEX approved_time (approved, time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------------------------------------------------------
-- League history tables
-- --------------------------------------------------------------------------------------------------------------------

--
-- Table structure league_history_daily_inactive
--

CREATE TABLE league_history_daily_inactive (
    id_l      SMALLINT       UNSIGNED NOT NULL,
    id_d      INT            UNSIGNED NOT NULL,
    time      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mean      DECIMAL(14,8)  UNSIGNED DEFAULT NULL,
    median    DECIMAL(14,8)  UNSIGNED DEFAULT NULL,
    mode      DECIMAL(14,8)  UNSIGNED DEFAULT NULL,
    min       DECIMAL(14,8)  UNSIGNED NOT NULL DEFAULT 0.0,
    max       DECIMAL(14,8)  UNSIGNED NOT NULL DEFAULT 0.0,
    exalted   DECIMAL(14,8)  UNSIGNED DEFAULT NULL,
    count     INT(16)        UNSIGNED DEFAULT NULL,
    quantity  INT(8)         UNSIGNED DEFAULT NULL,

    FOREIGN KEY (id_l) REFERENCES data_leagues  (id) ON DELETE RESTRICT,
    FOREIGN KEY (id_d) REFERENCES data_itemData (id) ON DELETE CASCADE,

    INDEX time (time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure league_history_daily_rolling
--

CREATE TABLE league_history_daily_rolling (
    id_l       SMALLINT       UNSIGNED NOT NULL,
    id_d       INT            UNSIGNED NOT NULL,
    time       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mean       DECIMAL(14,8)  UNSIGNED DEFAULT NULL,
    median     DECIMAL(14,8)  UNSIGNED DEFAULT NULL,
    mode       DECIMAL(14,8)  UNSIGNED DEFAULT NULL,
    min        DECIMAL(14,8)  UNSIGNED NOT NULL DEFAULT 0.0,
    max        DECIMAL(14,8)  UNSIGNED NOT NULL DEFAULT 0.0,
    exalted    DECIMAL(14,8)  UNSIGNED DEFAULT NULL,
    count      INT(16)        UNSIGNED DEFAULT NULL,
    quantity   INT(8)         UNSIGNED DEFAULT NULL,

    FOREIGN KEY (id_l) REFERENCES data_leagues  (id) ON DELETE RESTRICT,
    FOREIGN KEY (id_d) REFERENCES data_itemData (id) ON DELETE CASCADE,

    INDEX time (time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure league_history_hourly_quantity
--

CREATE TABLE league_history_hourly_quantity (
    id_l  SMALLINT   UNSIGNED NOT NULL,
    id_d  INT        UNSIGNED NOT NULL,
    time  TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    inc   INT(8)     UNSIGNED DEFAULT NULL,

    FOREIGN KEY (id_l) REFERENCES data_leagues  (id) ON DELETE RESTRICT,
    FOREIGN KEY (id_d) REFERENCES data_itemData (id) ON DELETE CASCADE,

    INDEX ids  (id_l, id_d),
    INDEX time (time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------------------------------------------------------
-- Account tables
-- --------------------------------------------------------------------------------------------------------------------

--
-- Table structure account_accounts
--

CREATE TABLE account_accounts (
    id     BIGINT       UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    name   VARCHAR(32)  NOT NULL UNIQUE,
    found  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    seen   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX seen (seen)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Table structure account_characters
--

CREATE TABLE account_characters (
    id     BIGINT       UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    name   VARCHAR(32)  NOT NULL UNIQUE,
    found  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    seen   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX seen (seen)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Table structure account_relations
--

CREATE TABLE account_relations (
    id     BIGINT     UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    id_l   SMALLINT   UNSIGNED NOT NULL,
    id_a   BIGINT     UNSIGNED NOT NULL,
    id_c   BIGINT     UNSIGNED NOT NULL,
    found  TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    seen   TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (id_l) REFERENCES data_leagues       (id) ON DELETE RESTRICT,
    FOREIGN KEY (id_a) REFERENCES account_accounts   (id) ON DELETE RESTRICT,
    FOREIGN KEY (id_c) REFERENCES account_characters (id) ON DELETE RESTRICT,
    CONSTRAINT `unique` UNIQUE (id_a, id_c),

    INDEX seen (seen)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure account_history
--

CREATE TABLE account_history (
    id_old  BIGINT     UNSIGNED NOT NULL,
    id_new  BIGINT     UNSIGNED NOT NULL,
    moved   TINYINT    UNSIGNED NOT NULL DEFAULT 1,
    found   TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (id_old) REFERENCES account_accounts (id) ON DELETE RESTRICT,
    FOREIGN KEY (id_new) REFERENCES account_accounts (id) ON DELETE RESTRICT,
    CONSTRAINT `unique` UNIQUE (id_old, id_new)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------------------------------------------------------
-- Base values
-- --------------------------------------------------------------------------------------------------------------------

--
-- Base values for data_leagues
--

INSERT INTO data_leagues
    (id, active, hardcore, name, display)
VALUES
    (1,  1, 1, 'Hardcore',                      'Hardcore'          ),
    (2,  1, 0, 'Standard',                      'Standard'          ),
    (3,  0, 1, 'Hardcore Breach',               'HC Breach'         ),
    (4,  0, 0, 'Breach',                        'Breach'            ),
    (5,  0, 1, 'Hardcore Legacy',               'HC Legacy'         ),
    (6,  0, 0, 'Legacy',                        'Legacy'            ),
    (7,  0, 1, 'Hardcore Harbinger',            'HC Harbinger'      ),
    (8,  0, 0, 'Harbinger',                     'Harbinger'         ),
    (9,  0, 1, 'Hardcore Abyss',                'HC Abyss'          ),
    (10, 0, 0, 'Abyss',                         'Abyss'             ),
    (11, 0, 1, 'Hardcore Bestiary',             'HC Bestiary'       ),
    (12, 0, 0, 'Bestiary',                      'Bestiary'          ),
    (13, 0, 1, 'Hardcore Incursion',            'HC Incursion'      ),
    (14, 0, 0, 'Incursion',                     'Incursion'         ),
    (15, 0, 1, 'Incursion Event HC (IRE002)',   'HC Incursion Event'),
    (16, 0, 0, 'Incursion Event (IRE001)',      'Incursion Event'   ),
    (17, 0, 1, 'Hardcore Delve',                'HC Delve'          ),
    (18, 0, 0, 'Delve',                         'Delve'             );

--
-- Base value for data_changeId
--

INSERT INTO data_changeId
    (changeId)
VALUES
    ('0-0-0-0-0');

--
-- Base values for data_categories
--

INSERT INTO data_categories
    (id, name, display)
VALUES
    (1,   'accessory',      'Accessories'),
    (2,   'armour',         'Armour'),
    (3,   'card',           'Divination cards'),
    (4,   'currency',       'Currency'),
    (5,   'enchantment',    'Enchants'),
    (6,   'flask',          'Flasks'),
    (7,   'gem',            'Gems'),
    (8,   'jewel',          'Jewels'),
    (9,   'map',            'Maps'),
    (10,  'prophecy',       'Prophecy'),
    (11,  'weapon',         'Weapons'),
    (12,  'base',           'Crafting Bases');

--
-- Base values for data_groups
--

INSERT INTO data_groups
    (id, id_cat, name, display)
VALUES
    (1,   1,     'amulet',     'Amulets'),
    (2,   1,     'belt',       'Belts'),
    (3,   1,     'ring',       'Rings'),
    (4,   2,     'boots',      'Boots'),
    (5,   2,     'chest',      'Body Armours'),
    (6,   2,     'gloves',     'Gloves'),
    (7,   2,     'helmet',     'Helmets'),
    (8,   2,     'quiver',     'Quivers'),
    (9,   2,     'shield',     'Shields'),
    (10,  3,     'card',       'Divination cards'),
    (11,  4,     'currency',   'Currency'),
    (12,  4,     'essence',    'Essences'),
    (13,  4,     'piece',      'Harbinger pieces'),
    (14,  4,     'fossil',     'Fossils'),
    (15,  4,     'essence',    'Essences'),
    (16,  4,     'resonator',  'Resonators'),
    (17,  5,     'boots',      'Boots'),
    (18,  5,     'gloves',     'Gloves'),
    (19,  5,     'helmet',     'Helmets'),
    (20,  6,     'flask',      'Flasks'),
    (21,  7,     'skill',      'Skill Gems'),
    (22,  7,     'support',    'Support Gems'),
    (23,  7,     'vaal',       'Vaal Gems'),
    (24,  8,     'jewel',      'Jewels'),
    (25,  9,     'map',        'Regular Maps'),
    (26,  9,     'fragment',   'Fragments'),
    (27,  9,     'unique',     'Unique Maps'),
    (28,  10,    'prophecy',   'Prophecies'),
    (29,  11,    'bow',        'Bows'),
    (30,  11,    'claw',       'Claws'),
    (31,  11,    'dagger',     'Daggers'),
    (32,  11,    'oneaxe',     '1H Axes'),
    (33,  11,    'onemace',    '1H Maces'),
    (34,  11,    'onesword',   '1H Swords'),
    (35,  11,    'rod',        'Rods'),
    (36,  11,    'sceptre',    'Sceptres'),
    (37,  11,    'staff',      'Staves'),
    (38,  11,    'twoaxe',     '2H Axes'),
    (39,  11,    'twomace',    '2H Maces'),
    (40,  11,    'twosword',   '2H Swords'),
    (41,  11,    'wand',       'Wands'),
    (42,  12,    'ring',       'Rings'),
    (43,  12,    'belt',       'Belts'),
    (44,  12,    'amulet',     'Amulets'),
    (45,  12,    'helmet',     'Helmets'),
    (46,  12,    'chest',      'Body Armour'),
    (47,  12,    'gloves',     'Gloves'),
    (48,  12,    'boots',      'Boots');

--
-- Base values for data_currencyItems
--

INSERT INTO data_currencyItems
    (id, name)
VALUES
    (1,   'Chaos Orb'),
    (2,   'Exalted Orb'),
    (3,   'Divine Orb'),
    (4,   'Orb of Alchemy'),
    (5,   'Orb of Fusing'),
    (6,   'Orb of Alteration'),
    (7,   'Regal Orb'),
    (8,   'Vaal Orb'),
    (9,   'Orb of Regret'),
    (10,  'Cartographer''s Chisel'),
    (11,  'Jeweller''s Orb'),
    (12,  'Silver Coin'),
    (13,  'Perandus Coin'),
    (14,  'Orb of Scouring'),
    (15,  'Gemcutter''s Prism'),
    (16,  'Orb of Chance'),
    (17,  'Chromatic Orb'),
    (18,  'Blessed Orb'),
    (19,  'Glassblower''s Bauble'),
    (20,  'Orb of Augmentation'),
    (21,  'Orb of Transmutation'),
    (22,  'Mirror of Kalandra'),
    (23,  'Scroll of Wisdom'),
    (24,  'Portal Scroll'),
    (25,  'Blacksmith''s Whetstone'),
    (26,  'Armourer''s Scrap'),
    (27,  'Apprentice Cartographer''s Sextant'),
    (28,  'Journeyman Cartographer''s Sextant'),
    (29,  'Master Cartographer''s Sextant');

--
-- Base values for data_currencyAliases
--

INSERT INTO data_currencyAliases
    (id_ci, name)
VALUES
    (1,   'chaos'),
    (1,   'choas'),
    (1,   'c'),
    (2,   'exalted'),
    (2,   'exalt'),
    (2,   'exa'),
    (2,   'ex'),
    (3,   'divine'),
    (3,   'div'),
    (4,   'alchemy'),
    (4,   'alch'),
    (4,   'alc'),
    (5,   'fusings'),
    (5,   'fusing'),
    (5,   'fuse'),
    (5,   'fus'),
    (6,   'alts'),
    (6,   'alteration'),
    (6,   'alt'),
    (7,   'regal'),
    (7,   'rega'),
    (8,   'vaal'),
    (9,   'regret'),
    (9,   'regrets'),
    (9,   'regr'),
    (10,  'chisel'),
    (10,  'chis'),
    (10,  'cart'),
    (11,  'jewellers'),
    (11,  'jeweller'),
    (11,  'jew'),
    (12,  'silver'),
    (13,  'coin'),
    (13,  'coins'),
    (13,  'perandus'),
    (14,  'scouring'),
    (14,  'scour'),
    (15,  'gcp'),
    (15,  'gemc'),
    (16,  'chance'),
    (16,  'chanc'),
    (17,  'chrome'),
    (17,  'chrom'),
    (18,  'blessed'),
    (18,  'bless'),
    (18,  'bles'),
    (19,  'glass'),
    (19,  'bauble'),
    (20,  'aug'),
    (21,  'tra'),
    (21,  'trans'),
    (22,  'mirror <disabled>'),
    (22,  'mir <disabled>'),
    (22,  'kal'),
    (23,  'wis'),
    (23,  'wisdom'),
    (24,  'port'),
    (24,  'portal'),
    (25,  'whetstone'),
    (25,  'blacksmith'),
    (25,  'whet'),
    (26,  'armour'),
    (26,  'scrap'),
    (27,  'apprentice-sextant'),
    (27,  'apprentice'),
    (28,  'journeyman-sextant'),
    (28,  'journeyman'),
    (29,  'master-sextant'),
    (29,  'master');

--
-- Base values for data_baseNames
--

INSERT INTO data_baseNames
    (category, base)
VALUES
    ('amulet', 'Blue Pearl Amulet'),
    ('amulet', 'Marble Amulet'),
    ('amulet', 'Jet Amulet'),
    ('amulet', 'Paua Amulet'),
    ('amulet', 'Citrine Amulet'),
    ('amulet', 'Ruby Amulet'),
    ('amulet', 'Coral Amulet'),
    ('amulet', 'Amber Amulet'),
    ('amulet', 'Jade Amulet'),
    ('amulet', 'Lapis Amulet'),
    ('amulet', 'Gold Amulet'),
    ('amulet', 'Onyx Amulet'),
    ('amulet', 'Turquoise Amulet'),
    ('amulet', 'Agate Amulet'),
    ('chest', 'Golden Mantle'),
    ('chest', 'Shabby Jerkin'),
    ('chest', 'Glorious Leather'),
    ('chest', 'Coronal Leather'),
    ('chest', 'Cutthroat''s Garb'),
    ('chest', 'Sharkskin Tunic'),
    ('chest', 'Destiny Leather'),
    ('chest', 'Exquisite Leather'),
    ('chest', 'Zodiac Leather'),
    ('chest', 'Assassin''s Garb'),
    ('chest', 'Strapped Leather'),
    ('chest', 'Buckskin Tunic'),
    ('chest', 'Wild Leather'),
    ('chest', 'Full Leather'),
    ('chest', 'Sun Leather'),
    ('chest', 'Thief''s Garb'),
    ('chest', 'Eelskin Tunic'),
    ('chest', 'Frontier Leather'),
    ('chest', 'Padded Vest'),
    ('chest', 'Crimson Raiment'),
    ('chest', 'Lacquered Garb'),
    ('chest', 'Crypt Armour'),
    ('chest', 'Sentinel Jacket'),
    ('chest', 'Varnished Coat'),
    ('chest', 'Blood Raiment'),
    ('chest', 'Sadist Garb'),
    ('chest', 'Carnal Armour'),
    ('chest', 'Oiled Vest'),
    ('chest', 'Padded Jacket'),
    ('chest', 'Oiled Coat'),
    ('chest', 'Scarlet Raiment'),
    ('chest', 'Waxed Garb'),
    ('chest', 'Bone Armour'),
    ('chest', 'Quilted Jacket'),
    ('chest', 'Sleek Coat'),
    ('chest', 'Simple Robe'),
    ('chest', 'Conjurer''s Vestment'),
    ('chest', 'Spidersilk Robe'),
    ('chest', 'Destroyer Regalia'),
    ('chest', 'Savant''s Robe'),
    ('chest', 'Necromancer Silks'),
    ('chest', 'Occultist''s Vestment'),
    ('chest', 'Widowsilk Robe'),
    ('chest', 'Vaal Regalia'),
    ('chest', 'Silken Vest'),
    ('chest', 'Scholar''s Robe'),
    ('chest', 'Silken Garb'),
    ('chest', 'Mage''s Vestment'),
    ('chest', 'Silk Robe'),
    ('chest', 'Cabalist Regalia'),
    ('chest', 'Sage''s Robe'),
    ('chest', 'Silken Wrap'),
    ('chest', 'Plate Vest'),
    ('chest', 'Sun Plate'),
    ('chest', 'Colosseum Plate'),
    ('chest', 'Majestic Plate'),
    ('chest', 'Golden Plate'),
    ('chest', 'Crusader Plate'),
    ('chest', 'Astral Plate'),
    ('chest', 'Gladiator Plate'),
    ('chest', 'Glorious Plate'),
    ('chest', 'Chestplate'),
    ('chest', 'Copper Plate'),
    ('chest', 'War Plate'),
    ('chest', 'Full Plate'),
    ('chest', 'Arena Plate'),
    ('chest', 'Lordly Plate'),
    ('chest', 'Bronze Plate'),
    ('chest', 'Battle Plate'),
    ('chest', 'Scale Vest'),
    ('chest', 'Full Wyrmscale'),
    ('chest', 'Commander''s Brigandine'),
    ('chest', 'Battle Lamellar'),
    ('chest', 'Dragonscale Doublet'),
    ('chest', 'Desert Brigandine'),
    ('chest', 'Full Dragonscale'),
    ('chest', 'General''s Brigandine'),
    ('chest', 'Triumphant Lamellar'),
    ('chest', 'Light Brigandine'),
    ('chest', 'Scale Doublet'),
    ('chest', 'Infantry Brigandine'),
    ('chest', 'Full Scale Armour'),
    ('chest', 'Soldier''s Brigandine'),
    ('chest', 'Field Lamellar'),
    ('chest', 'Wyrmscale Doublet'),
    ('chest', 'Hussar Brigandine'),
    ('chest', 'Sacrificial Garb'),
    ('chest', 'Chainmail Vest'),
    ('chest', 'Ornate Ringmail'),
    ('chest', 'Chain Hauberk'),
    ('chest', 'Devout Chainmail'),
    ('chest', 'Loricated Ringmail'),
    ('chest', 'Conquest Chainmail'),
    ('chest', 'Elegant Ringmail'),
    ('chest', 'Saint''s Hauberk'),
    ('chest', 'Saintly Chainmail'),
    ('chest', 'Chainmail Tunic'),
    ('chest', 'Ringmail Coat'),
    ('chest', 'Chainmail Doublet'),
    ('chest', 'Full Ringmail'),
    ('chest', 'Full Chainmail'),
    ('chest', 'Holy Chainmail'),
    ('chest', 'Latticed Ringmail'),
    ('chest', 'Crusader Chainmail'),
    ('chest', 'Kaom''s Plate'),
    ('boots', 'Two-Toned Boots'),
    ('boots', 'Golden Caligae'),
    ('boots', 'Avian Slippers'),
    ('boots', 'Rawhide Boots'),
    ('boots', 'Goathide Boots'),
    ('boots', 'Deerskin Boots'),
    ('boots', 'Nubuck Boots'),
    ('boots', 'Eelskin Boots'),
    ('boots', 'Sharkskin Boots'),
    ('boots', 'Shagreen Boots'),
    ('boots', 'Stealth Boots'),
    ('boots', 'Slink Boots'),
    ('boots', 'Wrapped Boots'),
    ('boots', 'Strapped Boots'),
    ('boots', 'Clasped Boots'),
    ('boots', 'Shackled Boots'),
    ('boots', 'Trapper Boots'),
    ('boots', 'Ambush Boots'),
    ('boots', 'Carnal Boots'),
    ('boots', 'Assassin''s Boots'),
    ('boots', 'Murder Boots'),
    ('boots', 'Wool Shoes'),
    ('boots', 'Velvet Slippers'),
    ('boots', 'Silk Slippers'),
    ('boots', 'Scholar Boots'),
    ('boots', 'Satin Slippers'),
    ('boots', 'Samite Slippers'),
    ('boots', 'Conjurer Boots'),
    ('boots', 'Arcanist Slippers'),
    ('boots', 'Sorcerer Boots'),
    ('boots', 'Iron Greaves'),
    ('boots', 'Steel Greaves'),
    ('boots', 'Plated Greaves'),
    ('boots', 'Reinforced Greaves'),
    ('boots', 'Antique Greaves'),
    ('boots', 'Ancient Greaves'),
    ('boots', 'Goliath Greaves'),
    ('boots', 'Vaal Greaves'),
    ('boots', 'Titan Greaves'),
    ('boots', 'Leatherscale Boots'),
    ('boots', 'Ironscale Boots'),
    ('boots', 'Bronzescale Boots'),
    ('boots', 'Steelscale Boots'),
    ('boots', 'Serpentscale Boots'),
    ('boots', 'Wyrmscale Boots'),
    ('boots', 'Hydrascale Boots'),
    ('boots', 'Dragonscale Boots'),
    ('boots', 'Chain Boots'),
    ('boots', 'Ringmail Boots'),
    ('boots', 'Mesh Boots'),
    ('boots', 'Riveted Boots'),
    ('boots', 'Zealot Boots'),
    ('boots', 'Soldier Boots'),
    ('boots', 'Legion Boots'),
    ('boots', 'Crusader Boots'),
    ('boots', 'Kaom''s Greaves'),
    ('gloves', 'Gripped Gloves'),
    ('gloves', 'Fingerless Silk Gloves'),
    ('gloves', 'Spiked Gloves'),
    ('gloves', 'Golden Bracers'),
    ('gloves', 'Rawhide Gloves'),
    ('gloves', 'Goathide Gloves'),
    ('gloves', 'Deerskin Gloves'),
    ('gloves', 'Nubuck Gloves'),
    ('gloves', 'Eelskin Gloves'),
    ('gloves', 'Sharkskin Gloves'),
    ('gloves', 'Shagreen Gloves'),
    ('gloves', 'Stealth Gloves'),
    ('gloves', 'Slink Gloves'),
    ('gloves', 'Wrapped Mitts'),
    ('gloves', 'Strapped Mitts'),
    ('gloves', 'Clasped Mitts'),
    ('gloves', 'Trapper Mitts'),
    ('gloves', 'Ambush Mitts'),
    ('gloves', 'Carnal Mitts'),
    ('gloves', 'Assassin''s Mitts'),
    ('gloves', 'Murder Mitts'),
    ('gloves', 'Wool Gloves'),
    ('gloves', 'Velvet Gloves'),
    ('gloves', 'Silk Gloves'),
    ('gloves', 'Embroidered Gloves'),
    ('gloves', 'Satin Gloves'),
    ('gloves', 'Samite Gloves'),
    ('gloves', 'Conjurer Gloves'),
    ('gloves', 'Arcanist Gloves'),
    ('gloves', 'Sorcerer Gloves'),
    ('gloves', 'Iron Gauntlets'),
    ('gloves', 'Plated Gauntlets'),
    ('gloves', 'Bronze Gauntlets'),
    ('gloves', 'Steel Gauntlets'),
    ('gloves', 'Antique Gauntlets'),
    ('gloves', 'Ancient Gauntlets'),
    ('gloves', 'Goliath Gauntlets'),
    ('gloves', 'Vaal Gauntlets'),
    ('gloves', 'Titan Gauntlets'),
    ('gloves', 'Fishscale Gauntlets'),
    ('gloves', 'Ironscale Gauntlets'),
    ('gloves', 'Bronzescale Gauntlets'),
    ('gloves', 'Steelscale Gauntlets'),
    ('gloves', 'Serpentscale Gauntlets'),
    ('gloves', 'Wyrmscale Gauntlets'),
    ('gloves', 'Hydrascale Gauntlets'),
    ('gloves', 'Dragonscale Gauntlets'),
    ('gloves', 'Chain Gloves'),
    ('gloves', 'Ringmail Gloves'),
    ('gloves', 'Mesh Gloves'),
    ('gloves', 'Riveted Gloves'),
    ('gloves', 'Zealot Gloves'),
    ('gloves', 'Soldier Gloves'),
    ('gloves', 'Legion Gloves'),
    ('gloves', 'Crusader Gloves'),
    ('helmet', 'Bone Helmet'),
    ('helmet', 'Leather Cap'),
    ('helmet', 'Lion Pelt'),
    ('helmet', 'Tricorne'),
    ('helmet', 'Leather Hood'),
    ('helmet', 'Wolf Pelt'),
    ('helmet', 'Hunter Hood'),
    ('helmet', 'Noble Tricorne'),
    ('helmet', 'Ursine Pelt'),
    ('helmet', 'Silken Hood'),
    ('helmet', 'Sinner Tricorne'),
    ('helmet', 'Scare Mask'),
    ('helmet', 'Vaal Mask'),
    ('helmet', 'Deicide Mask'),
    ('helmet', 'Plague Mask'),
    ('helmet', 'Iron Mask'),
    ('helmet', 'Festival Mask'),
    ('helmet', 'Golden Mask'),
    ('helmet', 'Raven Mask'),
    ('helmet', 'Callous Mask'),
    ('helmet', 'Regicide Mask'),
    ('helmet', 'Harlequin Mask'),
    ('helmet', 'Vine Circlet'),
    ('helmet', 'Mind Cage'),
    ('helmet', 'Hubris Circlet'),
    ('helmet', 'Iron Circlet'),
    ('helmet', 'Torture Cage'),
    ('helmet', 'Tribal Circlet'),
    ('helmet', 'Bone Circlet'),
    ('helmet', 'Lunaris Circlet'),
    ('helmet', 'Steel Circlet'),
    ('helmet', 'Necromancer Circlet'),
    ('helmet', 'Solaris Circlet'),
    ('helmet', 'Iron Hat'),
    ('helmet', 'Royal Burgonet'),
    ('helmet', 'Eternal Burgonet'),
    ('helmet', 'Cone Helmet'),
    ('helmet', 'Barbute Helmet'),
    ('helmet', 'Close Helmet'),
    ('helmet', 'Gladiator Helmet'),
    ('helmet', 'Reaver Helmet'),
    ('helmet', 'Siege Helmet'),
    ('helmet', 'Samite Helmet'),
    ('helmet', 'Ezomyte Burgonet'),
    ('helmet', 'Battered Helm'),
    ('helmet', 'Nightmare Bascinet'),
    ('helmet', 'Sallet'),
    ('helmet', 'Visored Sallet'),
    ('helmet', 'Gilded Sallet'),
    ('helmet', 'Secutor Helm'),
    ('helmet', 'Fencer Helm'),
    ('helmet', 'Lacquered Helmet'),
    ('helmet', 'Fluted Bascinet'),
    ('helmet', 'Pig-Faced Bascinet'),
    ('helmet', 'Rusted Coif'),
    ('helmet', 'Praetor Crown'),
    ('helmet', 'Soldier Helmet'),
    ('helmet', 'Great Helmet'),
    ('helmet', 'Crusader Helmet'),
    ('helmet', 'Aventail Helmet'),
    ('helmet', 'Zealot Helmet'),
    ('helmet', 'Great Crown'),
    ('helmet', 'Magistrate Crown'),
    ('helmet', 'Prophet Crown'),
    ('helmet', 'Golden Wreath'),
    ('shield', 'Golden Flame'),
    ('shield', 'Goathide Buckler'),
    ('shield', 'Battle Buckler'),
    ('shield', 'Golden Buckler'),
    ('shield', 'Ironwood Buckler'),
    ('shield', 'Lacquered Buckler'),
    ('shield', 'Vaal Buckler'),
    ('shield', 'Crusader Buckler'),
    ('shield', 'Imperial Buckler'),
    ('shield', 'Pine Buckler'),
    ('shield', 'Painted Buckler'),
    ('shield', 'Hammered Buckler'),
    ('shield', 'War Buckler'),
    ('shield', 'Gilded Buckler'),
    ('shield', 'Oak Buckler'),
    ('shield', 'Enameled Buckler'),
    ('shield', 'Corrugated Buckler'),
    ('shield', 'Spiked Bundle'),
    ('shield', 'Alder Spiked Shield'),
    ('shield', 'Ezomyte Spiked Shield'),
    ('shield', 'Mirrored Spiked Shield'),
    ('shield', 'Supreme Spiked Shield'),
    ('shield', 'Driftwood Spiked Shield'),
    ('shield', 'Alloyed Spiked Shield'),
    ('shield', 'Burnished Spiked Shield'),
    ('shield', 'Ornate Spiked Shield'),
    ('shield', 'Redwood Spiked Shield'),
    ('shield', 'Compound Spiked Shield'),
    ('shield', 'Polished Spiked Shield'),
    ('shield', 'Sovereign Spiked Shield'),
    ('shield', 'Twig Spirit Shield'),
    ('shield', 'Chiming Spirit Shield'),
    ('shield', 'Thorium Spirit Shield'),
    ('shield', 'Lacewood Spirit Shield'),
    ('shield', 'Fossilised Spirit Shield'),
    ('shield', 'Vaal Spirit Shield'),
    ('shield', 'Harmonic Spirit Shield'),
    ('shield', 'Titanium Spirit Shield'),
    ('shield', 'Yew Spirit Shield'),
    ('shield', 'Bone Spirit Shield'),
    ('shield', 'Tarnished Spirit Shield'),
    ('shield', 'Jingling Spirit Shield'),
    ('shield', 'Brass Spirit Shield'),
    ('shield', 'Walnut Spirit Shield'),
    ('shield', 'Ivory Spirit Shield'),
    ('shield', 'Ancient Spirit Shield'),
    ('shield', 'Splintered Tower Shield'),
    ('shield', 'Bronze Tower Shield'),
    ('shield', 'Girded Tower Shield'),
    ('shield', 'Crested Tower Shield'),
    ('shield', 'Shagreen Tower Shield'),
    ('shield', 'Ebony Tower Shield'),
    ('shield', 'Ezomyte Tower Shield'),
    ('shield', 'Colossal Tower Shield'),
    ('shield', 'Pinnacle Tower Shield'),
    ('shield', 'Corroded Tower Shield'),
    ('shield', 'Rawhide Tower Shield'),
    ('shield', 'Cedar Tower Shield'),
    ('shield', 'Copper Tower Shield'),
    ('shield', 'Reinforced Tower Shield'),
    ('shield', 'Painted Tower Shield'),
    ('shield', 'Buckskin Tower Shield'),
    ('shield', 'Mahogany Tower Shield'),
    ('shield', 'Rotted Round Shield'),
    ('shield', 'Teak Round Shield'),
    ('shield', 'Spiny Round Shield'),
    ('shield', 'Cardinal Round Shield'),
    ('shield', 'Elegant Round Shield'),
    ('shield', 'Fir Round Shield'),
    ('shield', 'Studded Round Shield'),
    ('shield', 'Scarlet Round Shield'),
    ('shield', 'Splendid Round Shield'),
    ('shield', 'Maple Round Shield'),
    ('shield', 'Spiked Round Shield'),
    ('shield', 'Crimson Round Shield'),
    ('shield', 'Baroque Round Shield'),
    ('shield', 'Plank Kite Shield'),
    ('shield', 'Branded Kite Shield'),
    ('shield', 'Champion Kite Shield'),
    ('shield', 'Mosaic Kite Shield'),
    ('shield', 'Archon Kite Shield'),
    ('shield', 'Linden Kite Shield'),
    ('shield', 'Reinforced Kite Shield'),
    ('shield', 'Layered Kite Shield'),
    ('shield', 'Ceremonial Kite Shield'),
    ('shield', 'Etched Kite Shield'),
    ('shield', 'Steel Kite Shield'),
    ('shield', 'Laminated Kite Shield'),
    ('shield', 'Angelic Kite Shield'),
    ('belt', 'Rustic Sash'),
    ('belt', 'Chain Belt'),
    ('belt', 'Leather Belt'),
    ('belt', 'Heavy Belt'),
    ('belt', 'Cloth Belt'),
    ('belt', 'Studded Belt'),
    ('belt', 'Stygian Vise'),
    ('belt', 'Vanguard Belt'),
    ('belt', 'Crystal Belt'),
    ('belt', 'Golden Obi'),
    ('jewel', 'Hypnotic Eye Jewel'),
    ('jewel', 'Murderous Eye Jewel'),
    ('jewel', 'Searching Eye Jewel'),
    ('jewel', 'Ghastly Eye Jewel'),
    ('jewel', 'Viridian Jewel'),
    ('jewel', 'Cobalt Jewel'),
    ('jewel', 'Prismatic Jewel'),
    ('jewel', 'Crimson Jewel'),
    ('quiver', 'Cured Quiver'),
    ('quiver', 'Fire Arrow Quiver'),
    ('quiver', 'Broadhead Arrow Quiver'),
    ('quiver', 'Penetrating Arrow Quiver'),
    ('quiver', 'Spike-Point Arrow Quiver'),
    ('quiver', 'Rugged Quiver'),
    ('quiver', 'Conductive Quiver'),
    ('quiver', 'Heavy Quiver'),
    ('quiver', 'Light Quiver'),
    ('quiver', 'Two-Point Arrow Quiver'),
    ('quiver', 'Sharktooth Arrow Quiver'),
    ('quiver', 'Blunt Arrow Quiver'),
    ('quiver', 'Serrated Arrow Quiver'),
    ('ring', 'Breach Ring'),
    ('ring', 'Iron Ring'),
    ('ring', 'Amethyst Ring'),
    ('ring', 'Diamond Ring'),
    ('ring', 'Two-Stone Ring'),
    ('ring', 'Unset Ring'),
    ('ring', 'Coral Ring'),
    ('ring', 'Paua Ring'),
    ('ring', 'Gold Ring'),
    ('ring', 'Topaz Ring'),
    ('ring', 'Sapphire Ring'),
    ('ring', 'Ruby Ring'),
    ('ring', 'Prismatic Ring'),
    ('ring', 'Moonstone Ring'),
    ('ring', 'Steel Ring'),
    ('ring', 'Opal Ring'),
    ('ring', 'Golden Hoop'),
    ('ring', 'Jet Ring'),
    ('claw', 'Nailed Fist'),
    ('claw', 'Gouger'),
    ('claw', 'Tiger''s Paw'),
    ('claw', 'Gut Ripper'),
    ('claw', 'Prehistoric Claw'),
    ('claw', 'Noble Claw'),
    ('claw', 'Eagle Claw'),
    ('claw', 'Great White Claw'),
    ('claw', 'Throat Stabber'),
    ('claw', 'Hellion''s Paw'),
    ('claw', 'Eye Gouger'),
    ('claw', 'Sharktooth Claw'),
    ('claw', 'Vaal Claw'),
    ('claw', 'Imperial Claw'),
    ('claw', 'Terror Claw'),
    ('claw', 'Awl'),
    ('claw', 'Cat''s Paw'),
    ('claw', 'Blinder'),
    ('claw', 'Timeworn Claw'),
    ('claw', 'Sparkling Claw'),
    ('claw', 'Fright Claw'),
    ('claw', 'Thresher Claw'),
    ('claw', 'Double Claw'),
    ('claw', 'Twin Claw'),
    ('claw', 'Gemini Claw'),
    ('dagger', 'Glass Shank'),
    ('dagger', 'Butcher Knife'),
    ('dagger', 'Poignard'),
    ('dagger', 'Boot Blade'),
    ('dagger', 'Golden Kris'),
    ('dagger', 'Royal Skean'),
    ('dagger', 'Fiend Dagger'),
    ('dagger', 'Gutting Knife'),
    ('dagger', 'Slaughter Knife'),
    ('dagger', 'Ambusher'),
    ('dagger', 'Ezomyte Dagger'),
    ('dagger', 'Skinning Knife'),
    ('dagger', 'Platinum Kris'),
    ('dagger', 'Imperial Skean'),
    ('dagger', 'Demon Dagger'),
    ('dagger', 'Carving Knife'),
    ('dagger', 'Stiletto'),
    ('dagger', 'Boot Knife'),
    ('dagger', 'Copper Kris'),
    ('dagger', 'Skean'),
    ('dagger', 'Imp Dagger'),
    ('dagger', 'Flaying Knife'),
    ('dagger', 'Prong Dagger'),
    ('dagger', 'Trisula'),
    ('dagger', 'Sai'),
    ('oneaxe', 'Rusted Hatchet'),
    ('oneaxe', 'Tomahawk'),
    ('oneaxe', 'Wrist Chopper'),
    ('oneaxe', 'War Axe'),
    ('oneaxe', 'Chest Splitter'),
    ('oneaxe', 'Ceremonial Axe'),
    ('oneaxe', 'Wraith Axe'),
    ('oneaxe', 'Karui Axe'),
    ('oneaxe', 'Siege Axe'),
    ('oneaxe', 'Reaver Axe'),
    ('oneaxe', 'Butcher Axe'),
    ('oneaxe', 'Jade Hatchet'),
    ('oneaxe', 'Vaal Hatchet'),
    ('oneaxe', 'Royal Axe'),
    ('oneaxe', 'Infernal Axe'),
    ('oneaxe', 'Boarding Axe'),
    ('oneaxe', 'Cleaver'),
    ('oneaxe', 'Broad Axe'),
    ('oneaxe', 'Arming Axe'),
    ('oneaxe', 'Decorative Axe'),
    ('oneaxe', 'Spectral Axe'),
    ('oneaxe', 'Jasper Axe'),
    ('oneaxe', 'Etched Hatchet'),
    ('oneaxe', 'Engraved Hatchet'),
    ('oneaxe', 'Runic Hatchet'),
    ('onemace', 'Driftwood Club'),
    ('onemace', 'Barbed Club'),
    ('onemace', 'Rock Breaker'),
    ('onemace', 'Battle Hammer'),
    ('onemace', 'Flanged Mace'),
    ('onemace', 'Ornate Mace'),
    ('onemace', 'Phantom Mace'),
    ('onemace', 'Ancestral Club'),
    ('onemace', 'Tenderizer'),
    ('onemace', 'Gavel'),
    ('onemace', 'Legion Hammer'),
    ('onemace', 'Tribal Club'),
    ('onemace', 'Pernarch'),
    ('onemace', 'Auric Mace'),
    ('onemace', 'Nightmare Mace'),
    ('onemace', 'Spiked Club'),
    ('onemace', 'Stone Hammer'),
    ('onemace', 'War Hammer'),
    ('onemace', 'Bladed Mace'),
    ('onemace', 'Ceremonial Mace'),
    ('onemace', 'Dream Mace'),
    ('onemace', 'Petrified Club'),
    ('onemace', 'Wyrm Mace'),
    ('onemace', 'Dragon Mace'),
    ('onemace', 'Behemoth Mace'),
    ('sceptre', 'Driftwood Sceptre'),
    ('sceptre', 'Sekhem'),
    ('sceptre', 'Crystal Sceptre'),
    ('sceptre', 'Lead Sceptre'),
    ('sceptre', 'Blood Sceptre'),
    ('sceptre', 'Royal Sceptre'),
    ('sceptre', 'Abyssal Sceptre'),
    ('sceptre', 'Karui Sceptre'),
    ('sceptre', 'Tyrant''s Sekhem'),
    ('sceptre', 'Opal Sceptre'),
    ('sceptre', 'Platinum Sceptre'),
    ('sceptre', 'Darkwood Sceptre'),
    ('sceptre', 'Vaal Sceptre'),
    ('sceptre', 'Carnal Sceptre'),
    ('sceptre', 'Void Sceptre'),
    ('sceptre', 'Bronze Sceptre'),
    ('sceptre', 'Quartz Sceptre'),
    ('sceptre', 'Iron Sceptre'),
    ('sceptre', 'Ochre Sceptre'),
    ('sceptre', 'Ritual Sceptre'),
    ('sceptre', 'Shadow Sceptre'),
    ('sceptre', 'Grinning Fetish'),
    ('sceptre', 'Horned Sceptre'),
    ('sceptre', 'Stag Sceptre'),
    ('sceptre', 'Sambar Sceptre'),
    ('onesword', 'Rusted Sword'),
    ('onesword', 'Cutlass'),
    ('onesword', 'Baselard'),
    ('onesword', 'Battle Sword'),
    ('onesword', 'Elder Sword'),
    ('onesword', 'Graceful Sword'),
    ('onesword', 'Twilight Blade'),
    ('onesword', 'Gemstone Sword'),
    ('onesword', 'Corsair Sword'),
    ('onesword', 'Gladius'),
    ('onesword', 'Legion Sword'),
    ('onesword', 'Copper Sword'),
    ('onesword', 'Vaal Blade'),
    ('onesword', 'Eternal Sword'),
    ('onesword', 'Midnight Blade'),
    ('onesword', 'Sabre'),
    ('onesword', 'Broad Sword'),
    ('onesword', 'War Sword'),
    ('onesword', 'Ancient Sword'),
    ('onesword', 'Elegant Sword'),
    ('onesword', 'Dusk Blade'),
    ('onesword', 'Variscite Blade'),
    ('onesword', 'Hook Sword'),
    ('onesword', 'Grappler'),
    ('onesword', 'Tiger Hook'),
    ('onesword', 'Rusted Spike'),
    ('onesword', 'Burnished Foil'),
    ('onesword', 'Estoc'),
    ('onesword', 'Serrated Foil'),
    ('onesword', 'Primeval Rapier'),
    ('onesword', 'Fancy Foil'),
    ('onesword', 'Apex Rapier'),
    ('onesword', 'Dragonbone Rapier'),
    ('onesword', 'Tempered Foil'),
    ('onesword', 'Pecoraro'),
    ('onesword', 'Spiraled Foil'),
    ('onesword', 'Whalebone Rapier'),
    ('onesword', 'Vaal Rapier'),
    ('onesword', 'Jewelled Foil'),
    ('onesword', 'Harpy Rapier'),
    ('onesword', 'Battered Foil'),
    ('onesword', 'Basket Rapier'),
    ('onesword', 'Jagged Foil'),
    ('onesword', 'Antique Rapier'),
    ('onesword', 'Elegant Foil'),
    ('onesword', 'Thorn Rapier'),
    ('onesword', 'Wyrmbone Rapier'),
    ('onesword', 'Smallsword'),
    ('onesword', 'Courtesan Sword'),
    ('onesword', 'Dragoon Sword'),
    ('onesword', 'Charan''s Sword'),
    ('wand', 'Driftwood Wand'),
    ('wand', 'Serpent Wand'),
    ('wand', 'Omen Wand'),
    ('wand', 'Demon''s Horn'),
    ('wand', 'Imbued Wand'),
    ('wand', 'Opal Wand'),
    ('wand', 'Tornado Wand'),
    ('wand', 'Prophecy Wand'),
    ('wand', 'Goat''s Horn'),
    ('wand', 'Carved Wand'),
    ('wand', 'Quartz Wand'),
    ('wand', 'Spiraled Wand'),
    ('wand', 'Sage Wand'),
    ('wand', 'Faun''s Horn'),
    ('wand', 'Engraved Wand'),
    ('wand', 'Crystal Wand'),
    ('wand', 'Pagan Wand'),
    ('wand', 'Heathen Wand'),
    ('wand', 'Profane Wand'),
    ('rod', 'Fishing Rod'),
    ('bow', 'Crude Bow'),
    ('bow', 'Decurve Bow'),
    ('bow', 'Compound Bow'),
    ('bow', 'Sniper Bow'),
    ('bow', 'Ivory Bow'),
    ('bow', 'Highborn Bow'),
    ('bow', 'Decimation Bow'),
    ('bow', 'Thicket Bow'),
    ('bow', 'Citadel Bow'),
    ('bow', 'Ranger Bow'),
    ('bow', 'Assassin Bow'),
    ('bow', 'Short Bow'),
    ('bow', 'Spine Bow'),
    ('bow', 'Imperial Bow'),
    ('bow', 'Harbinger Bow'),
    ('bow', 'Long Bow'),
    ('bow', 'Composite Bow'),
    ('bow', 'Recurve Bow'),
    ('bow', 'Bone Bow'),
    ('bow', 'Royal Bow'),
    ('bow', 'Death Bow'),
    ('bow', 'Grove Bow'),
    ('bow', 'Reflex Bow'),
    ('bow', 'Steelwood Bow'),
    ('bow', 'Maraketh Bow'),
    ('staff', 'Gnarled Branch'),
    ('staff', 'Military Staff'),
    ('staff', 'Serpentine Staff'),
    ('staff', 'Highborn Staff'),
    ('staff', 'Foul Staff'),
    ('staff', 'Primordial Staff'),
    ('staff', 'Lathi'),
    ('staff', 'Ezomyte Staff'),
    ('staff', 'Maelström Staff'),
    ('staff', 'Imperial Staff'),
    ('staff', 'Judgement Staff'),
    ('staff', 'Primitive Staff'),
    ('staff', 'Long Staff'),
    ('staff', 'Iron Staff'),
    ('staff', 'Coiled Staff'),
    ('staff', 'Royal Staff'),
    ('staff', 'Vile Staff'),
    ('staff', 'Woodful Staff'),
    ('staff', 'Quarterstaff'),
    ('staff', 'Crescent Staff'),
    ('staff', 'Moon Staff'),
    ('staff', 'Eclipse Staff'),
    ('twoaxe', 'Stone Axe'),
    ('twoaxe', 'Headsman Axe'),
    ('twoaxe', 'Labrys'),
    ('twoaxe', 'Noble Axe'),
    ('twoaxe', 'Abyssal Axe'),
    ('twoaxe', 'Karui Chopper'),
    ('twoaxe', 'Sundering Axe'),
    ('twoaxe', 'Ezomyte Axe'),
    ('twoaxe', 'Vaal Axe'),
    ('twoaxe', 'Despot Axe'),
    ('twoaxe', 'Void Axe'),
    ('twoaxe', 'Jade Chopper'),
    ('twoaxe', 'Woodsplitter'),
    ('twoaxe', 'Poleaxe'),
    ('twoaxe', 'Double Axe'),
    ('twoaxe', 'Gilded Axe'),
    ('twoaxe', 'Shadow Axe'),
    ('twoaxe', 'Jasper Chopper'),
    ('twoaxe', 'Timber Axe'),
    ('twoaxe', 'Dagger Axe'),
    ('twoaxe', 'Talon Axe'),
    ('twoaxe', 'Fleshripper'),
    ('twomace', 'Driftwood Maul'),
    ('twomace', 'Steelhead'),
    ('twomace', 'Spiny Maul'),
    ('twomace', 'Plated Maul'),
    ('twomace', 'Dread Maul'),
    ('twomace', 'Karui Maul'),
    ('twomace', 'Colossus Mallet'),
    ('twomace', 'Piledriver'),
    ('twomace', 'Meatgrinder'),
    ('twomace', 'Imperial Maul'),
    ('twomace', 'Terror Maul'),
    ('twomace', 'Tribal Maul'),
    ('twomace', 'Mallet'),
    ('twomace', 'Sledgehammer'),
    ('twomace', 'Jagged Maul'),
    ('twomace', 'Brass Maul'),
    ('twomace', 'Fright Maul'),
    ('twomace', 'Totemic Maul'),
    ('twomace', 'Great Mallet'),
    ('twomace', 'Morning Star'),
    ('twomace', 'Solar Maul'),
    ('twomace', 'Coronal Maul'),
    ('twosword', 'Corroded Blade'),
    ('twosword', 'Highland Blade'),
    ('twosword', 'Engraved Greatsword'),
    ('twosword', 'Tiger Sword'),
    ('twosword', 'Wraith Sword'),
    ('twosword', 'Headman''s Sword'),
    ('twosword', 'Reaver Sword'),
    ('twosword', 'Ezomyte Blade'),
    ('twosword', 'Vaal Greatsword'),
    ('twosword', 'Lion Sword'),
    ('twosword', 'Infernal Sword'),
    ('twosword', 'Longsword'),
    ('twosword', 'Bastard Sword'),
    ('twosword', 'Two-Handed Sword'),
    ('twosword', 'Etched Greatsword'),
    ('twosword', 'Ornate Sword'),
    ('twosword', 'Spectral Sword'),
    ('twosword', 'Butcher Sword'),
    ('twosword', 'Footman Sword'),
    ('twosword', 'Keyblade'),
    ('twosword', 'Curved Blade'),
    ('twosword', 'Lithe Blade'),
    ('twosword', 'Exquisite Blade');

-- --------------------------------------------------------------------------------------------------------------------
-- Event setup
-- --------------------------------------------------------------------------------------------------------------------

--
-- Event configuration remove24
--

DROP EVENT IF EXISTS remove24;

CREATE EVENT remove24
  ON SCHEDULE EVERY 1 HOUR
  STARTS '2018-01-01 08:00:03'
  COMMENT 'Clears out entries older than 1 day'
  DO
    DELETE FROM league_history_hourly_quantity
    WHERE       time < ADDDATE(NOW(), INTERVAL -25 HOUR);

--
-- Event configuration remove120
--

DROP EVENT IF EXISTS remove120;

CREATE EVENT remove120
  ON SCHEDULE EVERY 1 DAY
  STARTS '2018-01-01 08:00:06'
  COMMENT 'Clears out entries older than 120 days'
  DO
    DELETE FROM league_history_daily_rolling
    WHERE  id_l <= 2
      AND  time < ADDDATE(NOW(), INTERVAL -120 DAY);

-- --------------------------------------------------------------------------------------------------------------------
-- User accounts
-- --------------------------------------------------------------------------------------------------------------------

DROP USER IF EXISTS 'pw_app'@'localhost';
CREATE USER 'pw_app'@'localhost' IDENTIFIED BY 'password goes here';
GRANT ALL PRIVILEGES ON pw.* TO 'pw_app'@'localhost';

DROP USER IF EXISTS 'pw_web'@'localhost';
CREATE USER 'pw_web'@'localhost' IDENTIFIED BY 'password goes here';
GRANT SELECT ON pw.* TO 'pw_web'@'localhost';

FLUSH PRIVILEGES;
