CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TYPE level_of_detail_enum AS ENUM ('ORIGINAL', 'LOW', 'MEDIUM', 'HIGH');

CREATE TYPE file_status_enum AS ENUM ('AVAILABLE', 'PENDING', 'DELETED', 'FAILED');

CREATE TABLE files (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    file_name VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL CHECK (file_size >= 0),

    bucket_name VARCHAR(255) NOT NULL,
    object_name VARCHAR(1024) NOT NULL,

    status file_status_enum NOT NULL DEFAULT 'PENDING',
    owner_keycloak_id VARCHAR(255) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_file_object UNIQUE (bucket_name, object_name)
);

CREATE TABLE image (
    file_id UUID PRIMARY KEY,
    level_of_detail level_of_detail_enum NOT NULL,

    FOREIGN KEY (file_id) REFERENCES files (id) ON DELETE CASCADE
);

CREATE TABLE tag (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    tag_name VARCHAR(255) NOT NULL,
    owner_keycloak_id VARCHAR(255) NULL, -- NULL = system tag

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_tag_name_owner UNIQUE (tag_name, owner_keycloak_id)
);

CREATE TABLE tag_category (
    tag_id UUID NOT NULL,
    content_category VARCHAR(255) NOT NULL, -- 'IMAGE', 'FILE'

    PRIMARY KEY (tag_id, content_category),
    FOREIGN KEY (tag_id) REFERENCES tag (id) ON DELETE CASCADE
);

CREATE TABLE file_tag (
    file_id UUID NOT NULL,
    tag_id UUID NOT NULL,
    PRIMARY KEY (file_id, tag_id),
    FOREIGN KEY (file_id) REFERENCES files (id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tag (id) ON DELETE CASCADE
);

CREATE TABLE file_access (
    file_id UUID NOT NULL,
    keycloak_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (file_id, keycloak_id),
    FOREIGN KEY (file_id) REFERENCES files (id) ON DELETE CASCADE
);

CREATE TABLE property_file (
    property_id BIGINT NOT NULL,
    file_id UUID NOT NULL,
    PRIMARY KEY (property_id, file_id),
    FOREIGN KEY (file_id) REFERENCES files (id) ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX idx_file_owner ON files(owner_keycloak_id);
CREATE INDEX idx_file_status ON files(status);

CREATE INDEX idx_tag_name ON tag(tag_name);
CREATE INDEX idx_tag_owner ON tag(owner_keycloak_id);
CREATE INDEX idx_tag_category_content_category ON tag_category(content_category);

CREATE INDEX idx_file_tag_file ON file_tag(file_id);
CREATE INDEX idx_file_tag_tag ON file_tag(tag_id);

INSERT INTO tag (tag_name, owner_keycloak_id) VALUES
    ('Thumbnail', NULL),
    ('Outdoor', NULL),
    ('Indoor', NULL),
    ('Kitchen', NULL),
    ('Living room', NULL),
    ('Bedroom', NULL),
    ('Bathroom', NULL);

INSERT INTO tag_category (tag_id, content_category)
SELECT id, 'IMAGE'
FROM tag
WHERE tag_name IN (
  'Thumbnail','Outdoor','Indoor','Kitchen',
  'Living room','Bedroom','Bathroom'
);