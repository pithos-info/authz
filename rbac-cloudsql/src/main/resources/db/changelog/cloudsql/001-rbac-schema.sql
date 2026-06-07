-- liquibase formatted sql
-- Target: Cloud SQL (PostgreSQL-compatible)

-- changeset pithos:rbac-001 labels:rbac failOnError:true

CREATE TABLE "enterprise" (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug           TEXT UNIQUE NOT NULL,
    name           TEXT NOT NULL,
    plan           TEXT NOT NULL DEFAULT 'free',
    domain         TEXT,
    "utcCreatedAt" TIMESTAMPTZ NOT NULL DEFAULT now(),
    "deleted"      BOOLEAN DEFAULT FALSE,
    "utcModifiedAt" TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE "user" (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "enterpriseId" UUID NOT NULL REFERENCES enterprises(id),
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
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "enterpriseId" UUID NOT NULL REFERENCES enterprises(id),
    name           TEXT NOT NULL,
    "utcCreatedAt" TIMESTAMPTZ NOT NULL DEFAULT now(),
    "deleted"      BOOLEAN DEFAULT FALSE,
    "utcModifiedAt" TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE ("enterpriseId", name)
);

CREATE TABLE "groupMember" (
    "groupId"      UUID NOT NULL REFERENCES groups(id),
    "userId"       UUID NOT NULL REFERENCES users(id),
    "utcCreatedAt" TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY ("groupId", "userId")
);

CREATE TABLE role (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "enterpriseId" UUID NOT NULL REFERENCES enterprises(id),
    name           TEXT NOT NULL,
    "utcCreatedAt" TIMESTAMPTZ NOT NULL DEFAULT now(),
    "deleted"      BOOLEAN DEFAULT FALSE,
    "utcModifiedAt" TIMESTAMPTZ NOT NULL DEFAULT now(),
    "utcCreatedAt" TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE ("enterpriseId", name)
);

CREATE TABLE "userRole" (
    "userId"       UUID NOT NULL REFERENCES users(id),
    "roleId"       UUID NOT NULL REFERENCES roles(id),
    "grantedById"  UUID REFERENCES users(id),
    "utcCreatedAt" TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY ("userId", "roleId")
);

CREATE TABLE "groupRole" (
    "groupId"      UUID NOT NULL REFERENCES groups(id),
    "roleId"       UUID NOT NULL REFERENCES roles(id),
    "utcCreatedAt" TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY ("groupId", "roleId")
);

CREATE TABLE "rolePermission" (
    "roleId"       UUID NOT NULL REFERENCES roles(id),
    permission     TEXT NOT NULL,
    "utcCreatedAt" TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY ("roleId", permission)
);

CREATE TABLE "apiKey" (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "enterpriseId" UUID NOT NULL REFERENCES enterprises(id),
    "userId"       UUID NOT NULL REFERENCES users(id),
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
CREATE INDEX ON "userRole" ("roleId");
CREATE INDEX ON "groupRole" ("roleId");
CREATE INDEX ON "apiKey" ("userId");
CREATE INDEX ON "apiKey" ("enterpriseId");
