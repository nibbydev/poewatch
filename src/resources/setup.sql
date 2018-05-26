SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";

--
-- Database: `ps_test_database`
--
DROP DATABASE IF EXISTS `ps_test_database`;
CREATE DATABASE IF NOT EXISTS `ps_test_database` DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;

USE `ps_test_database`;

-- --------------------------------------------------------

--
-- Table structure `category_parent`
--

CREATE TABLE `category_parent` (
    `parent`    varchar(32)     PRIMARY KEY,
    `display`   varchar(32)     DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure `category_child`
--

CREATE TABLE `category_child` (
    CONSTRAINT `parent-child`
        PRIMARY KEY (`parent`,`child`),
    FOREIGN KEY (`parent`)
        REFERENCES `category_parent` (`parent`)
        ON DELETE CASCADE,

    `parent`    varchar(32)     NOT NULL,
    `child`     varchar(32)     NOT NULL,
    `display`   varchar(32)     DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure `item_data_sup`
--

CREATE TABLE `item_data_sup` (
    FOREIGN KEY (`parent`)
        REFERENCES `category_parent` (`parent`)
        ON DELETE CASCADE,
    CONSTRAINT `parent-child`
        FOREIGN KEY (`parent`,`child`)
        REFERENCES `category_child` (`parent`,`child`)
        ON DELETE CASCADE,

    `sup`       varchar(5)      PRIMARY KEY,
    `parent`    varchar(32)     NOT NULL,
    `child`     varchar(32)     DEFAULT NULL,

    `name`      varchar(128)    NOT NULL,
    `type`      varchar(64)     DEFAULT NULL,
    `frame`     tinyint(1)      NOT NULL,
    `key`       varchar(128)    NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure `item_data_sub`
--

CREATE TABLE `item_data_sub` (
    CONSTRAINT `sup-sub` 
        PRIMARY KEY (`sup`,`sub`),
    FOREIGN KEY (`sup`)
        REFERENCES `item_data_sup` (`sup`)
        ON DELETE CASCADE,
    
    `sup`       varchar(5)      NOT NULL,
    `sub`       varchar(2)      NOT NULL,

    `tier`      tinyint(1)      DEFAULT NULL,
    `lvl`       tinyint(1)      DEFAULT NULL,
    `quality`   tinyint(1)      DEFAULT NULL,
    `corrupted` tinyint(1)      DEFAULT NULL,
    `links`     tinyint(1)      DEFAULT NULL,
    `var`       varchar(32)     DEFAULT NULL,
    `key`       varchar(128)    NOT NULL,
    `icon`      text            NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure for table `league__item`
-- (to be created dynamically with league names)
--

CREATE TABLE `league__item` (
    CONSTRAINT `__sup-sub`
        FOREIGN KEY (`sup`,`sub`) 
        REFERENCES `item_data_sub` (`sup`,`sub`)
        ON DELETE CASCADE,

    `sup`       varchar(5)      NOT NULL,
    `sub`       varchar(2)      NOT NULL,

    `time`      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `mean`      decimal(9,3)    unsigned NOT NULL DEFAULT 0.0,
    `median`    decimal(9,3)    unsigned NOT NULL DEFAULT 0.0,
    `mode`      decimal(9,3)    unsigned NOT NULL DEFAULT 0.0,
    `exalted`   decimal(9,3)    unsigned NOT NULL DEFAULT 0.0,
    `count`     int(16)         unsigned NOT NULL DEFAULT 0,
    `quantity`  int(8)          unsigned NOT NULL DEFAULT 0,
    `inc`       int(8)          unsigned NOT NULL DEFAULT 0,
    `dec`       int(8)          unsigned NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure `league__item_history`
-- (to be created dynamically with league names)
--

CREATE TABLE `league__history` (
    CONSTRAINT `__history`
        FOREIGN KEY (`sup`,`sub`)
        REFERENCES `league__item` (`sup`,`sub`)
        ON DELETE CASCADE,

    `sup`       varchar(5)      NOT NULL,
    `sub`       varchar(2)      NOT NULL,

    `time`      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `mean`      decimal(9,3)    unsigned DEFAULT NULL,
    `median`    decimal(9,3)    unsigned DEFAULT NULL,
    `mode`      decimal(9,3)    unsigned DEFAULT NULL,
    `count`     int(16)         unsigned DEFAULT NULL,
    `quantity`  int(8)          unsigned DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure `league__entry`
-- (to be created dynamically with league names)
--

CREATE TABLE `league__entry` (
    CONSTRAINT `__item_entry`
        FOREIGN KEY (`sup`,`sub`)
        REFERENCES `league__item` (`sup`,`sub`)
        ON DELETE CASCADE,

    `sup`       varchar(5)      NOT NULL,
    `sub`       varchar(2)      NOT NULL,

    `time`      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `price`     decimal(9,3)    NOT NULL,
    `account`   varchar(32)     NOT NULL,
    `id`        varchar(32)     NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure `history_entry_category`
--

CREATE TABLE `history_entry_category` (
    `type`      varchar(32)     PRIMARY KEY
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure `league__history_entry`
-- (to be created dynamically with league names)
--

CREATE TABLE `league__history_entry` (
    CONSTRAINT `__history_entry`
        FOREIGN KEY (`sup`,`sub`)
        REFERENCES `league__item` (`sup`,`sub`)
        ON DELETE CASCADE,
    FOREIGN KEY (`type`)
        REFERENCES `history_entry_category` (`type`)
        ON DELETE CASCADE,

    `sup`       varchar(5)      NOT NULL,
    `sub`       varchar(2)      NOT NULL,
    `type`      varchar(32)     NOT NULL,

    `time`      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `mean`      decimal(9,3)    unsigned DEFAULT NULL,
    `median`    decimal(9,3)    unsigned DEFAULT NULL,
    `mode`      decimal(9,3)    unsigned DEFAULT NULL,
    `count`     int(16)         unsigned DEFAULT NULL,
    `quantity`  int(8)          unsigned DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure `leagues`
--

CREATE TABLE `leagues` (
    `id`        varchar(64)     PRIMARY KEY,
    `display`   varchar(64)     DEFAULT NULL,
    `start`     varchar(32)     DEFAULT NULL,
    `end`       varchar(32)     DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure `status`
--

CREATE TABLE `status` (
    `name`      varchar(32)     PRIMARY KEY,
    `val`       bigint(19)      unsigned NOT NULL DEFAULT 0,
    `time`      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure `changeid`
--

CREATE TABLE `changeid` (
    `changeid`  varchar(128)    PRIMARY KEY,
    `time`      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
