-- liquibase formatted sql
-- Target: Cloud SQL (PostgreSQL-compatible)

-- changeset pithos:rbac-003 labels:rbac failOnError:true
INSERT INTO "enterprise" (id, slug, name, domain, "utcCreatedAt", "utcModifiedAt")
VALUES ('2', 'pithos', 'pithos', 'pithos.info', now(), now());

INSERT INTO "group" (id, "enterpriseId", name, "utcCreatedAt", "utcModifiedAt")
VALUES ('61e3a4be-e377-4bd4-b56e-6ad583f881ea', '2', 'admin', now(), now());

INSERT INTO "role" (id, "enterpriseId", name, "utcCreatedAt", "utcModifiedAt")
VALUES ('7992fe54-1633-437b-80b7-594805b2ba7d', '2', 'admin', now(), now());

INSERT INTO "role" (id, "enterpriseId", name, "utcCreatedAt", "utcModifiedAt")
VALUES ('40f1960d-9ad3-4aea-af9e-0be5096b849f', '2', 'dev', now(), now());

INSERT INTO "user" (id, "enterpriseId", email, "externalId", "idpProvider", "displayName", "utcCreatedAt", "utcModifiedAt")
VALUES ('bf470037-fef1-4e57-91a1-b35abdda50ee', '2', 'shilpa@geekrox.com', 'pending-2:shilpa@geekrox.com', 'google', 'shilpa', now(), now());

INSERT INTO "userRole" ("enterpriseId", "userId", "roleId", "grantedById", "utcCreatedAt")
VALUES ('2', 'bf470037-fef1-4e57-91a1-b35abdda50ee', '7992fe54-1633-437b-80b7-594805b2ba7d', NULL, now());

-- Service account for headless / programmatic access
INSERT INTO "user" (id, "enterpriseId", email, "externalId", "idpProvider", "displayName", "utcCreatedAt", "utcModifiedAt")
VALUES ('8198d80e-ca57-4b19-bce6-b79538dfefc0', '2', 'svc-dev@pithos.info', 'service:svc-dev', 'service', 'pithos dev service account', now(), now());

INSERT INTO "userRole" ("enterpriseId", "userId", "roleId", "grantedById", "utcCreatedAt")
VALUES ('2', '8198d80e-ca57-4b19-bce6-b79538dfefc0', '40f1960d-9ad3-4aea-af9e-0be5096b849f', NULL, now());

-- raw key: pithos-dev-key-00000000000000000000
INSERT INTO "apiKey" (id, "enterpriseId", "userId", name, "keyHash", "keyPrefix", permissions, "utcCreatedAt")
VALUES (
  '6cedecb5-0430-4409-a8bd-5aa824145735',
  '2',
  '8198d80e-ca57-4b19-bce6-b79538dfefc0',
  'dev-service-key',
  '321e9ca228b106522ae7298aa78f15732e4fbb7e3281f5c5ccbe2c89ee86818f',
  'pithos-dev-k',
  '{}',
  now()
);
