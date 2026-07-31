-- V1__init_schema.sql
-- Database initialization for MSME Lending Decision System

CREATE TABLE business_profiles (
    id UUID PRIMARY KEY,
    owner_name VARCHAR(120) NOT NULL,
    pan CHAR(10) NOT NULL,
    business_type VARCHAR(20) NOT NULL,
    monthly_revenue NUMERIC(14,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_monthly_revenue CHECK (monthly_revenue > 0)
);

CREATE TABLE loan_applications (
    id UUID PRIMARY KEY,
    business_profile_id UUID NOT NULL,
    loan_amount NUMERIC(14,2) NOT NULL,
    tenure_months INTEGER NOT NULL,
    purpose VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_business_profile FOREIGN KEY (business_profile_id) REFERENCES business_profiles(id) ON DELETE CASCADE,
    CONSTRAINT chk_loan_amount CHECK (loan_amount > 0),
    CONSTRAINT chk_tenure_months CHECK (tenure_months > 0)
);

CREATE TABLE decisions (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL UNIQUE,
    decision VARCHAR(10) NOT NULL,
    credit_score SMALLINT NOT NULL,
    reason_codes TEXT[] NOT NULL,
    score_breakdown JSONB NOT NULL,
    decided_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_loan_application FOREIGN KEY (application_id) REFERENCES loan_applications(id) ON DELETE CASCADE,
    CONSTRAINT chk_credit_score CHECK (credit_score BETWEEN 0 AND 100)
);
