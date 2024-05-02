set @exist := (select count(*) from information_schema.statistics where table_name = 'services' and index_name = 'entity_id_UNIQUE');
set @sqlstmt := if( @exist > 0, 'ALTER TABLE services DROP KEY entity_id_UNIQUE', 'select ''INFO: Index does not exists.''');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
