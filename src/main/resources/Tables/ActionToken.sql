CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS access_tokens (
    uuid UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
        access_token VARCHAR(255) NOT NULL,
        refresh_token VARCHAR(255) NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
