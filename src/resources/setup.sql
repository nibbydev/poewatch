SET time_zone = "+02:00";

--
-- Database: `ps_database`
--
DROP DATABASE IF EXISTS `ps_database`;
CREATE DATABASE IF NOT EXISTS `ps_database` DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;

USE `ps_database`;

-- --------------------------------------------------------

--
-- Table structure `category_parent`
--

CREATE TABLE `category_parent` (
    `id`                  int             unsigned PRIMARY KEY AUTO_INCREMENT,
    `name`                varchar(32)     NOT NULL UNIQUE,
    `display`             varchar(32)     DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX `index-cp-id` ON `category_parent` (`id`);
CREATE INDEX `index-cp-name` ON `category_parent` (`name`);

--
-- Table structure `category_child`
--

CREATE TABLE `category_child` (
    FOREIGN KEY (`id_parent`)
        REFERENCES `category_parent` (`id`)
        ON DELETE CASCADE,

    `id`                  int             unsigned PRIMARY KEY AUTO_INCREMENT,
    `id_parent`           int             unsigned NOT NULL,
    `name`                varchar(32)     NOT NULL,
    `display`             varchar(32)     DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX `index-cc-id` ON `category_child` (`id`);
CREATE INDEX `index-cc-id_p` ON `category_child` (`id_parent`);
CREATE INDEX `index-cc-name` ON `category_child` (`name`);

--
-- Table structure `category_history`
--

CREATE TABLE `category_history` (
    `id`                  int             unsigned PRIMARY KEY AUTO_INCREMENT,
    `name`                varchar(32)     NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX `index-ch-id` ON `category_history` (`id`);

--
-- Table structure `item_data_parent`
--

CREATE TABLE `item_data_parent` (
    FOREIGN KEY (`id_category_parent`)
        REFERENCES `category_parent` (`id`)
        ON DELETE CASCADE,
    FOREIGN KEY (`id_category_child`)
        REFERENCES `category_child` (`id`)
        ON DELETE CASCADE,

    `id`                  int             unsigned PRIMARY KEY AUTO_INCREMENT,
    `id_category_parent`  int             unsigned NOT NULL,
    `id_category_child`   int             unsigned DEFAULT NULL,

    `name`                varchar(128)    NOT NULL,
    `type`                varchar(64)     DEFAULT NULL,
    `frame`               tinyint(1)      NOT NULL,
    `key`                 varchar(128)    NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX `index-idp-id` ON `item_data_parent` (`id`);
CREATE INDEX `index-idp-id_cp` ON `item_data_parent` (`id_category_parent`);
CREATE INDEX `index-idp-id_cc` ON `item_data_parent` (`id_category_child`);
CREATE INDEX `index-idp-key` ON `item_data_parent` (`key`);
CREATE INDEX `index-idp-frame` ON `item_data_parent` (`frame`);

--
-- Table structure `item_data_child`
--

CREATE TABLE `item_data_child` (
    FOREIGN KEY (`id_parent`)
        REFERENCES `item_data_parent` (`id`)
        ON DELETE CASCADE,

    `id`                  int             unsigned PRIMARY KEY AUTO_INCREMENT,
    `id_parent`           int             unsigned NOT NULL,

    `tier`                tinyint(1)      unsigned DEFAULT NULL,
    `lvl`                 tinyint(1)      unsigned DEFAULT NULL,
    `quality`             tinyint(1)      unsigned DEFAULT NULL,
    `corrupted`           tinyint(1)      unsigned DEFAULT NULL,
    `links`               tinyint(1)      unsigned DEFAULT NULL,
    `var`                 varchar(32)     DEFAULT NULL,
    `key`                 varchar(128)    NOT NULL UNIQUE,
    `icon`                text            NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX `index-idc-id` ON `item_data_child` (`id`);
CREATE INDEX `index-idc-id_p` ON `item_data_child` (`id_parent`);
CREATE INDEX `index-idc-key` ON `item_data_child` (`key`);

--
-- Table structure for table `#_item_`
-- (to be created dynamically with league names)
--

CREATE TABLE `#_item_` (
    FOREIGN KEY (`id_data_parent`)
        REFERENCES `item_data_parent` (`id`)
        ON DELETE CASCADE,
    FOREIGN KEY (`id_data_child`)
        REFERENCES `item_data_child` (`id`)
        ON DELETE CASCADE,

    `id`                  int             unsigned PRIMARY KEY AUTO_INCREMENT,
    `id_data_parent`      int             unsigned NOT NULL,
    `id_data_child`       int             unsigned NOT NULL,
    `time`                TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    `mean`                decimal(10,4)   unsigned NOT NULL DEFAULT 0.0,
    `median`              decimal(10,4)   unsigned NOT NULL DEFAULT 0.0,
    `mode`                decimal(10,4)   unsigned NOT NULL DEFAULT 0.0,
    `exalted`             decimal(10,4)   unsigned NOT NULL DEFAULT 0.0,
    `count`               int(16)         unsigned NOT NULL DEFAULT 0,
    `quantity`            int(8)          unsigned NOT NULL DEFAULT 0,
    `inc`                 int(8)          unsigned NOT NULL DEFAULT 0,
    `dec`                 int(8)          unsigned NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX `index-i-id` ON `#_item_` (`id`);
CREATE INDEX `index-i-id_idp` ON `#_item_` (`id_data_parent`);
CREATE INDEX `index-i-id_idc` ON `#_item_` (`id_data_child`);
CREATE INDEX `index-i-mean` ON `#_item_` (`mean`);

--
-- Table structure `#_entry_`
-- (to be created dynamically with league names)
--

CREATE TABLE `#_entry_` (
    FOREIGN KEY (`id_item`)
        REFERENCES `#_item_` (`id`)
        ON DELETE CASCADE,

    `id_item`             int             unsigned NOT NULL,
    `time`                TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    `price`               decimal(10,4)   unsigned NOT NULL,
    `account`             varchar(32)     NOT NULL UNIQUE,
    `item_id`             varchar(32)     NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX `index-e-id_i` ON `#_entry_` (`id_item`);
CREATE INDEX `index-e-time` ON `#_entry_` (`time`);
CREATE INDEX `index-e-account` ON `#_entry_` (`account`);

--
-- Table structure `#_history_`
-- (to be created dynamically with league names)
--

CREATE TABLE `#_history_` (
    FOREIGN KEY (`id`)
        REFERENCES `#_item_` (`id`)
        ON DELETE CASCADE,
    FOREIGN KEY (`id_type`)
        REFERENCES `category_history` (`id`)
        ON DELETE RESTRICT,

    `id`                  int             unsigned NOT NULL,
    `id_type`             int             unsigned NOT NULL,
    `time`                TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    `mean`                decimal(10,4)   unsigned DEFAULT NULL,
    `median`              decimal(10,4)   unsigned DEFAULT NULL,
    `mode`                decimal(10,4)   unsigned DEFAULT NULL,
    `exalted`             decimal(10,4)   unsigned DEFAULT NULL,
    `inc`                 int(8)          unsigned DEFAULT NULL,
    `dec`                 int(8)          unsigned DEFAULT NULL,
    `count`               int(16)         unsigned DEFAULT NULL,
    `quantity`            int(8)          unsigned DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX `index-h-id` ON `#_history_` (`id`);
CREATE INDEX `index-h-id_t` ON `#_history_` (`id_type`);
CREATE INDEX `index-h-time` ON `#_history_` (`time`);

-- --------------------------------------------------------

--
-- Table structure `leagues`
--

CREATE TABLE `leagues` (
    `id`                  int             unsigned PRIMARY KEY AUTO_INCREMENT,

    `name`                varchar(64)     NOT NULL UNIQUE,
    `display`             varchar(64)     DEFAULT NULL,
    `start`               varchar(32)     DEFAULT NULL,
    `end`                 varchar(32)     DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX `index-l-id` ON `leagues` (`id`);
CREATE INDEX `index-l-name` ON `leagues` (`name`);

--
-- Table structure `status`
--

CREATE TABLE `status` (
    `id`                  varchar(32)     PRIMARY KEY,
    `value`               bigint(19)      unsigned NOT NULL DEFAULT 0,
    `time`                TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX `index-s-id` ON `status` (`id`);

--
-- Table structure `change_id`
--

CREATE TABLE `change_id` (
    `id`                  varchar(128)    NOT NULL UNIQUE,
    `time`                TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure `output_files`
--

CREATE TABLE `output_files` (
    CONSTRAINT `pk_of`
        PRIMARY KEY (`league`, `category`),

    `league`              varchar(64)     NOT NULL,
    `category`            varchar(32)     NOT NULL,
    `path`                text            NOT NULL,
    `time`                TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX `index-of-league` ON `output_files` (`league`);
CREATE INDEX `index-of-category` ON `output_files` (`category`);

--
-- Table structure `currency_item`
--

CREATE TABLE `currency_item` (
    `id`                  int             unsigned PRIMARY KEY AUTO_INCREMENT,
    `name`                varchar(64)     NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX `index-ci-id` ON `currency_item` (`id`);

--
-- Table structure `currency_alias`
--

CREATE TABLE `currency_alias` (
    FOREIGN KEY (`id_parent`)
        REFERENCES `currency_item` (`id`)
        ON DELETE CASCADE,

    `id`                    int           unsigned PRIMARY KEY AUTO_INCREMENT,
    `id_parent`             int           unsigned NOT NULL,

    `name`                  varchar(32)   NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX `index-ca-id` ON `currency_alias` (`id`);
CREATE INDEX `index-ca-id_p` ON `currency_alias` (`id_parent`);

-- --------------------------------------------------------

--
-- Base value for `change_id`
--

INSERT INTO `change_id`
    (`id`)
VALUES
    ('0-0-0-0-0');

--
-- Base values for `status`
--

INSERT INTO `status`
    (`id`)
VALUES
    ('lastRunTime'),
    ('tenCounter'),
    ('sixtyCounter'),
    ('twentyFourCounter');

--
-- Base values for `category_history`
--

INSERT INTO `category_history`
    (`name`)
VALUES
    ('minutely'),
    ('hourly'),                             -- Notice: Hourly should always be id 2. (see sql_id_category_history_hourly)
    ('daily'),
    ('weekly');

--
-- Base values for `category_parent`
--

INSERT INTO `category_parent`
    (`name`, `display`)
VALUES
    ('accessories',   'Accessories'),
    ('armour',        'Armour'),
    ('cards',         'Divination cards'),
    ('currency',      'Currency'),          -- Notice: Currency should always be id 4.
    ('enchantments',  'Enchants'),
    ('essence',       'Essences'),
    ('flasks',        'Flasks'),
    ('gems',          'Gems'),
    ('jewels',        'Jewels'),
    ('maps',          'Maps'),
    ('prophecy',      'Prophecy'),
    ('weapons',       'Weapons');

--
-- Base values for `category_child`
--

INSERT INTO `category_child`
    (`id_parent`, `name`, `display`)
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

INSERT INTO `currency_item`
    (`name`)
VALUES
    ('Chaos Orb'),
    ('Exalted Orb'),
    ('Divine Orb'),
    ('Orb of Alchemy'),
    ('Orb of Fusing'),
    ('Orb of Alteration'),
    ('Regal Orb'),
    ('Vaal Orb'),
    ('Orb of Regret'),
    ('Cartographer''s Chisel'),
    ('Jeweller''s Orb'),
    ('Silver Coin'),
    ('Perandus Coin'),
    ('Orb of Scouring'),
    ('Gemcutter''s Prism'),
    ('Orb of Chance'),
    ('Chromatic Orb'),
    ('Blessed Orb'),
    ('Glassblower''s Bauble'),
    ('Orb of Augmentation'),
    ('Orb of Transmutation'),
    ('Mirror of Kalandra'),
    ('Scroll of Wisdom'),
    ('Portal Scroll'),
    ('Blacksmith''s Whetstone'),
    ('Armourer''s Scrap'),
    ('Apprentice Cartographer''s Sextant'),
    ('Journeyman Cartographer''s Sextant'),
    ('Master Cartographer''s Sextant');

--
-- Base values for `currency_alias`
--

INSERT INTO `currency_alias`
    (`id_parent`, `name`)
VALUES
    (1,     'chaos'),
    (1,     'choas'),
    (1,     'c'),
    (2,     'exalted'),
    (2,     'exalt'),
    (2,     'exa'),
    (2,     'ex'),
    (3,     'divine'),
    (3,     'div'),
    (4,     'alchemy'),
    (4,     'alch'),
    (4,     'alc'),
    (5,     'fusings'),
    (5,     'fusing'),
    (5,     'fuse'),
    (5,     'fus'),
    (6,     'alts'),
    (6,     'alteration'),
    (6,     'alt'),
    (7,     'regal'),
    (7,     'rega'),
    (8,     'vaal'),
    (9,     'regret'),
    (9,     'regrets'),
    (9,     'regr'),
    (10,    'chisel'),
    (10,    'chis'),
    (10,    'cart'),
    (11,    'jewellers'),
    (11,    'jeweller'),
    (11,    'jew'),
    (12,    'silver'),
    (13,    'coin'),
    (13,    'coins'),
    (13,    'perandus'),
    (14,    'scouring'),
    (14,    'scour'),
    (15,    'gcp'),
    (15,    'gemc'),
    (16,    'chance'),
    (16,    'chanc'),
    (17,    'chrome'),
    (17,    'chrom'),
    (18,    'blessed'),
    (18,    'bless'),
    (18,    'bles'),
    (19,    'glass'),
    (19,    'bauble'),
    (20,    'aug'),
    (21,    'tra'),
    (21,    'trans'),
    (22,    'mirror <disabled>'),
    (22,    'mir <disabled>'),
    (22,    'kal'),
    (23,    'wis'),
    (23,    'wisdom'),
    (24,    'port'),
    (24,    'portal'),
    (25,    'whetstone'),
    (25,    'blacksmith'),
    (25,    'whet'),
    (26,    'armour'),
    (26,    'scrap'),
    (27,    'apprentice-sextant'),
    (27,    'apprentice'),
    (28,    'journeyman-sextant'),
    (28,    'journeyman'),
    (29,    'master-sextant'),
    (29,    'master');

