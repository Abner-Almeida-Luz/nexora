-- Lock otimista no Product (JPA @Version).
-- Default 0 garante que linhas pré-existentes continuem válidas.
ALTER TABLE products ADD COLUMN version BIGINT NOT NULL DEFAULT 0;