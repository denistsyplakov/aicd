-- initial schema here

create table region(
    id serial primary key,
    name varchar(255)
);

create table account_group(
    id serial primary key,
    name varchar(255)
);

create table account(
    id serial primary key,
    name varchar(255),
    account_group_id integer references account_group(id),
    region_id integer references region(id)
);

create table sow(
    id serial primary key,
    account_id integer references account(id),
    date date,
    title varchar(255),
    amount numeric(19,2),
    description varchar(255),
    text varchar
);

create table sow_text_index(
    id integer primary key references sow(id),
    tsvector tsvector
);

create index idx_search_sow_text on sow_text_index using gin(tsvector);