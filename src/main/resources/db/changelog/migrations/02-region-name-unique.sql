-- enforce region name uniqueness
create unique index if not exists uq_region_name on region(name);
