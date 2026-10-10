UPDATE knowledge_base
SET visibility = 'PUBLIC'
WHERE created_by IN (
    SELECT id
    FROM sys_user
    WHERE role = 'ADMIN'
);
