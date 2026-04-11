-- ============================================
-- Employee Management System - Initial Schema
-- Flyway Migration Script V1
-- ============================================

-- Create admins table
CREATE TABLE IF NOT EXISTS admins (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    department VARCHAR(255),
    role VARCHAR(50),
    permissions TEXT,
    phone VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    parent_admin_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_admins_parent_admin FOREIGN KEY (parent_admin_id) REFERENCES admins(id)
);

-- Create employees table
CREATE TABLE IF NOT EXISTS employees (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(255) UNIQUE,
    password VARCHAR(255),
    phone VARCHAR(255) NOT NULL UNIQUE,
    department VARCHAR(255),
    position VARCHAR(255),
    state VARCHAR(255) NOT NULL,
    city VARCHAR(255) NOT NULL,
    area VARCHAR(255) NOT NULL,
    hierarchy_level VARCHAR(50) NOT NULL,
    reporting_manager_id BIGINT,
    hire_date DATE NOT NULL,
    salary DOUBLE PRECISION,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_by VARCHAR(255),
    last_updated_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_employees_reporting_manager FOREIGN KEY (reporting_manager_id) REFERENCES employees(id)
);

-- Create customers table
CREATE TABLE IF NOT EXISTS customers (
    id BIGSERIAL PRIMARY KEY,
    contact_person VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(255) NOT NULL,
    address VARCHAR(500),
    area VARCHAR(100),
    city VARCHAR(100),
    state VARCHAR(100),
    zip_code VARCHAR(20),
    country VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    notes VARCHAR(1000),
    promoter_id BIGINT,
    zonal_head_id BIGINT,
    cluster_head_id BIGINT,
    area_sales_manager_id BIGINT,
    created_by VARCHAR(255),
    last_updated_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create products table
CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    product_id VARCHAR(10) UNIQUE,
    name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(1000),
    unit_price NUMERIC(10,2) NOT NULL,
    commission_type VARCHAR(50) NOT NULL,
    commission_value NUMERIC(10,2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_by VARCHAR(255),
    last_updated_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create sales table
CREATE TABLE IF NOT EXISTS sales (
    id BIGSERIAL PRIMARY KEY,
    sale_id VARCHAR(10) UNIQUE,
    created_by_id BIGINT NOT NULL,
    promoter_id BIGINT,
    zonal_head_id BIGINT,
    cluster_head_id BIGINT,
    asm_id BIGINT,
    customer_id BIGINT NOT NULL,
    sale_date DATE NOT NULL,
    product_id BIGINT,
    product_name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    due_date DATE,
    quantity INTEGER NOT NULL DEFAULT 1,
    unit_price NUMERIC(10,2) NOT NULL,
    total_amount NUMERIC(10,2) NOT NULL,
    payment_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    status VARCHAR(50) NOT NULL DEFAULT 'IN_PROGRESS',
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sales_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_sales_product FOREIGN KEY (product_id) REFERENCES products(id)
);

-- Create payments table
CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    sale_id BIGINT NOT NULL,
    amount NUMERIC(10,2) NOT NULL,
    payment_date TIMESTAMP NOT NULL,
    transaction_mode VARCHAR(50) NOT NULL,
    transaction_id VARCHAR(100),
    notes VARCHAR(500),
    invoice_number VARCHAR(20) UNIQUE,
    settled BOOLEAN NOT NULL DEFAULT FALSE,
    settled_by_admin BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payments_sale FOREIGN KEY (sale_id) REFERENCES sales(id),
    CONSTRAINT fk_payments_settled_by FOREIGN KEY (settled_by_admin) REFERENCES admins(id)
);

-- Create sales_targets table
CREATE TABLE IF NOT EXISTS sales_targets (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    product_id BIGINT,
    target_units INTEGER NOT NULL,
    target_amount NUMERIC(12,2) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    achieved_amount NUMERIC(12,2) DEFAULT 0,
    base_salary NUMERIC(12,2),
    commission_rate NUMERIC(5,2) DEFAULT 0,
    commission_amount NUMERIC(12,2) DEFAULT 0,
    calculated_salary NUMERIC(12,2),
    salary_calculated BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sales_targets_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_sales_targets_product FOREIGN KEY (product_id) REFERENCES products(id)
);

-- Create attendances table
CREATE TABLE IF NOT EXISTS attendances (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    date DATE NOT NULL,
    check_in TIMESTAMP NOT NULL,
    check_out TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'PRESENT',
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_attendances_employee FOREIGN KEY (employee_id) REFERENCES employees(id)
);

-- Create salaries table
CREATE TABLE IF NOT EXISTS salaries (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    released_by BIGINT,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    salary_month DATE NOT NULL,
    basic_salary NUMERIC(10,2) NOT NULL,
    allowances NUMERIC(10,2),
    deductions NUMERIC(10,2),
    bonus NUMERIC(10,2),
    overtime NUMERIC(10,2),
    net_salary NUMERIC(10,2) NOT NULL,
    payslip_path VARCHAR(500),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    transaction_reference VARCHAR(100),
    transaction_type VARCHAR(20),
    paid_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_salaries_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_salaries_released_by FOREIGN KEY (released_by) REFERENCES admins(id)
);

-- Create commissions table
CREATE TABLE IF NOT EXISTS commissions (
    id BIGSERIAL PRIMARY KEY,
    sale_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    amount NUMERIC(10,2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_APPROVAL',
    approved_by BIGINT,
    approved_at TIMESTAMP,
    is_paid BOOLEAN DEFAULT FALSE,
    paid_by_id BIGINT,
    paid_at TIMESTAMP,
    payment_method VARCHAR(20),
    transaction_reference VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_commissions_sale FOREIGN KEY (sale_id) REFERENCES sales(id),
    CONSTRAINT fk_commissions_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_commissions_approved_by FOREIGN KEY (approved_by) REFERENCES employees(id),
    CONSTRAINT fk_commissions_paid_by FOREIGN KEY (paid_by_id) REFERENCES admins(id)
);

-- Create claims table
CREATE TABLE IF NOT EXISTS claims (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    assigned_to_id BIGINT,
    claim_type VARCHAR(50) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    amount NUMERIC(10,2) NOT NULL,
    claim_date DATE NOT NULL,
    bill_attachment VARCHAR(500),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    remarks VARCHAR(500),
    approved_by_id BIGINT,
    approved_at TIMESTAMP,
    is_paid BOOLEAN DEFAULT FALSE,
    paid_by_id BIGINT,
    paid_at TIMESTAMP,
    payment_method VARCHAR(20),
    transaction_reference VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_claims_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_claims_assigned_to FOREIGN KEY (assigned_to_id) REFERENCES employees(id),
    CONSTRAINT fk_claims_approved_by FOREIGN KEY (approved_by_id) REFERENCES employees(id),
    CONSTRAINT fk_claims_paid_by FOREIGN KEY (paid_by_id) REFERENCES admins(id)
);

-- Create leave_requests table
CREATE TABLE IF NOT EXISTS leave_requests (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    assigned_to_id BIGINT,
    leave_type VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    number_of_days INTEGER,
    reason VARCHAR(1000) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    approved_by_id BIGINT,
    remarks VARCHAR(500),
    approved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_leave_requests_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_leave_requests_assigned_to FOREIGN KEY (assigned_to_id) REFERENCES employees(id),
    CONSTRAINT fk_leave_requests_approved_by FOREIGN KEY (approved_by_id) REFERENCES employees(id)
);

-- Create bank_details table
CREATE TABLE IF NOT EXISTS bank_details (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    account_holder_name VARCHAR(255) NOT NULL,
    account_number VARCHAR(255) NOT NULL,
    ifsc_code VARCHAR(255) NOT NULL,
    bank_name VARCHAR(255) NOT NULL,
    branch_name VARCHAR(255),
    account_type VARCHAR(50),
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bank_details_employee FOREIGN KEY (employee_id) REFERENCES employees(id)
);

-- Create tasks table
CREATE TABLE IF NOT EXISTS tasks (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    priority VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    due_date DATE,
    completed_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tasks_employee FOREIGN KEY (employee_id) REFERENCES employees(id)
);

-- Create leads table
CREATE TABLE IF NOT EXISTS leads (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    mobile VARCHAR(255) NOT NULL,
    address VARCHAR(1000) NOT NULL,
    district VARCHAR(255) NOT NULL,
    interested_in VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    notes VARCHAR(2000),
    assigned_to BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_leads_assigned_to FOREIGN KEY (assigned_to) REFERENCES employees(id)
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_employees_reporting_manager ON employees(reporting_manager_id);
CREATE INDEX IF NOT EXISTS idx_employees_hierarchy_level ON employees(hierarchy_level);
CREATE INDEX IF NOT EXISTS idx_employees_status ON employees(status);
CREATE INDEX IF NOT EXISTS idx_sales_customer ON sales(customer_id);
CREATE INDEX IF NOT EXISTS idx_sales_created_by ON sales(created_by_id);
CREATE INDEX IF NOT EXISTS idx_sales_sale_date ON sales(sale_date);
CREATE INDEX IF NOT EXISTS idx_payments_sale ON payments(sale_id);
CREATE INDEX IF NOT EXISTS idx_sales_targets_employee ON sales_targets(employee_id);
CREATE INDEX IF NOT EXISTS idx_sales_targets_product ON sales_targets(product_id);
CREATE INDEX IF NOT EXISTS idx_attendances_employee ON attendances(employee_id);
CREATE INDEX IF NOT EXISTS idx_attendances_date ON attendances(date);
CREATE INDEX IF NOT EXISTS idx_salaries_employee ON salaries(employee_id);
CREATE INDEX IF NOT EXISTS idx_salaries_salary_month ON salaries(salary_month);
CREATE INDEX IF NOT EXISTS idx_commissions_employee ON commissions(employee_id);
CREATE INDEX IF NOT EXISTS idx_commissions_sale ON commissions(sale_id);
CREATE INDEX IF NOT EXISTS idx_commissions_status ON commissions(status);
CREATE INDEX IF NOT EXISTS idx_claims_employee ON claims(employee_id);
CREATE INDEX IF NOT EXISTS idx_claims_assigned_to ON claims(assigned_to_id);
CREATE INDEX IF NOT EXISTS idx_claims_status ON claims(status);
CREATE INDEX IF NOT EXISTS idx_leave_requests_employee ON leave_requests(employee_id);
CREATE INDEX IF NOT EXISTS idx_leave_requests_assigned_to ON leave_requests(assigned_to_id);
CREATE INDEX IF NOT EXISTS idx_leave_requests_status ON leave_requests(status);
CREATE INDEX IF NOT EXISTS idx_bank_details_employee ON bank_details(employee_id);
CREATE INDEX IF NOT EXISTS idx_tasks_employee ON tasks(employee_id);
CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(status);
CREATE INDEX IF NOT EXISTS idx_leads_assigned_to ON leads(assigned_to);
CREATE INDEX IF NOT EXISTS idx_leads_status ON leads(status);
