INSERT INTO account (login_id, password_hash, role, employee_id, must_change_password, created_by_account_id)
VALUES ('ADMIN-001', '$2a$10$7EqJtq98hPqEX7fNZaFWoOa2ktY5DHWFT7ovfj/PFtcpvQ0/UZE1e', 'ADMIN', NULL, false, NULL);

INSERT INTO account (login_id, password_hash, role, employee_id, must_change_password, created_by_account_id)
SELECT 'EMP-001', '$2a$10$7EqJtq98hPqEX7fNZaFWoOa2ktY5DHWFT7ovfj/PFtcpvQ0/UZE1e', 'EMPLOYEE', id, true,
       (SELECT id FROM account WHERE login_id = 'ADMIN-001')
FROM employee WHERE emp_no = 'EMP-001';
