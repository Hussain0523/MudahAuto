CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS car_details (
    uuid UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    owner_name VARCHAR(255) NOT NULL,
    owner_id VARCHAR(255) NOT NULL,
    owner_email VARCHAR(255) NOT NULL,
    extended_warranty_provider VARCHAR(255),
    currency_symbol VARCHAR(10),
    full_variant VARCHAR(255),
    name VARCHAR(255),
    last_activity_time TIMESTAMP,
    extended_warranty_program_yes_no BOOLEAN,
    car_id VARCHAR(255) NOT NULL,
    availability VARCHAR(255),
    variant VARCHAR(255),
    current_location VARCHAR(255),
    total_refurbishment_cost DECIMAL(19, 2),
    purchaser_hub VARCHAR(255),
    tcec_handling_fee DECIMAL(19, 2),
    photo_url VARCHAR(255),
    mileage_km INT,
    year_of_ewp INT,
    car_brand_car_make VARCHAR(255),
    colour VARCHAR(255),
    car_model VARCHAR(255),
    year_make INT,
    listing_price DECIMAL(19, 2),
    approval_state VARCHAR(255),
    total_price DECIMAL(19, 2),
    active BOOLEAN,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    modified_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS car_image (
    uuid UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    car_id VARCHAR(255) NOT NULL,
    car_name VARCHAR(255) NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    modified_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by VARCHAR(255)
);