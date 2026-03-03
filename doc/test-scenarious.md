# Integration test scenarious

## Region
- Create region successfully.
- Reject duplicate region name with HTTP 409.
- Get all regions and get region by id.
- Return HTTP 404 for get/update/delete on non-existing region id.
- Reject delete when region is referenced by account with HTTP 409.
- Get all accounts for region.

## Account Group
- Create account group successfully.
- Get all account groups and get account group by id.
- Update account group successfully.
- Return HTTP 404 for get/update/delete on non-existing account group id.
- Reject delete when account group is referenced by account with HTTP 409.
- Get all accounts for account group.

## Account
- Create account successfully with valid region and account group references.
- Reject account create/update when region reference is missing with HTTP 409.
- Reject account create/update when account group reference is missing with HTTP 409.
- Get all accounts and get account by id.
- Update account successfully.
- Return HTTP 404 for get/update/delete on non-existing account id.

## SoW
- Create SoW successfully and update text index.
- Update SoW successfully and update text index.
- Delete SoW successfully and remove text index.
- Reject SoW create/update when account reference is missing with HTTP 409.
- Get all SoWs and get SoW by id.
- Return HTTP 404 for get/update/delete on non-existing SoW id.
- Search SoWs with special characters and multilingual content.
- Enforce search parameters (`maxDoc`, `minRank`, `maxTextLength`) and reject invalid combinations with HTTP 400.
- Return cropped text with `"... "` suffix when text exceeds `maxTextLength`.
