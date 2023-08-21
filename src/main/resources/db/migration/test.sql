-- noinspection SqlNoDataSourceInspectionForFile

/*Entity*/
CREATE TABLE user
(
    id            UUID PRIMARY KEY,
    code          UUID         NOT NULL,
    username      VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password      VARCHAR(255) NOT NULL,
    first_name    VARCHAR(255),
    last_name     VARCHAR(255),
    date_of_birth DATE,
    created_at    TIMESTAMP DEFAULT NOW(),
    updated_at    TIMESTAMP DEFAULT NOW()
);

/*Entity*/
CREATE TABLE profile
(
    id              UUID PRIMARY KEY,
    code            UUID NOT NULL,
    user_id         UUID UNIQUE REFERENCES users (id),
    bio             TEXT,
    profile_picture VARCHAR(255),
    website         VARCHAR(255),
    phone_number    VARCHAR(15),
    country         VARCHAR(255),
    city            VARCHAR(255),
    address         TEXT
);

/*Entity*/
CREATE TABLE post
(
    id             UUID PRIMARY KEY,
    code           UUID         NOT NULL,
    user_id        UUID REFERENCES users (id),
    title          VARCHAR(255) NOT NULL,
    content        TEXT         NOT NULL,
    published_at   TIMESTAMP DEFAULT NOW(),
    likes          INT       DEFAULT 0,
    views          INT       DEFAULT 0,
    comments_count INT       DEFAULT 0,
    shared_count   INT       DEFAULT 0
);
/*Master*/
CREATE TABLE tag
(
    id                 UUID PRIMARY KEY,
    code               UUID         NOT NULL,
    name               VARCHAR(255) NOT NULL UNIQUE,
    description        TEXT,
    created_at         TIMESTAMP DEFAULT NOW(),
    updated_at         TIMESTAMP DEFAULT NOW(),
    related_tags_count INT       DEFAULT 0,
    posts_count        INT       DEFAULT 0
);

CREATE TABLE post_tag
(
    post_id UUID REFERENCES posts (post_id),
    tag_id  UUID REFERENCES tags (tag_id),
    PRIMARY KEY (post_id, tag_id)
);
