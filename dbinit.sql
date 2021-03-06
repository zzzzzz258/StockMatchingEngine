create database stock_market;

create table account (
account_id varchar(10) primary key,
balance double precision
);

create table position (
symbol varchar(10),
amount double precision,
account_id varchar(10),
primary key(symbol, account_id),
constraint fk_account_id
foreign key(account_id) references account(account_id) on delete cascade on update cascade
);

create table stock_order (
order_id serial primary key,
symbol varchar(10),
amount double precision,
limit_price double precision,
account_id varchar(10),
status varchar,
time bigint,
constraint fk_account_id
foreign key(account_id) references account(account_id) on delete cascade on update cascade
);

create table transaction (
transaction_id serial primary key,
order_id integer,
amount double precision,
price double precision,
time bigint,
constraint fk_order_id
foreign key(order_id) references stock_order(order_id) on delete cascade on update cascade
);
