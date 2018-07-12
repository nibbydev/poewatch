-- --------------------------------------------------------------------------------------------------------------------
-- Initial configuration
-- --------------------------------------------------------------------------------------------------------------------

--
-- Database: ps_accounts
--

DROP DATABASE IF EXISTS ps_accounts;
CREATE DATABASE IF NOT EXISTS ps_accounts DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE ps_accounts;

-- --------------------------------------------------------------------------------------------------------------------
-- Table setup
-- --------------------------------------------------------------------------------------------------------------------

--
-- Table structure accounts
--

CREATE TABLE accounts (
    id      INT           UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    name    VARCHAR(32)   NOT NULL UNIQUE,
    found   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    seen    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX   index_a_id    (id),
    INDEX   index_a_name  (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Table structure characters
--

CREATE TABLE characters (
    id      INT           UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    name    VARCHAR(32)   NOT NULL UNIQUE,
    found   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    seen    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX   index_c_id    (id),
    INDEX   index_c_name  (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Table structure relations
--

CREATE TABLE relations (
    id_a    INT           UNSIGNED NOT NULL,
    id_c    INT           UNSIGNED NOT NULL,
    found   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    seen    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (id_a)    REFERENCES  accounts      (id) ON DELETE RESTRICT,
    FOREIGN KEY (id_c)    REFERENCES  characters    (id) ON DELETE RESTRICT,
    CONSTRAINT  pk_r      PRIMARY KEY (id_a, id_c),

    INDEX   index_r_id_a  (id_a),
    INDEX   index_r_id_c  (id_c)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------------------------------------------------------------------
-- Testing
-- --------------------------------------------------------------------------------------------------------------------

INSERT INTO accounts (name) VALUES ('test676796😜😀😊😃test676896 我爱你 ❌');
SET @id_a = LAST_INSERT_ID();
INSERT INTO characters (name) VALUES ('test123123😜😀😊😃test123123 我爱你 ❌');
SET @id_c = LAST_INSERT_ID();
INSERT INTO relations (id_a, id_c) VALUES (@id_a, @id_c);
