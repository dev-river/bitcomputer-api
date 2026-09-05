-- criminal_record/education_verified/employment_verified/credit_score are left NULL here:
-- those columns are AES-encrypted at the application layer (EncryptedStringTypeHandler), and
-- Flyway/SQL cannot produce valid ciphertext. BgcSeedDataInitializer backfills them through the
-- mapper at application startup, the same pattern EmployeeSeedDataInitializer uses for
-- employee.date_of_birth.
INSERT INTO background_check (employee_id, external_check_id, status, requested_at, completed_at, triggered_by_account_id)
SELECT e.id, 'CHK-seed-demo-0001', 'CLEAR',
       now() - INTERVAL '95' DAY, now() - INTERVAL '95' DAY,
       (SELECT id FROM account WHERE login_id = 'ADMIN-001')
FROM employee e WHERE e.emp_no = 'EMP-001';
