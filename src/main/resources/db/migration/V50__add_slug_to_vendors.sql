-- Migration to add slug column to vendors table

ALTER TABLE vendors
ADD COLUMN slug VARCHAR(255);

CREATE UNIQUE INDEX idx_vendors_slug
ON vendors(slug);