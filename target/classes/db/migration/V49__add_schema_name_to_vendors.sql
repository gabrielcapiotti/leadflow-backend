-- Migration to add schema_name column to vendors table

ALTER TABLE vendors
ADD COLUMN schema_name VARCHAR(63);

CREATE UNIQUE INDEX uq_vendors_schema_name
ON vendors(schema_name);

-- Uncomment the following line to make the column mandatory
-- ALTER TABLE vendors
-- ALTER COLUMN schema_name SET NOT NULL;