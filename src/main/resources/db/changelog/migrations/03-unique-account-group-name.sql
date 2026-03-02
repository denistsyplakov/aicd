-- make account_group name unique
create unique index idx_account_group_name_unique on account_group(name);
