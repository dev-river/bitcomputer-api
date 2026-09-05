INSERT INTO account (login_id, password_hash, role, employee_id, must_change_password, created_by_account_id)
VALUES ('ADMIN-001', '$2a$10$ZRoN8DGY8XiDIxdbqYogK.vc7gq3R4rhVBBCCnU/Jj9ya2wybtBQq', 'ADMIN', NULL, false, NULL);

INSERT INTO account (login_id, password_hash, role, employee_id, must_change_password, created_by_account_id)
SELECT 'EMP-001', '$2a$10$ZRoN8DGY8XiDIxdbqYogK.vc7gq3R4rhVBBCCnU/Jj9ya2wybtBQq', 'EMPLOYEE', id, true,
       (SELECT id FROM account WHERE login_id = 'ADMIN-001')
FROM employee WHERE emp_no = 'EMP-001';
