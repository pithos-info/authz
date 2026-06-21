-- liquibase formatted sql

-- changeset pithos:rbac-004 labels:rbac failOnError:true

CREATE TABLE "account" (
    id              TEXT PRIMARY KEY,
    name            TEXT NOT NULL,
    type            TEXT NOT NULL,                            -- PERSONAL | TEAM
    "ownerId"       TEXT REFERENCES "user"(id),              -- set when type=PERSONAL
    "enterpriseId"  TEXT REFERENCES "enterprise"(id),        -- set when type=TEAM
    "utcCreatedAt"  TIMESTAMPTZ NOT NULL DEFAULT now(),
    "utcModifiedAt" TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted         BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT "account_type_check" CHECK (
        (type = 'PERSONAL' AND "ownerId" IS NOT NULL AND "enterpriseId" IS NULL) OR
        (type = 'TEAM'     AND "enterpriseId" IS NOT NULL AND "ownerId" IS NULL)
    )
);

CREATE TABLE "accountUser" (
    "accountId"     TEXT NOT NULL REFERENCES "account"(id),
    "userId"        TEXT NOT NULL REFERENCES "user"(id),
    role            TEXT NOT NULL DEFAULT 'MEMBER',           -- OWNER | MEMBER
    "utcCreatedAt"  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY ("accountId", "userId")
);

CREATE INDEX ON "account" ("ownerId");
CREATE INDEX ON "account" ("enterpriseId");
CREATE INDEX ON "accountUser" ("userId");
