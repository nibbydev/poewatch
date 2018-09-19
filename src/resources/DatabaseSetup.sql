-- --------------------------------------------------------------------------------------------------------------------
-- Initial configuration
-- --------------------------------------------------------------------------------------------------------------------

SET time_zone = "+02:00";

--
-- Database: ps5
--
DROP DATABASE IF EXISTS ps5;
CREATE DATABASE IF NOT EXISTS ps5 DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;

USE ps5;

-- --------------------------------------------------------------------------------------------------------------------
-- Category tables
-- --------------------------------------------------------------------------------------------------------------------

--
-- Table structure category_parent
--

CREATE TABLE category_parent (
    id       INT          UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    name     VARCHAR(32)  NOT NULL UNIQUE,
    display  VARCHAR(32)  DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure category_child
--

CREATE TABLE category_child (
    id       INT          UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    id_cp    INT          UNSIGNED NOT NULL,
    name     VARCHAR(32)  NOT NULL,
    display  VARCHAR(32)  DEFAULT NULL,

    FOREIGN KEY (id_cp) REFERENCES category_parent (id) ON DELETE CASCADE,

    INDEX name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------------------------------------------------------
-- Data tables
-- --------------------------------------------------------------------------------------------------------------------

--
-- Table structure data_leagues
--

CREATE TABLE data_leagues (
    id       SMALLINT     UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    active   TINYINT(1)   UNSIGNED NOT NULL DEFAULT 1,
    upcoming TINYINT(1)   UNSIGNED NOT NULL DEFAULT 0,
    event    TINYINT(1)   UNSIGNED NOT NULL DEFAULT 0,
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
    changeId  VARCHAR(256)  NOT NULL UNIQUE,
    time      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
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

-- --------------------------------------------------------------------------------------------------------------------
-- Item data
-- --------------------------------------------------------------------------------------------------------------------

--
-- Table structure data_itemData
--

CREATE TABLE data_itemData (
    id         INT           UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    id_cp      INT           UNSIGNED NOT NULL,
    id_cc      INT           UNSIGNED DEFAULT NULL,
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

    FOREIGN KEY (id_cp) REFERENCES category_parent (id) ON DELETE CASCADE,
    FOREIGN KEY (id_cc) REFERENCES category_child  (id) ON DELETE CASCADE,

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
    exalted     DECIMAL(14,8)  UNSIGNED NOT NULL DEFAULT 0.0,
    count       INT(16)        UNSIGNED NOT NULL DEFAULT 0,
    quantity    INT(8)         UNSIGNED NOT NULL DEFAULT 0,
    inc         INT(8)         UNSIGNED NOT NULL DEFAULT 0,
    `dec`       INT(8)         UNSIGNED NOT NULL DEFAULT 0,
    spark       VARCHAR(128)   DEFAULT NULL,

    FOREIGN KEY (id_l) REFERENCES data_leagues  (id) ON DELETE RESTRICT,
    FOREIGN KEY (id_d) REFERENCES data_itemData (id) ON DELETE CASCADE,
    CONSTRAINT pk PRIMARY KEY (id_l, id_d),

    INDEX volatile   (volatile),
    INDEX multiplier (multiplier),
    INDEX mean       (mean),
    INDEX median     (median)
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
    id_l       SMALLINT       UNSIGNED NOT NULL,
    id_d       INT            UNSIGNED NOT NULL,
    time       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved   TINYINT(1)     UNSIGNED NOT NULL DEFAULT 0,
    price      DECIMAL(14,8)  UNSIGNED NOT NULL,
    account    VARCHAR(32)    NOT NULL,

    FOREIGN KEY (id_l) REFERENCES  data_leagues         (id)   ON DELETE RESTRICT,
    FOREIGN KEY (id_d) REFERENCES  league_items_rolling (id_d) ON DELETE CASCADE,
    CONSTRAINT pk PRIMARY KEY (id_l, id_d, account),

    INDEX time     (time),
    INDEX approved (approved),
    INDEX compound_id (id_l, id_d)
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
    volatile  TINYINT(1)     UNSIGNED DEFAULT NULL,
    mean      DECIMAL(14,8)  UNSIGNED DEFAULT NULL,
    median    DECIMAL(14,8)  UNSIGNED DEFAULT NULL,
    mode      DECIMAL(14,8)  UNSIGNED DEFAULT NULL,
    exalted   DECIMAL(14,8)  UNSIGNED DEFAULT NULL,
    inc       INT(8)         UNSIGNED DEFAULT NULL,
    `dec`     INT(8)         UNSIGNED DEFAULT NULL,
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
    volatile   TINYINT(1)     UNSIGNED DEFAULT NULL,
    mean       DECIMAL(14,8)  UNSIGNED DEFAULT NULL,
    median     DECIMAL(14,8)  UNSIGNED DEFAULT NULL,
    mode       DECIMAL(14,8)  UNSIGNED DEFAULT NULL,
    exalted    DECIMAL(14,8)  UNSIGNED DEFAULT NULL,
    inc        INT(8)         UNSIGNED DEFAULT NULL,
    `dec`      INT(8)         UNSIGNED DEFAULT NULL,
    count      INT(16)        UNSIGNED DEFAULT NULL,
    quantity   INT(8)         UNSIGNED DEFAULT NULL,

    FOREIGN KEY (id_l) REFERENCES data_leagues  (id) ON DELETE RESTRICT,
    FOREIGN KEY (id_d) REFERENCES data_itemData (id) ON DELETE CASCADE,

    INDEX time (time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure league_history_hourly_rolling
--

CREATE TABLE league_history_hourly_rolling (
    id_l      SMALLINT       UNSIGNED NOT NULL,
    id_d      INT            UNSIGNED NOT NULL,
    time      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    volatile  TINYINT(1)     UNSIGNED DEFAULT NULL,
    mean      DECIMAL(14,8)  UNSIGNED DEFAULT NULL,
    median    DECIMAL(14,8)  UNSIGNED DEFAULT NULL,
    mode      DECIMAL(14,8)  UNSIGNED DEFAULT NULL,
    exalted   DECIMAL(14,8)  UNSIGNED DEFAULT NULL,
    inc       INT(8)         UNSIGNED DEFAULT NULL,
    `dec`     INT(8)         UNSIGNED DEFAULT NULL,
    count     INT(16)        UNSIGNED DEFAULT NULL,
    quantity  INT(8)         UNSIGNED DEFAULT NULL,

    FOREIGN KEY (id_l) REFERENCES data_leagues  (id) ON DELETE RESTRICT,
    FOREIGN KEY (id_d) REFERENCES data_itemData (id) ON DELETE CASCADE,

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

--
-- Table structure account_data
--

CREATE TABLE account_data (
    id        BIGINT          UNSIGNED PRIMARY KEY,
    private   TINYINT(1)      UNSIGNED NOT NULL DEFAULT 0,

    FOREIGN KEY (id) REFERENCES account_accounts (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------------------------------------------------------
-- Base values
-- --------------------------------------------------------------------------------------------------------------------

--
-- Base values for data_leagues
--

INSERT INTO data_leagues
    (id, active, name, display)
VALUES
    (1,  1, 'Hardcore',            'Hardcore'     ),
    (2,  1, 'Standard',            'Standard'     ),
    (3,  0, 'Hardcore Breach',     'HC Breach'    ),
    (4,  0, 'Breach',              'Breach'       ),
    (5,  0, 'Hardcore Legacy',     'HC Legacy'    ),
    (6,  0, 'Legacy',              'Legacy'       ),
    (7,  0, 'Hardcore Harbinger',  'HC Harbinger' ),
    (8,  0, 'Harbinger',           'Harbinger'    ),
    (9,  0, 'Hardcore Abyss',      'HC Abyss'     ),
    (10, 0, 'Abyss',               'Abyss'        ),
    (11, 0, 'Hardcore Bestiary',   'HC Bestiary'  ),
    (12, 0, 'Bestiary',            'Bestiary'     ),
    (13, 0, 'Hardcore Incursion',  'HC Incursion' ),
    (14, 0, 'Incursion',           'Incursion'    );

--
-- Base value for data_changeId
--

INSERT INTO data_changeId
    (changeId)
VALUES
    ('0-0-0-0-0');

--
-- Base values for category_parent
--

INSERT INTO category_parent
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
-- Base values for category_child
--

INSERT INTO category_child
    (id, id_cp, name, display)
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
    DELETE FROM league_history_hourly_rolling
    WHERE       time < ADDDATE(NOW(), INTERVAL -24 HOUR);

--
-- Event configuration remove120
--

DROP EVENT IF EXISTS remove120;

CREATE EVENT remove120
  ON SCHEDULE EVERY 1 DAY
  STARTS '2018-01-01 08:00:06'
  COMMENT 'Clears out entries older than 120 days'
  DO
    DELETE h
    FROM   league_history_daily_rolling AS h
    JOIN   data_leagues AS l
      ON   h.id_l = l.id
    WHERE  l.id > 2
      AND  time < ADDDATE(NOW(), INTERVAL -120 DAY);

