-- liquibase formatted sql

-- changeset pithos:rbac-001 labels:rbac failOnError:true

CREATE TABLE "enterprise" (
    id             TEXT PRIMARY KEY,
    slug           TEXT UNIQUE NOT NULL,
    name           TEXT NOT NULL,
    domain         TEXT,
    "utcCreatedAt" TIMESTAMPTZ NOT NULL DEFAULT now(),
    "deleted"      BOOLEAN DEFAULT FALSE,
    "utcModifiedAt" TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE "user" (
    id             TEXT PRIMARY KEY,
    "enterpriseId" TEXT NOT NULL REFERENCES "enterprise"(id),
    email          TEXT NOT NULL,
    "externalId"   TEXT NOT NULL,
    "idpProvider"  TEXT NOT NULL,
    "displayName"  TEXT,
    "lastLoginAt"  TIMESTAMPTZ,
    "utcCreatedAt" TIMESTAMPTZ NOT NULL DEFAULT now(),
    "deleted"      BOOLEAN DEFAULT FALSE,
    "utcModifiedAt" TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE ("enterpriseId", "externalId")
);

CREATE TABLE "group" (
    id             TEXT PRIMARY KEY,
    "enterpriseId" TEXT NOT NULL REFERENCES "enterprise"(id),
    name           TEXT NOT NULL,
    "utcCreatedAt" TIMESTAMPTZ NOT NULL DEFAULT now(),
    "deleted"      BOOLEAN DEFAULT FALSE,
    "utcModifiedAt" TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE ("enterpriseId", name)
);

CREATE TABLE "groupMember" (
    "enterpriseId" TEXT NOT NULL REFERENCES "enterprise"(id),
    "groupId"      TEXT NOT NULL REFERENCES "group"(id),
    "userId"       TEXT NOT NULL REFERENCES "user"(id),
    "utcCreatedAt" TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY ("enterpriseId", "groupId", "userId")
);

CREATE TABLE "role" (
    id             TEXT PRIMARY KEY,
    "enterpriseId" TEXT NOT NULL REFERENCES "enterprise"(id),
    name           TEXT NOT NULL,
    "utcCreatedAt" TIMESTAMPTZ NOT NULL DEFAULT now(),
    "deleted"      BOOLEAN DEFAULT FALSE,
    "utcModifiedAt" TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE ("enterpriseId", name)
);

CREATE TABLE "userRole" (
    "enterpriseId" TEXT NOT NULL REFERENCES "enterprise"(id),
    "userId"       TEXT NOT NULL REFERENCES "user"(id),
    "roleId"       TEXT NOT NULL REFERENCES "role"(id),
    "grantedById"  TEXT REFERENCES "user"(id),
    "utcCreatedAt" TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY ("enterpriseId", "userId", "roleId")
);

CREATE TABLE "groupRole" (
    "enterpriseId" TEXT NOT NULL REFERENCES "enterprise"(id),
    "groupId"      TEXT NOT NULL REFERENCES "group"(id),
    "roleId"       TEXT NOT NULL REFERENCES "role"(id),
    "utcCreatedAt" TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY ("enterpriseId", "groupId", "roleId")
);

CREATE TABLE "rolePermission" (
    "enterpriseId" TEXT NOT NULL REFERENCES "enterprise"(id),
    "roleId"       TEXT NOT NULL REFERENCES "role"(id),
    permission     TEXT NOT NULL,
    "utcCreatedAt" TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY ("enterpriseId", "roleId", permission)
);

CREATE TABLE "apiKey" (
    id             TEXT PRIMARY KEY,
    "enterpriseId" TEXT NOT NULL REFERENCES "enterprise"(id),
    "userId"       TEXT NOT NULL REFERENCES "user"(id),
    name           TEXT NOT NULL,
    "keyHash"      TEXT UNIQUE NOT NULL,
    "keyPrefix"    TEXT NOT NULL,
    permissions    TEXT[] NOT NULL DEFAULT '{}',
    "expiresAt"    TIMESTAMPTZ,
    "lastUsedAt"   TIMESTAMPTZ,
    "utcCreatedAt" TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- indexes for common query patterns
CREATE INDEX ON "user" ("enterpriseId");
CREATE INDEX ON "group" ("enterpriseId");
CREATE INDEX ON "role" ("enterpriseId");
CREATE INDEX ON "groupMember" ("userId");
CREATE INDEX ON "groupMember" ("groupId");
CREATE INDEX ON "userRole" ("userId");
CREATE INDEX ON "userRole" ("roleId");
CREATE INDEX ON "groupRole" ("roleId");
CREATE INDEX ON "groupRole" ("groupId");
CREATE INDEX ON "rolePermission" ("roleId");
CREATE INDEX ON "apiKey" ("userId");
CREATE INDEX ON "apiKey" ("enterpriseId");
