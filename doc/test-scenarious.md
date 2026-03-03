# Test Scenarios

## Region
- **Create Region**: Verify that a new region can be created with a unique name.
- **Get All Regions**: Verify that all regions can be retrieved.
- **Get Region by ID**: Verify that a region can be retrieved by its ID.
- **Update Region**: Verify that a region's name can be updated.
- **Negative: Duplicate Region Name**: Verify that creating a region with an existing name returns a 409 Conflict error.
- **Negative: Delete Region in Use**: Verify that deleting a region referenced by an account returns a 409 Conflict error.
- **Get Accounts for Region**: Verify that all accounts belonging to a specific region can be retrieved.

## Account Group
- **Create Account Group**: Verify that a new account group can be created.
- **Get All Account Groups**: Verify that all account groups can be retrieved.
- **Get Account Group by ID**: Verify that an account group can be retrieved by its ID.
- **Update Account Group**: Verify that an account group's name can be updated.
- **Negative: Delete Account Group in Use**: Verify that deleting an account group referenced by an account returns a 409 Conflict error.
- **Get Accounts for Group**: Verify that all accounts belonging to a specific account group can be retrieved.

## Account
- **Create Account**: Verify that a new account can be created and linked to a region and an account group.
- **Get All Accounts**: Verify that all accounts can be retrieved.
- **Get Account by ID**: Verify that an account can be retrieved by its ID.
- **Update Account**: Verify that an account's details can be updated.
- **Delete Account**: Verify that an account can be deleted (when not in use by SoWs).
- **Negative: Create Account with missing Region**: Verify that creating an account with a non-existent region ID returns a 404 Not Found error.
- **Negative: Create Account with missing Group**: Verify that creating an account with a non-existent account group ID returns a 404 Not Found error.

## SoW (Statement of Work)
- **Create SoW**: Verify that a new SoW can be created and the text index is updated.
- **Get All SoWs**: Verify that all SoWs can be retrieved.
- **Get SoW by ID**: Verify that an SoW can be retrieved by its ID.
- **Update SoW**: Verify that an SoW's details and text can be updated, and the index is refreshed.
- **Delete SoW**: Verify that an SoW and its corresponding text index entry are deleted.
- **Search SoW**: Verify full-text search across all SoWs with parameters:
  - `query`: search term.
  - `maxDoc`: maximum number of results.
  - `minRank`: minimum rank for results.
  - `maxTextLength`: verify that the returned text is cropped and ends with "... " if it exceeds the limit.
- **Multilingual Search**: Verify that search handles special characters and different languages (Hebrew, Arabic) without crashing.
- **Negative: Create SoW with missing Account**: Verify that creating an SoW with a non-existent account ID returns a 404 Not Found error.

## Ping
- **Ping**: Verify that the `/api/ping` endpoint returns the expected response.
