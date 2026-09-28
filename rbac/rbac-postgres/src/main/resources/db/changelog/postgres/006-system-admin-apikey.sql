-- liquibase formatted sql

-- changeset pithos:rbac-006 labels:rbac failOnError:true
-- API key for the system admin client (enterprise 1).
INSERT INTO "apiKey" (id, "enterpriseId", "userId", name, "keyHash", "keyPrefix", permissions, "utcCreatedTimestampMs")
VALUES (
  '42f62e1b-9c50-4392-b603-5f0982abb41e',
  '1',
  '1',
  'admin-client-key',
  'd79f227a557719e725fe35fd230f98f1fdbb86d5cc55538f2441660d450e5c77',
  'sys_59ae1c0e',
  '{}',
  now()
);
