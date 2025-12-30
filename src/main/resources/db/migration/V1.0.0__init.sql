CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TYPE file_status_enum AS ENUM ('PENDING', 'AVAILABLE', 'DELETED', 'FAILED');
CREATE TYPE file_type_enum AS ENUM ('FILE', 'IMAGE');
CREATE TYPE level_of_detail_enum AS ENUM ('LOW', 'MEDIUM', 'HIGH');
CREATE TYPE access_level_enum AS ENUM ('READ', 'WRITE', 'OWNER');

CREATE TABLE files (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    file_name VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL CHECK (file_size >= 0),

    bucket_name VARCHAR(255) NOT NULL,
    object_name VARCHAR(1024) NOT NULL,

    file_type file_type_enum NOT NULL,
    status file_status_enum NOT NULL DEFAULT 'PENDING',
    owner_keycloak_id VARCHAR(255) NOT NULL,

    created TIMESTAMPTZ NOT NULL DEFAULT now(),
    modified TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_file_object UNIQUE (bucket_name, object_name)
);

CREATE TABLE image_level_of_detail (
    file_id UUID NOT NULL,
    level_of_detail level_of_detail_enum NOT NULL,

    object_name VARCHAR(1024) NOT NULL,
    file_size BIGINT NOT NULL CHECK (file_size >= 0),

    created TIMESTAMPTZ NOT NULL DEFAULT now(),

    PRIMARY KEY (file_id, level_of_detail),
    FOREIGN KEY (file_id) REFERENCES files (id) ON DELETE CASCADE,

    CONSTRAINT uq_lod_object UNIQUE (object_name)
);

CREATE TABLE tag (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    tag_name VARCHAR(255) NOT NULL,
    owner_keycloak_id VARCHAR(255) NULL, -- NULL = system tag
    file_type file_type_enum NOT NULL,   -- IMAGE or FILE

    created TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_tag_name_owner_file_type UNIQUE (tag_name, owner_keycloak_id, file_type)
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
    access_level access_level_enum NOT NULL,
    created TIMESTAMPTZ NOT NULL DEFAULT now(),

    PRIMARY KEY (file_id, keycloak_id),
    FOREIGN KEY (file_id) REFERENCES files (id) ON DELETE CASCADE
);

CREATE TABLE property_file (
    property_id BIGINT NOT NULL,
    file_id UUID NOT NULL,

    created TIMESTAMPTZ NOT NULL DEFAULT now(),

    PRIMARY KEY (property_id, file_id),
    FOREIGN KEY (file_id) REFERENCES files (id) ON DELETE CASCADE
);

CREATE INDEX idx_file_owner ON files(owner_keycloak_id);
CREATE INDEX idx_file_status ON files(status);
CREATE INDEX idx_file_type ON files(file_type);

CREATE INDEX idx_image_lod_file ON image_level_of_detail(file_id);
CREATE INDEX idx_image_lod_level ON image_level_of_detail(level_of_detail);

CREATE INDEX idx_tag_name ON tag(tag_name);
CREATE INDEX idx_tag_owner ON tag(owner_keycloak_id);
CREATE INDEX idx_tag_file_type ON tag(file_type);

CREATE INDEX idx_file_tag_file ON file_tag(file_id);
CREATE INDEX idx_file_tag_tag ON file_tag(tag_id);

INSERT INTO tag (tag_name, owner_keycloak_id, file_type) VALUES
    ('Thumbnail', NULL, 'IMAGE'),
    ('Outdoor', NULL, 'IMAGE'),
    ('Indoor', NULL, 'IMAGE'),
    ('Kitchen', NULL, 'IMAGE'),
    ('Living room', NULL, 'IMAGE'),
    ('Bedroom', NULL, 'IMAGE'),
    ('Bathroom', NULL, 'IMAGE');
