-- create unique index for region name
CREATE UNIQUE INDEX IF NOT EXISTS idx_region_name_unique ON region(name);
