-- --------------------------------------------------------------------------------------------------------------------
-- Initial configuration
-- --------------------------------------------------------------------------------------------------------------------

SET time_zone = "+02:00";

--
-- Database: `ps2_database`
--
DROP DATABASE IF EXISTS `ps2_database`;
CREATE DATABASE IF NOT EXISTS `ps2_database` DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;

USE `ps2_database`;

-- --------------------------------------------------------------------------------------------------------------------
-- Category tables
-- --------------------------------------------------------------------------------------------------------------------

--
-- Table structure `category-parent`
--

CREATE TABLE `category-parent` (
    `id`                  int             unsigned PRIMARY KEY AUTO_INCREMENT,
    `name`                varchar(32)     NOT NULL UNIQUE,
    `display`             varchar(32)     DEFAULT NULL,

    INDEX `ind-cp-id`     (`id`),
    INDEX `ind-cp-name`   (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure `category-child`
--

CREATE TABLE `category-child` (
    `id`                  int             unsigned PRIMARY KEY AUTO_INCREMENT,
    `id-cp`               int             unsigned NOT NULL,

    `name`                varchar(32)     NOT NULL,
    `display`             varchar(32)     DEFAULT NULL,

    FOREIGN KEY (`id-cp`)
        REFERENCES `category-parent` (`id`)
        ON DELETE CASCADE,

    INDEX `ind-cc-id`     (`id`),
    INDEX `ind-cc-id_p`   (`id-cp`),
    INDEX `ind-cc-name`   (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure `category-history`
--

CREATE TABLE `category-history` (
    `id`                  int             unsigned PRIMARY KEY AUTO_INCREMENT,
    `name`                varchar(32)     NOT NULL UNIQUE,
    INDEX `ind-ch-id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------------------------------------------------------
-- Item data
-- --------------------------------------------------------------------------------------------------------------------

--
-- Table structure `itemdata-parent`
--

CREATE TABLE `itemdata-parent` (
    `id`                  int             unsigned PRIMARY KEY AUTO_INCREMENT,
    `id-cp`               int             unsigned NOT NULL,
    `id-cc`               int             unsigned DEFAULT NULL,

    `name`                varchar(128)    NOT NULL,
    `type`                varchar(64)     DEFAULT NULL,
    `frame`               tinyint(1)      NOT NULL,
    `key`                 varchar(128)    NOT NULL UNIQUE,

    FOREIGN KEY (`id-cp`)
        REFERENCES `category-parent` (`id`)
        ON DELETE CASCADE,
    FOREIGN KEY (`id-cc`)
        REFERENCES `category-child` (`id`)
        ON DELETE CASCADE,

    INDEX `ind-idp-id`    (`id`),
    INDEX `ind-idp-id-cp` (`id-cp`),
    INDEX `ind-idp-id-cc` (`id-cc`),
    INDEX `ind-idp-key`   (`key`),
    INDEX `ind-idp-frame` (`frame`),
    INDEX `ind-idp-name`  (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure `itemdata-child`
--

CREATE TABLE `itemdata-child` (
    `id`                  int             unsigned PRIMARY KEY AUTO_INCREMENT,
    `id-idp`              int             unsigned NOT NULL,

    `tier`                tinyint(1)      unsigned DEFAULT NULL,
    `lvl`                 tinyint(1)      unsigned DEFAULT NULL,
    `quality`             tinyint(1)      unsigned DEFAULT NULL,
    `corrupted`           tinyint(1)      unsigned DEFAULT NULL,
    `links`               tinyint(1)      unsigned DEFAULT NULL,
    `var`                 varchar(32)     DEFAULT NULL,
    `key`                 varchar(128)    NOT NULL UNIQUE,
    `icon`                text            NOT NULL,

    FOREIGN KEY (`id-idp`)
        REFERENCES `itemdata-parent` (`id`)
        ON DELETE CASCADE,

    INDEX `ind-idc-id`    (`id`),
    INDEX `ind-idc-id-p`  (`id-idp`),
    INDEX `ind-idc-key`   (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------------------------------------------------------
-- League-specific tables
-- --------------------------------------------------------------------------------------------------------------------

--
-- Table structure for table `#_LEAGUE-items`
--

CREATE TABLE `#_LEAGUE-items` (
    `id`                  int             unsigned PRIMARY KEY AUTO_INCREMENT,
    `id-idp`              int             unsigned NOT NULL,
    `id-idc`              int             unsigned NOT NULL,
    `time`                timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    `volatile`            tinyint(1)      unsigned NOT NULL DEFAULT 0,
    `mean`                decimal(10,4)   unsigned NOT NULL DEFAULT 0.0,
    `median`              decimal(10,4)   unsigned NOT NULL DEFAULT 0.0,
    `mode`                decimal(10,4)   unsigned NOT NULL DEFAULT 0.0,
    `exalted`             decimal(10,4)   unsigned NOT NULL DEFAULT 0.0,
    `count`               int(16)         unsigned NOT NULL DEFAULT 0,
    `quantity`            int(8)          unsigned NOT NULL DEFAULT 0,
    `inc`                 int(8)          unsigned NOT NULL DEFAULT 0,
    `dec`                 int(8)          unsigned NOT NULL DEFAULT 0,

    FOREIGN KEY (`id-idp`)
        REFERENCES `itemdata-parent` (`id`)
        ON DELETE CASCADE,
    FOREIGN KEY (`id-idc`)
        REFERENCES `itemdata-child` (`id`)
        ON DELETE CASCADE,

    INDEX `ind-i-id`      (`id`),
    INDEX `ind-i-id-idp`  (`id-idp`),
    INDEX `ind-i-id-idc`  (`id-idc`),
    INDEX `ind-i-volatile`(`volatile`),
    INDEX `ind-i-mean`    (`mean`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure `#_LEAGUE-entries`
--

CREATE TABLE `#_LEAGUE-entries` (
    `id-i`                int             unsigned NOT NULL,
    `time`                timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    `price`               decimal(10,4)   unsigned NOT NULL,
    `account`             varchar(32)     NOT NULL UNIQUE,
    `itemid`              varchar(32)     NOT NULL,

    FOREIGN KEY (`id-i`)
        REFERENCES `#_LEAGUE-items` (`id`)
        ON DELETE CASCADE,

    INDEX `ind-e-id-i`    (`id-i`),
    INDEX `ind-e-time`    (`time`),
    INDEX `ind-e-price`   (`price`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure `#_LEAGUE-history`
--

CREATE TABLE `#_LEAGUE-history` (
    `id-i`                int             unsigned NOT NULL,
    `id-ch`               int             unsigned NOT NULL,
    `time`                timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    `volatile`            tinyint(1)      unsigned DEFAULT NULL,
    `mean`                decimal(10,4)   unsigned DEFAULT NULL,
    `median`              decimal(10,4)   unsigned DEFAULT NULL,
    `mode`                decimal(10,4)   unsigned DEFAULT NULL,
    `exalted`             decimal(10,4)   unsigned DEFAULT NULL,
    `inc`                 int(8)          unsigned DEFAULT NULL,
    `dec`                 int(8)          unsigned DEFAULT NULL,
    `count`               int(16)         unsigned DEFAULT NULL,
    `quantity`            int(8)          unsigned DEFAULT NULL,

    FOREIGN KEY (`id-i`)
        REFERENCES `#_LEAGUE-items` (`id`)
        ON DELETE CASCADE,
    FOREIGN KEY (`id-ch`)
        REFERENCES `category-history` (`id`)
        ON DELETE RESTRICT,

    INDEX `ind-h-id`      (`id-i`),
    INDEX `ind-h-id-ch`   (`id-ch`),
    INDEX `ind-h-volatile`(`volatile`),
    INDEX `ind-h-time`    (`time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------------------------------------------------------
-- System data tables
-- --------------------------------------------------------------------------------------------------------------------

--
-- Table structure `sys-leagues`
--

CREATE TABLE `sys-leagues` (
    `id`                  int             unsigned PRIMARY KEY AUTO_INCREMENT,
    `name`                varchar(64)     NOT NULL UNIQUE,
    `display`             varchar(64)     DEFAULT NULL,
    `start`               varchar(32)     DEFAULT NULL,
    `end`                 varchar(32)     DEFAULT NULL,

    INDEX `ind-l-id`      (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure `sys-change_id`
--

CREATE TABLE `sys-change_id` (
    `change_id`           varchar(128)    NOT NULL UNIQUE,
    `time`                TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure `sys-output_files`
--

CREATE TABLE `sys-output_files` (
    `league`              varchar(64)     NOT NULL,
    `category`            varchar(32)     NOT NULL,
    `path`                text            NOT NULL,
    `time`                TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT `pk-of`
        PRIMARY KEY (`league`, `category`),

    INDEX `ind-of-league` (`league`),
    INDEX `ind-of-category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure `sys-currency_items`
--

CREATE TABLE `sys-currency_items` (
    `id`                  int             unsigned PRIMARY KEY AUTO_INCREMENT,
    `name`                varchar(64)     NOT NULL,

    INDEX `ind-ci-id`     (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure `sys-currency_aliases`
--

CREATE TABLE `sys-currency_aliases` (
    `id`                    int           unsigned PRIMARY KEY AUTO_INCREMENT,
    `id-ci`                 int           unsigned NOT NULL,
    `name`                  varchar(32)   NOT NULL,

    FOREIGN KEY (`id-ci`)
        REFERENCES `sys-currency_items` (`id`)
        ON DELETE CASCADE,

    INDEX `ind-ca-id`       (`id`),
    INDEX `ind-ca-id-ci`      (`id-ci`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------------------------------------------------------
-- Base values
-- --------------------------------------------------------------------------------------------------------------------

--
-- Base value for `change_id`
--

INSERT INTO `sys-change_id`
    (`change_id`)
VALUES
    ('0-0-0-0-0');

--
-- Base values for `category-history`
--

INSERT INTO `category-history`
    (`id`, `name`)
VALUES
    (1, 'minutely'),
    (2, 'hourly'),                          -- Notice: Hourly should always be id 2 (see sql_id_category_history_hourly)
    (3, 'daily'),
    (4, 'weekly');

--
-- Base values for `category-parent`
--

INSERT INTO `category-parent`
    (`id`, `name`, `display`)
VALUES
    (1,   'accessories',    'Accessories'),
    (2,   'armour',         'Armour'),
    (3,   'cards',          'Divination cards'),
    (4,   'currency',       'Currency'),          -- Notice: Currency should always be id 4.
    (5,   'enchantments',   'Enchants'),
    (6,   'essence',        'Essences'),
    (7,   'flasks',         'Flasks'),
    (8,   'gems',           'Gems'),
    (9,   'jewels',         'Jewels'),
    (10,  'maps',           'Maps'),
    (11,  'prophecy',       'Prophecy'),
    (12,  'weapons',        'Weapons');

--
-- Base values for `category-child`
--

INSERT INTO `category-child`
    (`id-cp`, `name`, `display`)
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
-- Base values for `currency_item`
--

INSERT INTO `sys-currency_items`
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
-- Base values for `currency_alias`
--

INSERT INTO `sys-currency_aliases`
    (`id-ci`, `name`)
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
