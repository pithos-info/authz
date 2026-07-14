-- liquibase formatted sql
-- Target: Cloud SQL (PostgreSQL-compatible)

-- changeset pithos:rbac-002 labels:rbac failOnError:true
INSERT INTO "enterprise" (id, slug, name, domain, "utcCreatedTimestampMs", "utcTimestampMs")
VALUES ('1', 'system', 'system', 'geekrox.com', now(), now());

INSERT INTO "group" (id, "enterpriseId", name, "utcCreatedTimestampMs", "utcTimestampMs")
VALUES ('1', '1', 'admin', now(), now());

INSERT INTO "role" (id, "enterpriseId", name, "utcCreatedTimestampMs", "utcTimestampMs")
VALUES ('1', '1', 'admin', now(), now());

INSERT INTO "role" (id, "enterpriseId", name, "utcCreatedTimestampMs", "utcTimestampMs")
VALUES ('2', '1', 'dev', now(), now());

INSERT INTO "user" (id, "enterpriseId", email, "externalId", "idpProvider", "displayName", "utcCreatedTimestampMs", "utcTimestampMs")
VALUES ('1', '1', 'shilpa@geekrox.com', 'pending:shilpa@geekrox.com', 'google', 'shilpa', now(), now());

INSERT INTO "groupMember" ("enterpriseId", "groupId", "userId", "utcCreatedTimestampMs")
VALUES ('1', '1', '1', now());

INSERT INTO "userRole" ("enterpriseId", "userId", "roleId", "grantedById", "utcCreatedTimestampMs")
VALUES ('1', '1', '1', NULL, now());

INSERT INTO "userRole" ("enterpriseId", "userId", "roleId", "grantedById", "utcCreatedTimestampMs")
VALUES ('1', '1', '2', NULL, now());
