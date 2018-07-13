-- --------------------------------------------------------------------------------------------------------------------
-- Initial configuration
-- --------------------------------------------------------------------------------------------------------------------

SET time_zone = "+02:00";

--
-- Database: `ps4`
--
DROP DATABASE IF EXISTS `ps4`;
CREATE DATABASE IF NOT EXISTS `ps4` DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;

USE `ps4`;


-- --------------------------------------------------------------------------------------------------------------------
-- Category tables
-- --------------------------------------------------------------------------------------------------------------------

--
-- Table structure `category_parent`
--

CREATE TABLE `category_parent` (
    `id`                    INT             UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    `name`                  VARCHAR(32)     NOT NULL UNIQUE,
    `display`               VARCHAR(32)     DEFAULT NULL,

    INDEX `index_cp_id`         (`id`),
    INDEX `index_cp_name`       (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure `category_child`
--

CREATE TABLE `category_child` (
    `id`                    INT             UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    `id_cp`                 INT             UNSIGNED NOT NULL,
    `name`                  VARCHAR(32)     NOT NULL,
    `display`               VARCHAR(32)     DEFAULT NULL,

    FOREIGN KEY (`id_cp`) REFERENCES `category_parent` (`id`) ON DELETE CASCADE,

    INDEX `index_cc_id`         (`id`),
    INDEX `index_cc_id_cp`      (`id_cp`),
    INDEX `index_cc_name`       (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure `category_history`
--

CREATE TABLE `category_history` (
    `id`                    INT             UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    `name`                  VARCHAR(32)     NOT NULL UNIQUE,

    INDEX `index_ch_id`         (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


-- --------------------------------------------------------------------------------------------------------------------
-- Data tables
-- --------------------------------------------------------------------------------------------------------------------

--
-- Table structure `data_leagues`
--

CREATE TABLE `data_leagues` (
    `id`                  INT             UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    `active`              TINYINT(1)      UNSIGNED NOT NULL DEFAULT 1,
    `name`                VARCHAR(64)     NOT NULL UNIQUE,
    `display`             VARCHAR(64)     DEFAULT NULL,
    `start`               VARCHAR(32)     DEFAULT NULL,
    `end`                 VARCHAR(32)     DEFAULT NULL,

    INDEX `index_l_id`              (`id`),
    INDEX `index_l_active`          (`active`),
    INDEX `index_l_name`            (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure `data_changeId`
--

CREATE TABLE `data_changeId` (
    `changeId`            VARCHAR(256)    NOT NULL UNIQUE,
    `time`                TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure `data_outputFiles`
--

CREATE TABLE `data_outputFiles` (
    `league`              VARCHAR(64)     NOT NULL,
    `category`            VARCHAR(32)     NOT NULL,
    `path`                VARCHAR(128)    NOT NULL,
    `time`                TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT  `pk_dof`  PRIMARY KEY (`league`, `category`),

    INDEX `index_dof_pk`          (`league`, `category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure `data_currencyItems`
--

CREATE TABLE `data_currencyItems` (
    `id`                  INT             UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    `name`                VARCHAR(64)     NOT NULL,

    INDEX `index_ci_id`               (`id`),
    INDEX `index_ci_name`             (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure `data_currencyAliases`
--

CREATE TABLE `data_currencyAliases` (
    `id`                    INT           UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    `id_ci`                 INT           UNSIGNED NOT NULL,
    `name`                  VARCHAR(32)   NOT NULL,

    FOREIGN KEY (`id_ci`) REFERENCES `data_currencyItems` (`id`) ON DELETE CASCADE,

    INDEX `index_ca_id`               (`id`),
    INDEX `index_ca_id_ci`            (`id_ci`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------------------------------------------------------
-- Item data
-- --------------------------------------------------------------------------------------------------------------------

--
-- Table structure `data_itemData`
--

CREATE TABLE `data_itemData` (
    `id`                    INT             UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    `id_cp`                 INT             UNSIGNED NOT NULL,
    `id_cc`                 INT             UNSIGNED DEFAULT NULL,
    `name`                  VARCHAR(128)    NOT NULL,
    `type`                  VARCHAR(64)     DEFAULT NULL,
    `frame`                 TINYINT(1)      NOT NULL,

    `tier`                  TINYINT(1)      UNSIGNED DEFAULT NULL,
    `lvl`                   TINYINT(1)      UNSIGNED DEFAULT NULL,
    `quality`               TINYINT(1)      UNSIGNED DEFAULT NULL,
    `corrupted`             TINYINT(1)      UNSIGNED DEFAULT NULL,
    `links`                 TINYINT(1)      UNSIGNED DEFAULT NULL,
    `var`                   VARCHAR(32)     DEFAULT NULL,
    `key`                   VARCHAR(128)    NOT NULL,
    `icon`                  VARCHAR(256)    NOT NULL,

    FOREIGN KEY (`id_cp`) REFERENCES `category_parent` (`id`) ON DELETE CASCADE,
    FOREIGN KEY (`id_cc`) REFERENCES `category_child`  (`id`) ON DELETE CASCADE,

    INDEX `index_idp_id`        (`id`),
    INDEX `index_idp_id_cp`     (`id_cp`),
    INDEX `index_idp_id_cc`     (`id_cc`),
    INDEX `index_idp_key`       (`key`),
    INDEX `index_idp_frame`     (`frame`),
    INDEX `index_idp_name`      (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------------------------------------------------------
-- League tables
-- --------------------------------------------------------------------------------------------------------------------

--
-- Table structure for table `league_items`
--

CREATE TABLE `league_items` (
    `id_l`                INT             UNSIGNED NOT NULL,
    `id_d`                INT             UNSIGNED NOT NULL,
    `time`                TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `volatile`            TINYINT(1)      UNSIGNED NOT NULL DEFAULT 0,
    `multiplier`          DECIMAL(6,4)    UNSIGNED NOT NULL DEFAULT 2.0,
    `mean`                DECIMAL(10,4)   UNSIGNED NOT NULL DEFAULT 0.0,
    `median`              DECIMAL(10,4)   UNSIGNED NOT NULL DEFAULT 0.0,
    `mode`                DECIMAL(10,4)   UNSIGNED NOT NULL DEFAULT 0.0,
    `exalted`             DECIMAL(10,4)   UNSIGNED NOT NULL DEFAULT 0.0,
    `count`               INT(16)         UNSIGNED NOT NULL DEFAULT 0,
    `quantity`            INT(8)          UNSIGNED NOT NULL DEFAULT 0,
    `inc`                 INT(8)          UNSIGNED NOT NULL DEFAULT 0,
    `dec`                 INT(8)          UNSIGNED NOT NULL DEFAULT 0,

    FOREIGN KEY (`id_l`) REFERENCES  `data_leagues`   (`id`) ON DELETE RESTRICT,
    FOREIGN KEY (`id_d`) REFERENCES  `data_itemData`  (`id`) ON DELETE CASCADE,
    CONSTRAINT  `pk_i`   PRIMARY KEY (`id_l`, `id_d`),

    INDEX `index_i_id_l`        (`id_l`),
    INDEX `index_i_id_d`        (`id_d`),
    INDEX `index_i_pk`          (`id_l`, `id_d`),
    INDEX `index_i_volatile`    (`volatile`),
    INDEX `index_i_multiplier`  (`multiplier`),
    INDEX `index_i_mean`        (`mean`),
    INDEX `index_i_median`      (`median`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure `league_entries`
--

CREATE TABLE `league_entries` (
    `id_l`                INT             UNSIGNED NOT NULL,
    `id_d`                INT             UNSIGNED NOT NULL,
    `time`                TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `approved`            TINYINT(1)      UNSIGNED NOT NULL DEFAULT 0,
    `price`               DECIMAL(10,4)   UNSIGNED NOT NULL,
    `account`             VARCHAR(32)     NOT NULL,
    `itemid`              VARCHAR(32)     NOT NULL,

    FOREIGN KEY (`id_l`) REFERENCES  `data_leagues`   (`id`) ON DELETE RESTRICT,
    FOREIGN KEY (`id_d`) REFERENCES  `league_items` (`id_d`) ON DELETE CASCADE,
    CONSTRAINT  `pk_e`   PRIMARY KEY (`id_l`, `id_d`, `account`),

    INDEX `index_e_id_l`          (`id_l`),
    INDEX `index_e_id_d`          (`id_d`),
    INDEX `index_e_time`          (`time`),
    INDEX `index_e_approved`      (`approved`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------------------------------------------------------
-- League history tables
-- --------------------------------------------------------------------------------------------------------------------

--
-- Table structure `league_history_daily_inactive`
--

CREATE TABLE `league_history_daily_inactive` (
    `id_l`                INT             UNSIGNED NOT NULL,
    `id_d`                INT             UNSIGNED NOT NULL,
    `time`                TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `volatile`            TINYINT(1)      UNSIGNED DEFAULT NULL,
    `mean`                DECIMAL(10,4)   UNSIGNED DEFAULT NULL,
    `median`              DECIMAL(10,4)   UNSIGNED DEFAULT NULL,
    `mode`                DECIMAL(10,4)   UNSIGNED DEFAULT NULL,
    `exalted`             DECIMAL(10,4)   UNSIGNED DEFAULT NULL,
    `inc`                 INT(8)          UNSIGNED DEFAULT NULL,
    `dec`                 INT(8)          UNSIGNED DEFAULT NULL,
    `count`               INT(16)         UNSIGNED DEFAULT NULL,
    `quantity`            INT(8)          UNSIGNED DEFAULT NULL,

    FOREIGN KEY (`id_l`)  REFERENCES `data_leagues`  (`id`) ON DELETE RESTRICT,
    FOREIGN KEY (`id_d`)  REFERENCES `data_itemData` (`id`) ON DELETE RESTRICT,

    INDEX `index_hir_id_l`         (`id_l`),
    INDEX `index_hir_id_d`         (`id_d`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure `league_history_daily_rolling`
--

CREATE TABLE `league_history_daily_rolling` (
    `id_l`                INT             UNSIGNED NOT NULL,
    `id_d`                INT             UNSIGNED NOT NULL,
    `time`                TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `volatile`            TINYINT(1)      UNSIGNED DEFAULT NULL,
    `mean`                DECIMAL(10,4)   UNSIGNED DEFAULT NULL,
    `median`              DECIMAL(10,4)   UNSIGNED DEFAULT NULL,
    `mode`                DECIMAL(10,4)   UNSIGNED DEFAULT NULL,
    `exalted`             DECIMAL(10,4)   UNSIGNED DEFAULT NULL,
    `inc`                 INT(8)          UNSIGNED DEFAULT NULL,
    `dec`                 INT(8)          UNSIGNED DEFAULT NULL,
    `count`               INT(16)         UNSIGNED DEFAULT NULL,
    `quantity`            INT(8)          UNSIGNED DEFAULT NULL,

    FOREIGN KEY (`id_l`)  REFERENCES `data_leagues`  (`id`) ON DELETE RESTRICT,
    FOREIGN KEY (`id_d`)  REFERENCES `data_itemData` (`id`) ON DELETE RESTRICT,

    INDEX `index_hdr_id_l`         (`id_l`),
    INDEX `index_hdr_id_d`         (`id_d`),
    INDEX `index_hdr_time`         (`time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure `league_history_hourly_rolling`
--

CREATE TABLE `league_history_hourly_rolling` (
    `id_l`                INT             UNSIGNED NOT NULL,
    `id_d`                INT             UNSIGNED NOT NULL,
    `time`                TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `volatile`            TINYINT(1)      UNSIGNED DEFAULT NULL,
    `mean`                DECIMAL(10,4)   UNSIGNED DEFAULT NULL,
    `median`              DECIMAL(10,4)   UNSIGNED DEFAULT NULL,
    `mode`                DECIMAL(10,4)   UNSIGNED DEFAULT NULL,
    `exalted`             DECIMAL(10,4)   UNSIGNED DEFAULT NULL,
    `inc`                 INT(8)          UNSIGNED DEFAULT NULL,
    `dec`                 INT(8)          UNSIGNED DEFAULT NULL,
    `count`               INT(16)         UNSIGNED DEFAULT NULL,
    `quantity`            INT(8)          UNSIGNED DEFAULT NULL,

    FOREIGN KEY (`id_l`)  REFERENCES `data_leagues`  (`id`) ON DELETE RESTRICT,
    FOREIGN KEY (`id_d`)  REFERENCES `data_itemData` (`id`) ON DELETE RESTRICT,

    INDEX `index_hhr_id_l`         (`id_l`),
    INDEX `index_hhr_id_d`         (`id_d`),
    INDEX `index_hhr_time`         (`time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure `league_history_minutely_rolling`
--

CREATE TABLE `league_history_minutely_rolling` (
    `id_l`                INT             UNSIGNED NOT NULL,
    `id_d`                INT             UNSIGNED NOT NULL,
    `time`                TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `mean`                DECIMAL(10,4)   UNSIGNED DEFAULT NULL,
    `median`              DECIMAL(10,4)   UNSIGNED DEFAULT NULL,
    `mode`                DECIMAL(10,4)   UNSIGNED DEFAULT NULL,
    `exalted`             DECIMAL(10,4)   UNSIGNED DEFAULT NULL,

    FOREIGN KEY (`id_l`)  REFERENCES `data_leagues`  (`id`) ON DELETE RESTRICT,
    FOREIGN KEY (`id_d`)  REFERENCES `data_itemData` (`id`) ON DELETE RESTRICT,

    INDEX `index_hmr_id_l`         (`id_l`),
    INDEX `index_hmr_id_d`         (`id_d`),
    INDEX `index_hmr_time`         (`time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------------------------------------------------------
-- Base values
-- --------------------------------------------------------------------------------------------------------------------

--
-- Base values for `data_leagues`
--

INSERT INTO `data_leagues`
    (`id`, `active`, `name`, `display`)
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
-- Base value for `data_changeId`
--

INSERT INTO `data_changeId`
    (`changeId`)
VALUES
    ('0-0-0-0-0');

--
-- Base values for `category_history`
--

INSERT INTO `category_history`
    (`id`, `name`)
VALUES
    (1, 'minutely'),
    (2, 'hourly'),                                -- Notice: id 2 dependency
    (3, 'daily'),
    (4, 'weekly');

--
-- Base values for `category_parent`
--

INSERT INTO `category_parent`
    (`id`, `name`, `display`)
VALUES
    (1,   'accessories',    'Accessories'),
    (2,   'armour',         'Armour'),
    (3,   'cards',          'Divination cards'),
    (4,   'currency',       'Currency'),          -- Notice: id 4 dependency
    (5,   'enchantments',   'Enchants'),
    (6,   'essence',        'Essences'),
    (7,   'flasks',         'Flasks'),
    (8,   'gems',           'Gems'),
    (9,   'jewels',         'Jewels'),
    (10,  'maps',           'Maps'),
    (11,  'prophecy',       'Prophecy'),
    (12,  'weapons',        'Weapons');

--
-- Base values for `category_child`
--

INSERT INTO `category_child`
    (`id_cp`, `name`, `display`)
VALUES
    (1,     'amulet',     'Amulets'),
    (1,     'belt',       'Belts'),
    (1,     'ring',       'Rings'),
    (2,     'boots',      'Boots'),
    (2,     'chest',      'Body Armour'),
    (2,     'gloves',     'Gloves'),
    (2,     'helmet',     'Helmets'),
    (2,     'quiver',     'Quivers'),
    (2,     'shield',     'Shields'),
    (4,     'piece',      'Pieces'),
    (5,     'boots',      'Boots'),
    (5,     'gloves',     'Gloves'),
    (5,     'helmet',     'Helmets'),
    (8,     'activegem',  'Skill Gems'),
    (8,     'supportgem', 'Support Gems'),
    (8,     'vaalgem',    'Vaal Gems'),
    (10,    'fragment',   'Fragments'),
    (10,    'map',        'Maps'),
    (10,    'unique',     'Unique Maps'),
    (12,    'bow',        'Bows'),
    (12,    'claw',       'Claws'),
    (12,    'dagger',     'Daggers'),
    (12,    'oneaxe',     '1H Axes'),
    (12,    'onemace',    '1H Maces'),
    (12,    'onesword',   '1H Swords'),
    (12,    'rod',        'Rods'),
    (12,    'sceptre',    'Sceptres'),
    (12,    'staff',      'Staves'),
    (12,    'twoaxe',     '2H Axes'),
    (12,    'twomace',    '2H Maces'),
    (12,    'twosword',   '2H Swords'),
    (12,    'wand',       'Wands');

--
-- Base values for `data_currencyItems`
--

INSERT INTO `data_currencyItems`
    (`id`, `name`)
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
-- Base values for `data_currencyAliases`
--

INSERT INTO `data_currencyAliases`
    (`id_ci`, `name`)
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
