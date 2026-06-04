-- liquibase formatted sql

-- changeset pithos:rbac-001 labels:rbac failOnError:true

CREATE TABLE "enterprise" (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug           TEXT UNIQUE NOT NULL,
    name           TEXT NOT NULL,
    plan           TEXT NOT NULL DEFAULT 'free',
    domain         TEXT,
    "utcCreatedAt" TIMESTAMPTZ NOT NULL DEFAULT now(),
    "deleted"      BOOLEAN DEFAULT FALSE
);

CREATE TABLE "user" (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "enterpriseId" UUID NOT NULL REFERENCES enterprises(id) ON DELETE CASCADE,
    email          TEXT NOT NULL,
    "externalId"   TEXT NOT NULL,
    "idpProvider"  TEXT NOT NULL,
    "displayName"  TEXT,
    "lastLoginAt"  TIMESTAMPTZ,
    "utcCreatedAt" TIMESTAMPTZ NOT NULL DEFAULT now(),
    "deleted"      BOOLEAN DEFAULT FALSE,
    UNIQUE ("enterpriseId", "externalId")
);

CREATE TABLE "group" (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "enterpriseId" UUID NOT NULL REFERENCES enterprises(id) ON DELETE CASCADE,
    name           TEXT NOT NULL,
    "utcCreatedAt" TIMESTAMPTZ NOT NULL DEFAULT now(),
    "deleted"      BOOLEAN DEFAULT FALSE,
    UNIQUE ("enterpriseId", name)
);

CREATE TABLE "groupMember" (
    "groupId"      UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    "userId"       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY ("groupId", "userId")
);

CREATE TABLE role (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "enterpriseId" UUID NOT NULL REFERENCES enterprises(id) ON DELETE CASCADE,
    name           TEXT NOT NULL,
    "utcCreatedAt" TIMESTAMPTZ NOT NULL DEFAULT now(),
    "deleted"      BOOLEAN DEFAULT FALSE,
    UNIQUE ("enterpriseId", name)
);

CREATE TABLE "userRole" (
    "userId"       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    "roleId"       UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    "grantedBy"    UUID REFERENCES users(id),
    PRIMARY KEY ("userId", "roleId")
);

CREATE TABLE "groupRole" (
    "groupId"      UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    "roleId"       UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY ("groupId", "roleId")
);

CREATE TABLE "rolePermission" (
    "roleId"       UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission     TEXT NOT NULL,
    PRIMARY KEY ("roleId", permission)
);

CREATE TABLE "apiKey" (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "enterpriseId" UUID NOT NULL REFERENCES enterprises(id) ON DELETE CASCADE,
    "userId"       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
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
CREATE INDEX ON "groupMembers" ("userId");
CREATE INDEX ON "userRoles" ("roleId");
CREATE INDEX ON "groupRoles" ("roleId");
CREATE INDEX ON "apiKeys" ("userId");
CREATE INDEX ON "apiKeys" ("enterpriseId");
