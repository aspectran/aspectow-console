-- Set the current schema
SET SCHEMA appmon;

-- User accounts
create table if not exists asc_user (
    user_id bigint not null auto_increment,
    username varchar(50) not null unique,
    password varchar(100) not null,
    nickname varchar(50),
    email varchar(100),
    status varchar(10) default 'NORMAL' not null, -- NORMAL, LOCKED, EXPIRED
    last_login_at timestamp,
    created_at timestamp default current_timestamp not null,
    updated_at timestamp default current_timestamp not null,
    primary key (user_id)
);

comment on table asc_user is 'User accounts';

-- Roles
create table if not exists asc_role (
    role_id bigint not null auto_increment,
    role_name varchar(50) not null unique,
    description varchar(200),
    primary key (role_id)
);

comment on table asc_role is 'Roles';

-- User-Role mapping
create table if not exists asc_user_role (
    user_id bigint not null,
    role_id bigint not null,
    primary key (user_id, role_id),
    foreign key (user_id) references asc_user(user_id) on delete cascade,
    foreign key (role_id) references asc_role(role_id) on delete cascade
);

comment on table asc_user_role is 'User-Role mapping';

-- Permissions
create table if not exists asc_permission (
    perm_id bigint not null auto_increment,
    perm_code varchar(50) not null unique,
    description varchar(200),
    primary key (perm_id)
);

comment on table asc_permission is 'Permissions';

-- Role-Permission mapping
create table if not exists asc_role_permission (
    role_id bigint not null,
    perm_id bigint not null,
    primary key (role_id, perm_id),
    foreign key (role_id) references asc_role(role_id) on delete cascade,
    foreign key (perm_id) references asc_permission(perm_id) on delete cascade
);

comment on table asc_role_permission is 'Role-Permission mapping';

-- Login History
create table if not exists asc_login_history (
    history_id bigint not null auto_increment,
    username varchar(50) not null,
    login_at timestamp default current_timestamp not null,
    ip_address varchar(45),
    user_agent varchar(500),
    success_yn char(1) default 'Y' not null,
    primary key (history_id)
);

comment on table asc_login_history is 'Login History';

-- Raw event count data (typically 5-minute intervals)
create table if not exists asc_appmon_event_count (
    domain varchar(30) not null,
    instance varchar(30) not null,
    event varchar(30) not null,
    datetime timestamp not null,
    total int not null,
    delta int not null,
    error int not null,
    primary key (domain, instance, event, datetime)
);

comment on table asc_appmon_event_count is 'Raw event count data';

-- Hourly aggregated event count data
create table if not exists asc_appmon_event_count_hourly (
    domain varchar(30) not null,
    instance varchar(30) not null,
    event varchar(30) not null,
    datetime timestamp not null,
    total int not null,
    delta int not null,
    error int not null,
    primary key (domain, instance, event, datetime)
);

comment on table asc_appmon_event_count_hourly is 'Hourly aggregated event count data';

-- Most recent event count state for incremental updates
create table if not exists asc_appmon_event_count_last (
    domain varchar(30) not null,
    instance varchar(30) not null,
    event varchar(30) not null,
    datetime timestamp not null,
    total int not null,
    delta int not null,
    error int not null,
    reg_dt timestamp default current_timestamp not null,
    primary key (domain, instance, event)
);

comment on table asc_appmon_event_count_last is 'Most recent event count state';

-- Initial data for testing
insert IGNORE into asc_role (role_name, description) values ('SUPER_ADMIN', 'Super administrator with full access');
insert IGNORE into asc_role (role_name, description) values ('ADMIN', 'Administrator with limited management access');
insert IGNORE into asc_role (role_name, description) values ('VIEWER', 'User with read-only access');

insert IGNORE into asc_permission (perm_code, description) values ('MONITOR_VIEW', 'Access to monitoring dashboard');
insert IGNORE into asc_permission (perm_code, description) values ('MONITOR_CONTROL', 'Control monitoring settings');
insert IGNORE into asc_permission (perm_code, description) values ('USER_MANAGE', 'Manage users and roles');

-- Map permissions to SUPER_ADMIN
insert IGNORE into asc_role_permission (role_id, perm_id) select 1, perm_id from asc_permission;
-- Map permissions to VIEWER
insert IGNORE into asc_role_permission (role_id, perm_id) select 3, perm_id from asc_permission where perm_code = 'MONITOR_VIEW';

-- Initial Super Admin user (password: admin123)
insert IGNORE into asc_user (username, password, nickname, email) values ('admin', 'admin123', 'Super Admin', 'admin@aspectow.com');
insert IGNORE into asc_user_role (user_id, role_id) values (1, 1);
