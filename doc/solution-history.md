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

start: 13:12
end: 13:17

--- same prompt ---

do you have tests for
```java
  if (!regionRepository.existsById(id)) {
  throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Region not found");
  }
```

# Account group

implement CRUD methods for account_group table

use RegionController , PingRepository and RegionService as example of behavior 

start: 13:25
end: 13:26

--- same prompt ---

at some point in time Integer nonExistentId = 9999; will fail


# Account

implement CRUD methods for account table similar to account_group and region

start: 13:53
end: 13:55

-- same prompt ---

do you have tests for new account with non-existent region or/and account group?

-- same prompt ---

cover all cases, both region and account group is invalid,

and cover all cases in update, have case only for invalid region

# Account for group and for region

in region controller add method to get all accounts for region
in account group controller add method to get all accounts for group

--- same prompt ---

you cannot guarantee that there will be no region 999999

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

--- same prompt ---

add test for non english search e.g.
اَلْعَرَبِيَّةُ
