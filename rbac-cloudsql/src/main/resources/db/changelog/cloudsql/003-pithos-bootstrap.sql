-- liquibase formatted sql
-- Target: Cloud SQL (PostgreSQL-compatible)

-- changeset pithos:rbac-003 labels:rbac failOnError:true
INSERT INTO "enterprise" (id, slug, name, domain, "utcCreatedAt", "utcModifiedAt")
VALUES ('2', 'pithos', 'pithos', 'pithos.info', now(), now());

INSERT INTO "group" (id, "enterpriseId", name, "utcCreatedAt", "utcModifiedAt")
VALUES ('2', '2', 'admin', now(), now());

INSERT INTO "role" (id, "enterpriseId", name, "utcCreatedAt", "utcModifiedAt")
VALUES ('2', '2', 'admin', now(), now());

INSERT INTO "user" (id, "enterpriseId", email, "externalId", "idpProvider", "displayName", "utcCreatedAt", "utcModifiedAt")
VALUES ('2', '2', 'shilpa@geekrox.com', 'pending-2:shilpa@geekrox.com', 'google', 'shilpa', now(), now());
