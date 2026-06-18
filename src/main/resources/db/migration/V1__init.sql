CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(255),
    role VARCHAR(50) NOT NULL,
    custom_role_id BIGINT,
    suspension_reason VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    is_lead BOOLEAN DEFAULT FALSE,
    github_id VARCHAR(255) UNIQUE NOT NULL
);
