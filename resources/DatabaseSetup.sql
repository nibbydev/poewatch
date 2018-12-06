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
    time      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
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
-- Table structure for table league_items
--

CREATE TABLE league_items (
    id_l     SMALLINT       UNSIGNED NOT NULL,
    id_d     INT            UNSIGNED NOT NULL,
    time     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mean     DECIMAL(14,8)  UNSIGNED NOT NULL DEFAULT 0.0,
    median   DECIMAL(14,8)  UNSIGNED NOT NULL DEFAULT 0.0,
    mode     DECIMAL(14,8)  UNSIGNED NOT NULL DEFAULT 0.0,
    min      DECIMAL(14,8)  UNSIGNED NOT NULL DEFAULT 0.0,
    max      DECIMAL(14,8)  UNSIGNED NOT NULL DEFAULT 0.0,
    exalted  DECIMAL(14,8)  UNSIGNED NOT NULL DEFAULT 0.0,
    total    INT(16)        UNSIGNED NOT NULL DEFAULT 0,
    daily    INT(8)         UNSIGNED NOT NULL DEFAULT 0,
    inc      INT(8)         UNSIGNED NOT NULL DEFAULT 0,
    spark    VARCHAR(128)   DEFAULT NULL,

    FOREIGN KEY (id_l) REFERENCES data_leagues  (id) ON DELETE RESTRICT,
    FOREIGN KEY (id_d) REFERENCES data_itemData (id) ON DELETE CASCADE,
    CONSTRAINT pk PRIMARY KEY (id_l, id_d),

    INDEX volatile (volatile),
    INDEX total    (total),
    INDEX median   (median),
    INDEX inc      (inc)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure league_entries
--

CREATE TABLE league_entries (
    id_l       SMALLINT       UNSIGNED NOT NULL,
    id_d       INT            UNSIGNED NOT NULL,
    account    VARCHAR(32)    NOT NULL,
    time       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    outlier    BIT(1)         NOT NULL DEFAULT 0,
    price      DECIMAL(14,8)  UNSIGNED NOT NULL,
    listings   INT            UNSIGNED NOT NULL DEFAULT 1,
    id_item    VARCHAR(64)    NOT NULL DEFAULT '',

    FOREIGN KEY (id_l) REFERENCES  data_leagues (id)   ON DELETE RESTRICT,
    FOREIGN KEY (id_d) REFERENCES  league_items (id_d) ON DELETE CASCADE,

    CONSTRAINT pk PRIMARY KEY (id_l, id_d, account, id_item),
    INDEX outlier_time (outlier, `time`),
    INDEX compound_id (id_l, id_d)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------------------------------------------------------
-- League history tables
-- --------------------------------------------------------------------------------------------------------------------

CREATE TABLE league_history_daily (
    id_l     SMALLINT       UNSIGNED NOT NULL,
    id_d     INT            UNSIGNED NOT NULL,
    time     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mean     DECIMAL(14,8)  UNSIGNED DEFAULT NULL,
    median   DECIMAL(14,8)  UNSIGNED DEFAULT NULL,
    mode     DECIMAL(14,8)  UNSIGNED DEFAULT NULL,
    min      DECIMAL(14,8)  UNSIGNED NOT NULL DEFAULT 0.0,
    max      DECIMAL(14,8)  UNSIGNED NOT NULL DEFAULT 0.0,
    exalted  DECIMAL(14,8)  UNSIGNED DEFAULT NULL,
    total    INT(16)        UNSIGNED DEFAULT NULL,
    daily    INT(8)         UNSIGNED DEFAULT NULL,

    FOREIGN KEY (id_l) REFERENCES data_leagues  (id) ON DELETE RESTRICT,
    FOREIGN KEY (id_d) REFERENCES data_itemData (id) ON DELETE CASCADE,

    INDEX time (time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure league_history_hourly
--

CREATE TABLE league_history_hourly (
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
    (name, display)
VALUES
    ('accessory',      'Accessories'),
    ('armour',         'Armour'),
    ('card',           'Divination cards'),
    ('currency',       'Currency'),
    ('enchantment',    'Enchants'),
    ('flask',          'Flasks'),
    ('gem',            'Gems'),
    ('jewel',          'Jewels'),
    ('map',            'Maps'),
    ('prophecy',       'Prophecy'),
    ('weapon',         'Weapons'),
    ('base',           'Crafting Bases');

--
-- Base values for data_groups
--

INSERT INTO data_groups
    (id_cat, name, display)
VALUES
    (1,     'amulet',     'Amulets'),
    (1,     'belt',       'Belts'),
    (1,     'ring',       'Rings'),
    (2,     'boots',      'Boots'),
    (2,     'chest',      'Body Armours'),
    (2,     'gloves',     'Gloves'),
    (2,     'helmet',     'Helmets'),
    (2,     'quiver',     'Quivers'),
    (2,     'shield',     'Shields'),
    (3,     'card',       'Divination cards'),
    (4,     'currency',   'Currency'),
    (4,     'essence',    'Essences'),
    (4,     'piece',      'Harbinger pieces'),
    (4,     'fossil',     'Fossils'),
    (4,     'resonator',  'Resonators'),
    (4,     'vial',       'Vials'),
    (4,     'net',        'Nets'),
    (5,     'boots',      'Boots'),
    (5,     'gloves',     'Gloves'),
    (5,     'helmet',     'Helmets'),
    (6,     'flask',      'Flasks'),
    (7,     'skill',      'Skill Gems'),
    (7,     'support',    'Support Gems'),
    (7,     'vaal',       'Vaal Gems'),
    (8,     'jewel',      'Jewels'),
    (9,     'map',        'Regular Maps'),
    (9,     'fragment',   'Fragments'),
    (9,     'unique',     'Unique Maps'),
    (10,    'prophecy',   'Prophecies'),
    (11,    'bow',        'Bows'),
    (11,    'claw',       'Claws'),
    (11,    'dagger',     'Daggers'),
    (11,    'oneaxe',     '1H Axes'),
    (11,    'onemace',    '1H Maces'),
    (11,    'onesword',   '1H Swords'),
    (11,    'rod',        'Rods'),
    (11,    'sceptre',    'Sceptres'),
    (11,    'staff',      'Staves'),
    (11,    'twoaxe',     '2H Axes'),
    (11,    'twomace',    '2H Maces'),
    (11,    'twosword',   '2H Swords'),
    (11,    'wand',       'Wands'),
    (12,    'ring',       'Rings'),
    (12,    'belt',       'Belts'),
    (12,    'amulet',     'Amulets'),
    (12,    'helmet',     'Helmets'),
    (12,    'chest',      'Body Armour'),
    (12,    'gloves',     'Gloves'),
    (12,    'boots',      'Boots');
    (12,    'onemace,'	  '1H Maces'),
    (12,    'sceptre',	  'Sceptres'),
    (12,    'bow',	      'Bows'),
    (12,    'wand',	      'Wands'),
    (12,    'onesword',	  '1H Swords'),
    (12,    'claw',	      'Claws'),
    (12,    'shield',	    'Shields'),
    (12,    'dagger',	    'Daggers'),
    (12,    'twosword',	  '2H Swords'),
    (12,    'staff',	    'Staves'),
    (12,    'oneaxe',	    '1H Axes'),
    (12,    'quiver',	    'Quivers'),
    (12,    'twoaxe',	    '2H Axes'),
    (12,    'twomace',	  '2H Maces'),
    (12,    'jewel',	    'Jewels');

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
  COMMENT 'Clears out entries older than 24h'
  DO
    DELETE FROM league_history_hourly
    WHERE       time < ADDDATE(NOW(), INTERVAL -25 HOUR);

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
