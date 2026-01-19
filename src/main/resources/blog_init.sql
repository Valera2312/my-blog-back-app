CREATE SCHEMA IF NOT EXISTS blog;

CREATE TABLE IF NOT EXISTS posts (
                       id              BIGSERIAL PRIMARY KEY,
                       title           VARCHAR(255) NOT NULL,
                       text            TEXT NOT NULL,
                       comments_count  INTEGER NOT NULL DEFAULT 0,
                       likes_count     INTEGER NOT NULL DEFAULT 0,
                       created_at      TIMESTAMP NOT NULL DEFAULT now(),
                       updated_at      TIMESTAMP NOT NULL DEFAULT now()
);


CREATE TABLE IF NOT EXISTS tags (
                       id              BIGSERIAL PRIMARY KEY,
                       name            VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS post_tags (
                       post_id         BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
                       tag_id          BIGINT NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
                       PRIMARY KEY (post_id, tag_id)
);
CREATE TABLE IF NOT EXISTS images (
                       post_id         BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE PRIMARY KEY,
                       url             VARCHAR(255) NOT NULL
);
CREATE TABLE IF NOT EXISTS comments (
                       id              BIGSERIAL PRIMARY KEY,
                       post_id         BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
                       text            TEXT NOT NULL
);
