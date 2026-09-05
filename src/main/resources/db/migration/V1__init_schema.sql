CREATE TABLE employee (
    id             SERIAL PRIMARY KEY,
    emp_no         VARCHAR(20) NOT NULL UNIQUE,
    name           VARCHAR(50) NOT NULL,
    department     VARCHAR(50),
    position       VARCHAR(50),
    hire_date      DATE,
    email          VARCHAR(100),
    date_of_birth  VARCHAR(200),
    phone          VARCHAR(200),
    address        VARCHAR(500),
    status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    terminated_at  TIMESTAMP
);

CREATE TABLE account (
    id                      SERIAL PRIMARY KEY,
    login_id                VARCHAR(50) NOT NULL UNIQUE,
    password_hash           VARCHAR(200) NOT NULL,
    role                    VARCHAR(20) NOT NULL,
    employee_id             INT REFERENCES employee(id),
    must_change_password    BOOLEAN NOT NULL DEFAULT true,
    created_by_account_id   INT REFERENCES account(id),
    created_at              TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE background_check (
    id                       SERIAL PRIMARY KEY,
    employee_id              INT NOT NULL REFERENCES employee(id),
    external_check_id        VARCHAR(100),
    status                   VARCHAR(20) NOT NULL,
    criminal_record          VARCHAR(200),
    education_verified       VARCHAR(200),
    employment_verified      VARCHAR(200),
    credit_score             VARCHAR(200),
    error_message            VARCHAR(500),
    retry_count              INT NOT NULL DEFAULT 0,
    last_error_at            TIMESTAMP,
    requested_at             TIMESTAMP NOT NULL DEFAULT now(),
    completed_at             TIMESTAMP,
    masked_at                TIMESTAMP,
    triggered_by_account_id  INT NOT NULL REFERENCES account(id)
);

CREATE TABLE employee_change_log (
    id            SERIAL PRIMARY KEY,
    employee_id   INT NOT NULL REFERENCES employee(id),
    field_name    VARCHAR(50) NOT NULL,
    old_value     VARCHAR(500),
    new_value     VARCHAR(500),
    changed_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_background_check_employee_id ON background_check(employee_id);
CREATE INDEX idx_background_check_status ON background_check(status);
CREATE INDEX idx_employee_change_log_employee_id ON employee_change_log(employee_id);
