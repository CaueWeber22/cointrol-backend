INSERT INTO access.roles (id, name, description)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'ROLE_USER', 'Default application user'),
    ('00000000-0000-0000-0000-000000000002', 'ROLE_ADMIN', 'Application administrator')
ON CONFLICT (name) DO NOTHING;
