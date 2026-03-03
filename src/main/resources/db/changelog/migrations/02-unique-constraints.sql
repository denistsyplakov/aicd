alter table region add constraint uq_region_name unique (name);
alter table account_group add constraint uq_account_group_name unique (name);
