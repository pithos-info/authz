-- liquibase formatted sql

-- changeset pithos:rbac-007 labels:rbac failOnError:true
INSERT INTO "enterprise" (id, slug, name, domain, "utcCreatedTimestampMs", "utcTimestampMs")
VALUES ('3', 'kestrel', 'kestrel', 'kestrel.app', now(), now());

INSERT INTO "group" (id, "enterpriseId", name, "utcCreatedTimestampMs", "utcTimestampMs")
VALUES ('9577fb88-343b-4eeb-b0bf-4cc58db68ae0', '3', 'admin', now(), now());

INSERT INTO "role" (id, "enterpriseId", name, "utcCreatedTimestampMs", "utcTimestampMs")
VALUES ('37dc9460-568b-49cf-86c8-a66a1f2cf365', '3', 'admin', now(), now());

INSERT INTO "role" (id, "enterpriseId", name, "utcCreatedTimestampMs", "utcTimestampMs")
VALUES ('1f9a0b2a-fcd0-48fd-9eea-4376251ae8db', '3', 'dev', now(), now());

INSERT INTO "user" (id, "enterpriseId", email, "externalId", "idpProvider", "displayName", "utcCreatedTimestampMs", "utcTimestampMs")
VALUES ('205a3ce0-7026-4bf2-bcaf-3c95af9c005a', '3', 'evergetiki', 'pending:evergetiki.agora@gmail.com', 'google', 'evergetiki', now(), now());

INSERT INTO "userRole" ("enterpriseId", "userId", "roleId", "grantedById", "utcCreatedTimestampMs")
VALUES ('3', '205a3ce0-7026-4bf2-bcaf-3c95af9c005a', '37dc9460-568b-49cf-86c8-a66a1f2cf365', NULL, now());

-- Service account for headless / programmatic access
INSERT INTO "user" (id, "enterpriseId", email, "externalId", "idpProvider", "displayName", "utcCreatedTimestampMs", "utcTimestampMs")
VALUES ('15eb8c74-4484-410b-a8a3-0cf81fe40912', '3', 'svc-dev@kestrel.app', 'service:svc-dev', 'service', 'kestrel dev service account', now(), now());

INSERT INTO "userRole" ("enterpriseId", "userId", "roleId", "grantedById", "utcCreatedTimestampMs")
VALUES ('3', '15eb8c74-4484-410b-a8a3-0cf81fe40912', '1f9a0b2a-fcd0-48fd-9eea-4376251ae8db', NULL, now());

INSERT INTO "apiKey" (id, "enterpriseId", "userId", name, "keyHash", "keyPrefix", permissions, "utcCreatedTimestampMs")
VALUES (
  'ec3f2eba-074b-4d3c-839b-9e2ac0209e84',
  '3',
  '15eb8c74-4484-410b-a8a3-0cf81fe40912',
  'dev-service-key',
  '54728c6ba843573640392408ae31ac41f44394156a0080cd6bc11ea828885fa2',
  'kst_0d97b506',
  '{}',
  now()
);
