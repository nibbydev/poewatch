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
    name     VARCHAR(32)  NOT NULL UNIQUE,
    display  VARCHAR(32)  DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure data_leagues
--

CREATE TABLE data_leagues (
    id         SMALLINT     UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    active     TINYINT(1)   UNSIGNED NOT NULL DEFAULT 1,
    upcoming   TINYINT(1)   UNSIGNED NOT NULL DEFAULT 0,
    event      TINYINT(1)   UNSIGNED NOT NULL DEFAULT 0,
    hardcore   TINYINT(1)   UNSIGNED NOT NULL DEFAULT 0,
    challenge  TINYINT(1)   UNSIGNED NOT NULL DEFAULT 0,
    name       VARCHAR(64)  NOT NULL UNIQUE,
    display    VARCHAR(64)  DEFAULT NULL,
    start      VARCHAR(32)  DEFAULT NULL,
    end        VARCHAR(32)  DEFAULT NULL,

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
-- Table structure data_statistics
--

CREATE TABLE data_statistics (
    type      VARCHAR(32)  NOT NULL,
    time      TIMESTAMP    NOT NULL,
    value     INT          DEFAULT NULL,

    INDEX type (type),
    INDEX time (time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure data_statistics_tmp
--

CREATE TABLE data_statistics_tmp (
    type        VARCHAR(32)  NOT NULL PRIMARY KEY,
    created     TIMESTAMP    NOT NULL,
    sum         BIGINT       DEFAULT NULL,
    count       INT          NOT NULL
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
    reindex    BIT(1)        NOT NULL DEFAULT 0,
    name       VARCHAR(128)  NOT NULL,
    type       VARCHAR(64)   DEFAULT NULL,
    frame      TINYINT(1)    NOT NULL,
    found      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    stack      SMALLINT      DEFAULT NULL,
    tier       TINYINT(1)    UNSIGNED DEFAULT NULL,
    series     TINYINT(1)    UNSIGNED DEFAULT NULL,
    shaper     BIT(1)        DEFAULT NULL,
    elder      BIT(1)        DEFAULT NULL,
    enchantMin DECIMAL(4,1)  DEFAULT NULL,
    enchantMax DECIMAL(4,1)  DEFAULT NULL,
    lvl        TINYINT(1)    UNSIGNED DEFAULT NULL,
    quality    TINYINT(1)    UNSIGNED DEFAULT NULL,
    corrupted  TINYINT(1)    UNSIGNED DEFAULT NULL,
    links      TINYINT(1)    UNSIGNED DEFAULT NULL,
    ilvl       TINYINT(1)    UNSIGNED DEFAULT NULL,
    var        VARCHAR(32)   DEFAULT NULL,
    icon       VARCHAR(256)  NOT NULL,

    FOREIGN KEY (id_cat) REFERENCES data_categories (id) ON DELETE CASCADE,
    FOREIGN KEY (id_grp) REFERENCES data_groups     (id) ON DELETE CASCADE,

    CONSTRAINT idx_unique UNIQUE (name, type, frame, tier, lvl, quality, corrupted, links, ilvl, var, shaper, elder, enchantMin, enchantMax),

    INDEX reindex (reindex),
    INDEX frame   (frame),
    INDEX name    (name)
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
    found    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    seen     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    mean     DECIMAL(14,8)  UNSIGNED NOT NULL DEFAULT 0.0,
    median   DECIMAL(14,8)  UNSIGNED NOT NULL DEFAULT 0.0,
    mode     DECIMAL(14,8)  UNSIGNED NOT NULL DEFAULT 0.0,
    min      DECIMAL(14,8)  UNSIGNED NOT NULL DEFAULT 0.0,
    max      DECIMAL(14,8)  UNSIGNED NOT NULL DEFAULT 0.0,
    exalted  DECIMAL(14,8)  UNSIGNED NOT NULL DEFAULT 0.0,
    total    INT(16)        UNSIGNED NOT NULL DEFAULT 0,
    daily    INT(8)         UNSIGNED NOT NULL DEFAULT 0,
    current  INT(8)         UNSIGNED NOT NULL DEFAULT 0,
    accepted INT(8)         UNSIGNED NOT NULL DEFAULT 0,
    spark    VARCHAR(128)   DEFAULT NULL,

    FOREIGN KEY (id_l) REFERENCES data_leagues  (id) ON DELETE RESTRICT,
    FOREIGN KEY (id_d) REFERENCES data_itemData (id) ON DELETE CASCADE,
    CONSTRAINT pk PRIMARY KEY (id_l, id_d),

    INDEX total    (total),
    INDEX median   (median)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure league_accounts
--

CREATE TABLE league_accounts (
     id     BIGINT       UNSIGNED PRIMARY KEY AUTO_INCREMENT,

     name   VARCHAR(32)  NOT NULL UNIQUE,
     found  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
     seen   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

     INDEX seen (seen)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Table structure league_characters
--

CREATE TABLE league_characters (
    id     BIGINT       UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    id_l   SMALLINT     UNSIGNED NOT NULL,
    id_a   BIGINT       UNSIGNED NOT NULL,

    name   VARCHAR(32)  NOT NULL UNIQUE,
    found  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    seen   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (id_l) REFERENCES data_leagues    (id) ON DELETE CASCADE,
    FOREIGN KEY (id_a) REFERENCES league_accounts (id) ON DELETE CASCADE,

    INDEX seen (seen)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Table structure league_entries
--

CREATE TABLE league_entries (
    id_l         SMALLINT       UNSIGNED NOT NULL,
    id_d         INT            UNSIGNED NOT NULL,
    id_a         BIGINT         UNSIGNED NOT NULL,

    stash_crc    INT            UNSIGNED DEFAULT NULL,
    item_crc     INT            UNSIGNED NOT NULL,

    discovered   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updates      SMALLINT       UNSIGNED NOT NULL DEFAULT 1,

    stack        SMALLINT       UNSIGNED DEFAULT NULL,
    price        DECIMAL(14,8)  UNSIGNED DEFAULT NULL,
    id_price     INT            UNSIGNED DEFAULT NULL,

    FOREIGN KEY (id_l) REFERENCES data_leagues (id) ON DELETE RESTRICT,
    FOREIGN KEY (id_d) REFERENCES data_itemData (id) ON DELETE CASCADE,
    FOREIGN KEY (id_a) REFERENCES league_accounts (id) ON DELETE CASCADE,
    FOREIGN KEY (id_price) REFERENCES data_itemData (id) ON DELETE CASCADE,
    CONSTRAINT pk PRIMARY KEY (id_l, id_d, id_a, item_crc),

    INDEX discovered (discovered),
    INDEX updated (updated),
    INDEX del (stash_crc, updated)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------------------------------------------------------
-- League history tables
-- --------------------------------------------------------------------------------------------------------------------

CREATE TABLE league_history_daily (
    id_l     SMALLINT       UNSIGNED NOT NULL,
    id_d     INT            UNSIGNED NOT NULL,
    time     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mean     DECIMAL(14,8)  UNSIGNED NOT NULL,
    median   DECIMAL(14,8)  UNSIGNED NOT NULL,
    mode     DECIMAL(14,8)  UNSIGNED NOT NULL,
    min      DECIMAL(14,8)  UNSIGNED NOT NULL,
    max      DECIMAL(14,8)  UNSIGNED NOT NULL,
    exalted  DECIMAL(14,8)  UNSIGNED NOT NULL,
    total    INT(16)        UNSIGNED NOT NULL,
    daily    INT(8)         UNSIGNED NOT NULL,
    current  INT(8)         UNSIGNED NOT NULL,
    accepted INT(8)         UNSIGNED NOT NULL,

    FOREIGN KEY (id_l) REFERENCES data_leagues  (id) ON DELETE RESTRICT,
    FOREIGN KEY (id_d) REFERENCES data_itemData (id) ON DELETE CASCADE,

    INDEX time (time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------------------------------------------------------
-- Web tables
-- --------------------------------------------------------------------------------------------------------------------

--
-- Table structure web_feedback
--

CREATE TABLE web_feedback (
    id        INT           UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    time      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_crc    INT           UNSIGNED NOT NULL,
    contact   VARCHAR(128)  NOT NULL,
    message   TEXT          NOT NULL,

    INDEX ip_crc  (ip_crc),
    INDEX `time`  (`time`),
    INDEX contact (contact)
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
    (18, 0, 0, 'Delve',                         'Delve'             ),
    (19, 0, 1, 'Hardcore Betrayal',             'HC Betrayal'       ),
    (20, 0, 0, 'Betrayal',                      'Betrayal'          ),
    (21, 0, 1, 'Hardcore Synthesis',            'HC Synthesis'      ),
    (22, 0, 0, 'Synthesis',                     'Synthesis'         );

--
-- Base value for data_changeId
--

INSERT INTO data_changeId
    (changeId)
VALUES
    ('0-0-0-0-0');

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
