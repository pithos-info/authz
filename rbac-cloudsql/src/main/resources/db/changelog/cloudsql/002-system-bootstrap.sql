-- liquibase formatted sql
-- Target: Cloud SQL (PostgreSQL-compatible)

-- changeset pithos:rbac-002 labels:rbac failOnError:true
INSERT INTO "enterprise" (id, slug, name, domain, "utcCreatedAt", "utcModifiedAt")
VALUES ('1', 'system', 'system', 'geekrox.com', now(), now());

INSERT INTO "group" (id, "enterpriseId", name, "utcCreatedAt", "utcModifiedAt")
VALUES ('1', '1', 'admin', now(), now());

INSERT INTO "role" (id, "enterpriseId", name, "utcCreatedAt", "utcModifiedAt")
VALUES ('1', '1', 'admin', now(), now());

INSERT INTO "role" (id, "enterpriseId", name, "utcCreatedAt", "utcModifiedAt")
VALUES ('2', '1', 'dev', now(), now());

INSERT INTO "user" (id, "enterpriseId", email, "externalId", "idpProvider", "displayName", "utcCreatedAt", "utcModifiedAt")
VALUES ('1', '1', 'shilpa@geekrox.com', 'pending:shilpa@geekrox.com', 'google', 'shilpa', now(), now());

INSERT INTO "groupMember" ("enterpriseId", "groupId", "userId", "utcCreatedAt")
VALUES ('1', '1', '1', now());

INSERT INTO "userRole" ("enterpriseId", "userId", "roleId", "grantedById", "utcCreatedAt")
VALUES ('1', '1', '1', NULL, now());

INSERT INTO "userRole" ("enterpriseId", "userId", "roleId", "grantedById", "utcCreatedAt")
VALUES ('1', '1', '2', NULL, now());
