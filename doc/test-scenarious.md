# Integration Test Scenarios (grouped by entity)

## Ping
- GET `/api/ping` returns 200 OK and body `"2"`.

## Region
- CRUD
  - Create: POST `/api/region` creates a region and returns DTO with `id` and `name`.
  - Get all: GET `/api/region` returns list containing the created region.
  - Get by id: GET `/api/region/{id}` returns the created region DTO.
  - Update: PUT `/api/region/{id}` updates the region name and returns updated DTO.
  - Update conflict: PUT `/api/region/{id}` to an existing name returns 409 CONFLICT.
  - Delete (referenced): DELETE `/api/region/{id}` fails with 409 CONFLICT when accounts reference the region.
  - Delete (success): After removing references, DELETE `/api/region/{id}` returns 204 NO CONTENT.
- Not Found
  - GET `/api/region/{nonExistentId}` returns 404 NOT FOUND.
  - PUT `/api/region/{nonExistentId}` returns 404 NOT FOUND.
  - DELETE `/api/region/{nonExistentId}` returns 404 NOT FOUND.
- Relationships (Accounts for Region)
  - GET `/api/region/{id}/account` returns only accounts belonging to the region (verifies count and names).
  - GET `/api/region/{nonExistentId}/account` returns 404 NOT FOUND.

## Account Group
- CRUD
  - Create: POST `/api/account-group` creates a group and returns DTO with `id` and `name`.
  - Get all: GET `/api/account-group` returns list containing the created group.
  - Get by id: GET `/api/account-group/{id}` returns the created group DTO.
  - Update: PUT `/api/account-group/{id}` updates the group name and returns updated DTO.
  - Update conflict: PUT `/api/account-group/{id}` to an existing name returns 409 CONFLICT.
  - Delete (referenced): DELETE `/api/account-group/{id}` fails with 409 CONFLICT when accounts reference the group.
  - Delete (success): After removing references, DELETE `/api/account-group/{id}` returns 204 NO CONTENT.
- Not Found
  - GET `/api/account-group/{nonExistentId}` returns 404 NOT FOUND.
  - PUT `/api/account-group/{nonExistentId}` returns 404 NOT FOUND.
  - DELETE `/api/account-group/{nonExistentId}` returns 404 NOT FOUND.
- Relationships (Accounts for Account Group)
  - GET `/api/account-group/{id}/account` returns only accounts belonging to the group (verifies count and names).
  - GET `/api/account-group/{nonExistentId}/account` returns 404 NOT FOUND.

## Account
- CRUD
  - Prerequisites: create an Account Group and a Region.
  - Create: POST `/api/account` creates an account with valid `account_group_id` and `region_id` and returns DTO including those IDs.
  - Get all: GET `/api/account` returns list containing the created account.
  - Get by id: GET `/api/account/{id}` returns the created account DTO.
  - Update: PUT `/api/account/{id}` updates the account name and returns updated DTO.
  - Update conflict: PUT `/api/account/{id}` to an existing account name returns 409 CONFLICT.
  - Delete (referenced): DELETE `/api/account/{id}` fails with 409 CONFLICT when SoW rows reference the account.
  - Delete (success): After removing references, DELETE `/api/account/{id}` returns 204 NO CONTENT.
- Not Found
  - GET `/api/account/{nonExistentId}` returns 404 NOT FOUND.
  - PUT `/api/account/{nonExistentId}` returns 404 NOT FOUND.
  - DELETE `/api/account/{nonExistentId}` returns 404 NOT FOUND.
- Foreign key validation
  - Create with invalid `account_group_id` returns 400 BAD REQUEST.
  - Create with invalid `region_id` returns 400 BAD REQUEST.
  - Create with both invalid `account_group_id` and `region_id` returns 400 BAD REQUEST.
  - Update with invalid `account_group_id` returns 400 BAD REQUEST.
  - Update with invalid `region_id` returns 400 BAD REQUEST.
  - Update with both invalid FKs returns 400 BAD REQUEST.

## SoW
- Setup per test: create Account Group, Region, and Account for SoW association.
- CRUD
  - Create: POST `/api/sow` creates a SoW tied to an account; response contains `id`, `title`, `accountId`, `amount`, `description`, `text`.
  - Get by id: GET `/api/sow/{id}` returns the created SoW DTO with matching fields.
  - Update: PUT `/api/sow/{id}` updates title/amount/description/text and returns updated DTO.
  - Delete: DELETE `/api/sow/{id}` returns 204 NO CONTENT; subsequent GET by id returns 404 NOT FOUND.
- Search
  - Indexing/creation: load 5+ text documents and create corresponding SoWs.
  - Query with `max_text_length`: GET `/api/sow/search?query=AWS&max_text_length=50` returns 200 OK, non-empty list; first item `text` is cropped and ends with `... `; contains expected word from document.
  - Simple term query: GET `/api/sow/search?query=Banking` returns non-empty list containing the term.
  - Limit results: `max_doc=2` caps the number of results to 2.
  - Minimum rank filter: `min_rank=0.9` returns an empty result set.
  - Non‑English search: searching for `اَلْعَرَبِيَّةُ` returns results that include the inserted Arabic‑text SoW.
  - Special characters and whitespace in query: queries containing symbols like `?>/`, line breaks, and tabs are accepted and return the matching SoW.
