/*Entity*/
CREATE TABLE users
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
CREATE TABLE article
(
    id           UUID PRIMARY KEY,
    code         UUID         NOT NULL,
    user_id      UUID REFERENCES users (id),
    title        VARCHAR(255) NOT NULL,
    body         TEXT         NOT NULL,
    published    BOOLEAN   DEFAULT FALSE,
    published_at TIMESTAMP,
    created_at   TIMESTAMP DEFAULT NOW(),
    updated_at   TIMESTAMP DEFAULT NOW()
);

/*Entity*/
CREATE TABLE category
(
    id          UUID PRIMARY KEY,
    code        UUID         NOT NULL,
    name        VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW()
);

/*Entity*/
CREATE TABLE comment
(
    id         UUID PRIMARY KEY,
    code       UUID NOT NULL,
    user_id    UUID REFERENCES users (id),
    article_id UUID REFERENCES article (id),
    text       TEXT NOT NULL,
    likes      INT       DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

/*Master*/
CREATE TABLE permission
(
    id          UUID PRIMARY KEY,
    code        UUID         NOT NULL,
    name        VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW()
);

CREATE TABLE user_permission
(
    user_id       UUID REFERENCES users (id),
    permission_id UUID REFERENCES permission (id),
    PRIMARY KEY (user_id, permission_id)
);

CREATE TABLE article_category
(
    article_id  UUID REFERENCES article (id),
    category_id UUID REFERENCES category (id),
    PRIMARY KEY (article_id, category_id)
);
