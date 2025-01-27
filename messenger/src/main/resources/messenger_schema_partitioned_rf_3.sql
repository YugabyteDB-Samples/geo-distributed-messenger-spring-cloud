CREATE SCHEMA Messenger;

SET search_path TO Messenger;

CREATE TABLESPACE us_ts WITH (
    replica_placement = '{"num_replicas": 3, "placement_blocks":
  [
    {"cloud":"gcp","region":"us-east1","zone":"us-east1-b","min_num_replicas":1},
    {"cloud":"gcp","region":"us-central1","zone":"us-central1-b","min_num_replicas":1},
    {"cloud":"gcp","region":"us-west1","zone":"us-west1-c","min_num_replicas":1}
  ]}'
);

CREATE TABLESPACE europe_ts WITH (
    replica_placement = '{"num_replicas": 3, "placement_blocks":
  [{"cloud":"gcp","region":"europe-west3","zone":"europe-west3-a","min_num_replicas":3}]}'
);

CREATE TABLESPACE apac_ts WITH (
    replica_placement = '{"num_replicas": 3, "placement_blocks":
  [{"cloud":"gcp","region":"asia-east1","zone":"asia-east1-a","min_num_replicas":3}]}'
);

CREATE SEQUENCE profile_id_seq
    CACHE 100
    INCREMENT BY 5;

CREATE TABLE Profile(
    id integer NOT NULL DEFAULT nextval('profile_id_seq'),
    full_name text NOT NULL,
    email text NOT NULL,
    phone text NOT NULL,
    country_code text NOT NULL,
    hashed_password text NOT NULL,
    user_picture_url text
)
PARTITION BY LIST (country_code);

CREATE TABLE Profile_USA PARTITION OF Profile(id, full_name, email, phone, country_code, hashed_password, user_picture_url, PRIMARY KEY (id, country_code))
FOR VALUES IN ('US') TABLESPACE us_ts;

CREATE TABLE Profile_EU PARTITION OF Profile(id, full_name, email, phone, country_code, hashed_password, user_picture_url, PRIMARY KEY (id, country_code))
FOR VALUES IN ('EU') TABLESPACE europe_ts;

CREATE TABLE Profile_APAC PARTITION OF Profile(id, full_name, email, phone, country_code, hashed_password, user_picture_url, PRIMARY KEY (id, country_code))
FOR VALUES IN ('APAC') TABLESPACE apac_ts;

CREATE SEQUENCE workspace_id_seq
    CACHE 100
    INCREMENT BY 5;

CREATE TABLE Workspace(
    id integer NOT NULL DEFAULT nextval('workspace_id_seq'),
    name text NOT NULL,
    country_code text NOT NULL
)
PARTITION BY LIST (country_code);

CREATE TABLE Workspace_USA PARTITION OF Workspace(id, name, country_code, PRIMARY KEY (id, country_code))
FOR VALUES IN ('US') TABLESPACE us_ts;

CREATE TABLE Workspace_EU PARTITION OF Workspace(id, name, country_code, PRIMARY KEY (id, country_code))
FOR VALUES IN ('EU') TABLESPACE europe_ts;

CREATE TABLE Workspace_APAC PARTITION OF Workspace(id, name, country_code, PRIMARY KEY (id, country_code))
FOR VALUES IN ('APAC') TABLESPACE apac_ts;

CREATE TABLE Workspace_Profile(
    workspace_id integer,
    profile_id integer,
    workspace_country text NOT NULL,
    profile_country text NOT NULL
)
PARTITION BY LIST (workspace_country);

CREATE TABLE Workspace_Profile_USA PARTITION OF Workspace_Profile(workspace_id, profile_id, workspace_country, profile_country, PRIMARY KEY (workspace_id, profile_id, workspace_country))
FOR VALUES IN ('US') TABLESPACE us_ts;

CREATE TABLE Workspace_Profile_EU PARTITION OF Workspace_Profile(workspace_id, profile_id, workspace_country, profile_country, PRIMARY KEY (workspace_id, profile_id, workspace_country))
FOR VALUES IN ('EU') TABLESPACE europe_ts;

CREATE TABLE Workspace_Profile_APAC PARTITION OF Workspace_Profile(workspace_id, profile_id, workspace_country, profile_country, PRIMARY KEY (workspace_id, profile_id, workspace_country))
FOR VALUES IN ('APAC') TABLESPACE apac_ts;

CREATE SEQUENCE channel_id_seq
    CACHE 100
    INCREMENT BY 5;

CREATE TABLE Channel(
    id integer NOT NULL DEFAULT nextval('channel_id_seq'),
    name text NOT NULL,
    workspace_id integer NOT NULL,
    country_code text NOT NULL
)
PARTITION BY LIST (country_code);

CREATE TABLE Channel_USA PARTITION OF Channel(id, name, workspace_id, country_code, PRIMARY KEY (id, country_code))
FOR VALUES IN ('US') TABLESPACE us_ts;

CREATE TABLE Channel_EU PARTITION OF Channel(id, name, workspace_id, country_code, PRIMARY KEY (id, country_code))
FOR VALUES IN ('EU') TABLESPACE europe_ts;

CREATE TABLE Channel_APAC PARTITION OF Channel(id, name, workspace_id, country_code, PRIMARY KEY (id, country_code))
FOR VALUES IN ('APAC') TABLESPACE apac_ts;

CREATE TABLE Message(
    id uuid NOT NULL,
    channel_id integer,
    sender_id integer NOT NULL,
    message text NOT NULL,
    attachment boolean NOT NULL DEFAULT FALSE,
    sent_at timestamp NOT NULL DEFAULT NOW(),
    country_code text NOT NULL,
    sender_country_code text NOT NULL
)
PARTITION BY LIST (country_code);

CREATE TABLE Message_USA PARTITION OF Message(id, channel_id, sender_id, message, sent_at, country_code, sender_country_code, PRIMARY KEY (id, country_code))
FOR VALUES IN ('US') TABLESPACE us_ts;

CREATE TABLE Message_EU PARTITION OF Message(id, channel_id, sender_id, message, sent_at, country_code, sender_country_code, PRIMARY KEY (id, country_code))
FOR VALUES IN ('EU') TABLESPACE europe_ts;

CREATE TABLE Message_APAC PARTITION OF Message(id, channel_id, sender_id, message, sent_at, country_code, sender_country_code, PRIMARY KEY (id, country_code))
FOR VALUES IN ('APAC') TABLESPACE apac_ts;

