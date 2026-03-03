-- add unique constraint to region.name
alter table region add constraint unique_region_name unique (name);
