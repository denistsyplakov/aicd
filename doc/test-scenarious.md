# Test Scenarios

## Region

| # | Scenario | Expected |
|---|----------|----------|
| 1 | GET /api/region | 200, JSON array |
| 2 | POST /api/region with valid name | 201, DTO with non-null id and correct name |
| 3 | GET /api/region/{id} for existing region | 200, correct name |
| 4 | GET /api/region/{id} for non-existent id | 404 |
| 5 | PUT /api/region/{id} with new name | 200, updated name |
| 6 | PUT /api/region/{id} for non-existent id | 404 |
| 7 | DELETE /api/region/{id} for existing region | 204 |
| 8 | DELETE /api/region/{id} for non-existent id | 404 |
| 9 | POST /api/region with duplicate name | 409 |
| 10 | PUT /api/region/{id} to name of another existing region | 409 |
| 11 | DELETE /api/region/{id} when region is referenced by an account | 409 |
| 12 | GET /api/region/{id}/accounts | 200, array containing the associated account |

## Account Group

| # | Scenario | Expected |
|---|----------|----------|
| 1 | GET /api/account-group | 200, JSON array |
| 2 | POST /api/account-group with valid name | 201, DTO with non-null id and correct name |
| 3 | GET /api/account-group/{id} for existing group | 200, correct name |
| 4 | GET /api/account-group/{id} for non-existent id | 404 |
| 5 | PUT /api/account-group/{id} with new name | 200, updated name |
| 6 | PUT /api/account-group/{id} for non-existent id | 404 |
| 7 | DELETE /api/account-group/{id} for existing group | 204 |
| 8 | DELETE /api/account-group/{id} for non-existent id | 404 |
| 9 | POST /api/account-group with duplicate name | 409 |
| 10 | PUT /api/account-group/{id} to name of another existing group | 409 |
| 11 | DELETE /api/account-group/{id} when group is referenced by an account | 409 |
| 12 | GET /api/account-group/{id}/accounts | 200, array containing the associated account |

## Account

| # | Scenario | Expected |
|---|----------|----------|
| 1 | GET /api/account | 200, JSON array |
| 2 | POST /api/account with valid name, accountGroupId, regionId | 201, DTO with non-null id |
| 3 | GET /api/account/{id} for existing account | 200, correct name |
| 4 | GET /api/account/{id} for non-existent id | 404 |
| 5 | PUT /api/account/{id} with new name | 200, updated name |
| 6 | PUT /api/account/{id} for non-existent id | 404 |
| 7 | DELETE /api/account/{id} for existing account | 204 |
| 8 | DELETE /api/account/{id} for non-existent id | 404 |
| 9 | POST /api/account with non-existent region_id | 409 |
| 10 | POST /api/account with non-existent account_group_id | 409 |
| 11 | PUT /api/account/{id} with non-existent region_id | 409 |
| 12 | DELETE /api/account/{id} when account is referenced by a SoW | 409 |

## SoW (Statement of Work)

| # | Scenario | Expected |
|---|----------|----------|
| 1 | POST /api/sow with valid data | 201, DTO with non-null id and correct title |
| 2 | GET /api/sow/{id} for existing SoW | 200, correct title |
| 3 | GET /api/sow/{id} for non-existent id | 404 |
| 4 | PUT /api/sow/{id} with updated title and text | 200, updated title |
| 5 | DELETE /api/sow/{id} for existing SoW | 204 |
| 6 | DELETE /api/sow/{id} for non-existent id | 404 |
| 7 | POST /api/sow with non-existent account_id | 409 |
| 8 | PUT /api/sow/{id} with non-existent account_id | 409 |
| 9 | POST /api/sow/search with English query matching document | 200, results include matching document |
| 10 | POST /api/sow/search with Hebrew query | 200, results include Hebrew document |
| 11 | POST /api/sow/search with Arabic query | 200, results include Arabic document |
| 12 | POST /api/sow/search with maxDoc=2 when more results exist | 200, at most 2 results |
| 13 | POST /api/sow/search with maxTextLength shorter than text | 200, text cropped with "... " suffix |
| 14 | POST /api/sow/search with high minRank | 200, fewer or no results vs minRank=0 |
