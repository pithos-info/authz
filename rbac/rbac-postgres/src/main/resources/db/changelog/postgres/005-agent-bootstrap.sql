-- liquibase formatted sql

-- changeset pithos:rbac-005 labels:rbac failOnError:true
-- Agent platform roles and permissions for enterprise 'pithos' (id='2').

INSERT INTO "role" (id, "enterpriseId", name, "utcCreatedAt", "utcModifiedAt")
VALUES ('60883ad5-e57e-47cc-a9d1-e73c29d77762', '2', 'agent.admin', now(), now());

INSERT INTO "role" (id, "enterpriseId", name, "utcCreatedAt", "utcModifiedAt")
VALUES ('7f8165ea-c278-495e-b423-b1fc58de0eef', '2', 'agent.dev', now(), now());

INSERT INTO "role" (id, "enterpriseId", name, "utcCreatedAt", "utcModifiedAt")
VALUES ('90ca6e61-2db4-4e1b-99e4-1dce1830d60e', '2', 'agent.execution', now(), now());

-- agent.admin: full read/write on definitions + all execution data
INSERT INTO "rolePermission" ("enterpriseId", "roleId", permission, "utcCreatedAt")
VALUES ('2', '60883ad5-e57e-47cc-a9d1-e73c29d77762', 'agent.definitions:write', now());
INSERT INTO "rolePermission" ("enterpriseId", "roleId", permission, "utcCreatedAt")
VALUES ('2', '60883ad5-e57e-47cc-a9d1-e73c29d77762', 'agent.definitions:read', now());
INSERT INTO "rolePermission" ("enterpriseId", "roleId", permission, "utcCreatedAt")
VALUES ('2', '60883ad5-e57e-47cc-a9d1-e73c29d77762', 'agent.execution:read', now());

-- agent.dev: same as admin
INSERT INTO "rolePermission" ("enterpriseId", "roleId", permission, "utcCreatedAt")
VALUES ('2', '7f8165ea-c278-495e-b423-b1fc58de0eef', 'agent.definitions:write', now());
INSERT INTO "rolePermission" ("enterpriseId", "roleId", permission, "utcCreatedAt")
VALUES ('2', '7f8165ea-c278-495e-b423-b1fc58de0eef', 'agent.definitions:read', now());
INSERT INTO "rolePermission" ("enterpriseId", "roleId", permission, "utcCreatedAt")
VALUES ('2', '7f8165ea-c278-495e-b423-b1fc58de0eef', 'agent.execution:read', now());

-- agent.execution: read-only on definitions + own execution data only
INSERT INTO "rolePermission" ("enterpriseId", "roleId", permission, "utcCreatedAt")
VALUES ('2', '90ca6e61-2db4-4e1b-99e4-1dce1830d60e', 'agent.definitions:read', now());
INSERT INTO "rolePermission" ("enterpriseId", "roleId", permission, "utcCreatedAt")
VALUES ('2', '90ca6e61-2db4-4e1b-99e4-1dce1830d60e', 'agent.execution:read.own', now());
