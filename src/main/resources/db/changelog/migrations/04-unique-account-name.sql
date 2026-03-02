-- create unique index for account name
CREATE UNIQUE INDEX IF NOT EXISTS idx_account_name_unique ON account(name);
