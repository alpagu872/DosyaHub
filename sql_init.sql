-- DosyaHub - PostgreSQL Veritabanı Başlangıç Scripti

-- Veritabanı oluşturma (Docker kullanılmıyorsa)
-- CREATE DATABASE dosyahub;

-- Bağlantı
-- \c dosyahub

-- Tabloları oluşturmadan önce varsa silme (Geliştirme aşamasında kullanışlı)
DROP TABLE IF EXISTS file_metadata;
DROP TABLE IF EXISTS users;
DROP TYPE IF EXISTS file_type;

-- FileType enum tipi
CREATE TYPE file_type AS ENUM ('PDF', 'PNG', 'JPG');

-- Kullanıcılar tablosu
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_login_at TIMESTAMP
);

-- Dosya metadata tablosu
CREATE TABLE file_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_type file_type NOT NULL,
    size BIGINT NOT NULL,
    bucket_name VARCHAR(100) NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- İndeksler
CREATE INDEX idx_file_metadata_user_id ON file_metadata(user_id);
CREATE INDEX idx_file_metadata_file_type ON file_metadata(file_type);
CREATE INDEX idx_file_metadata_uploaded_at ON file_metadata(uploaded_at);

-- Test kullanıcısı (Şifre: password)
INSERT INTO users (email, password, first_name, last_name)
VALUES ('test@example.com', '$2a$12$ZMM.o0l8xn0vSYFmKJTSXO6BgBRYayloPUYuQFPNd5rnQ8CgKdCeC', 'Test', 'Kullanıcı');

-- Yetki ve izinler (Gerekirse)
-- GRANT ALL PRIVILEGES ON DATABASE dosyahub TO dosyahub;
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO dosyahub;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO dosyahub; 