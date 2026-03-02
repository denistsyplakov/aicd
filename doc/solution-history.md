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


