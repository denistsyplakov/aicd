# Region
for DB table Region

Create Region repository for CRUD operations use `PingRepository` as example
Create DTO for Region as record inside `PingRepository`
do not create iontermidiate DTO for controller.

create RegionController that should support CRUD operations
- create
- get all
- get by id
- update
- delete
  Use common naming conventions, prefix all http methods by /api

all get calls should go directly to the repository

for create, read, update create RegionService

on update mind uniquness of region name (create change set with uniuque index)
and if not unique throw proper http error (not 500)

on delete check if region is in use and if reference constraint fails return proper http error

# Account group

implement CRUD methods for account_group table

use RegionController , PingRepository and RegionService as example of behavior


# Account

implement CRUD methods for account table similar to account_group and region


# Account for group and for region

in region controller add method to get all accounts for region
in account group controller add method to get all accounts for group

# SoW

implement CRUD methods for SoW table similar to account.
once Sow is created or updated or deleted update sow_text_index index

create in SoWController method to do search across all SoWs. Method should accepts
- max doc
- min rank
- max text length

and should return list of SoWDTO with text cropped to max text length with "... " in end if cropped.

for tests in test/resources - generate 5+ SoW like documents and make test methods to test search.

do not do direct JDBCTemplate queries. You can use JDBCTemplate only in tests. For production use repository with @Query

mind that search string and text could contain special characters and use differenet languages including Hebrew and Arabic.

# update API paths

In Ping controller I have @GetMapping("/api/ping") full path in GetMapping update all controllers to be consistent with Ping controller

duration: about 5 min

# test scenarious

collection list of all test scenarios in IT tests and write them into doc/test-scenarious.md
and group by entity.

# implementation notes

In tests cover all possible combinations of negative scenarios.

In tests do not rely on magic numbers like region 9999 does not exists in DB. 

Do not cleanup DB before tests. Cleanup all creates entities from DB after tests.