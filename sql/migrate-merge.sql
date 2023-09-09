-- patch 9906
INSERT INTO iris.system_attribute (name,value) VALUES ('video_still_enable', 'false');

-- patch 9915
insert into iris.system_attribute (name,value) values ('incident_free_enable','false');

-- patch 9919
ALTER TABLE event.incident ADD COLUMN notes VARCHAR(256);
UPDATE event.incident set notes='';          
ALTER TABLE event.incident alter COLUMN notes type VARCHAR(256),alter column notes set NOT null;