INSERT INTO background_check (employee_id, external_check_id, status, criminal_record, education_verified,
                               employment_verified, credit_score, requested_at, completed_at, triggered_by_account_id)
SELECT e.id, 'CHK-seed-demo-0001', 'CLEAR', 'false', 'true', 'true', 'good',
       now() - INTERVAL '95' DAY, now() - INTERVAL '95' DAY,
       (SELECT id FROM account WHERE login_id = 'ADMIN-001')
FROM employee e WHERE e.emp_no = 'EMP-001';
