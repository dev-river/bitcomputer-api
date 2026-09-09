-- ADMIN-001 시드 비밀번호를 ChangeMe123!에서 BitComputer123!로 변경.
-- EMP-001은 그대로 ChangeMe123! 유지(최초 로그인 시 변경 필수 플래그가 이미 있음).
UPDATE account
SET password_hash = '$2a$10$P/dQhN.Z50J4om0MUf5c0uOLmSD31nUeKDGnHbvF7j1Mx1sQBMDom'
WHERE login_id = 'ADMIN-001';
