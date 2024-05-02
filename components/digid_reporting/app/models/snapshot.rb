
# Deze broncode is openbaar gemaakt vanwege een Woo-verzoek zodat deze
# gericht is op transparantie en niet op hergebruik. Hergebruik van 
# de broncode is toegestaan onder de EUPL licentie, met uitzondering 
# van broncode waarvoor een andere licentie is aangegeven.
# 
# Het archief waar dit bestand deel van uitmaakt is te vinden op:
#   https://github.com/MinBZK/woo-besluit-broncode-digid
# 
# Eventuele kwetsbaarheden kunnen worden gemeld bij het NCSC via:
#   https://www.ncsc.nl/contact/kwetsbaarheid-melden
# onder vermelding van "Logius, openbaar gemaakte broncode DigiD" 
# 
# Voor overige vragen over dit Woo-besluit kunt u mailen met open@logius.nl
# 
# This code has been disclosed in response to a request under the Dutch
# Open Government Act ("Wet open Overheid"). This implies that publication 
# is primarily driven by the need for transparence, not re-use.
# Re-use is permitted under the EUPL-license, with the exception 
# of source files that contain a different license.
# 
# The archive that this file originates from can be found at:
#   https://github.com/MinBZK/woo-besluit-broncode-digid
# 
# Security vulnerabilities may be responsibly disclosed via the Dutch NCSC:
#   https://www.ncsc.nl/contact/kwetsbaarheid-melden
# using the reference "Logius, publicly disclosed source code DigiD" 
# 
# Other questions regarding this Open Goverment Act decision may be
# directed via email to open@logius.nl

include ApplicationHelper

class Snapshot < ReportingBase
   def self.table_name
      "snapshots" # QQQ which is the default table name for this class
   end

   @@no_replication_position = "nofile@-1"
   # QQQ Configurable? Through model(s)??
   @@snapshot_tables = ["accounts", "activation_letters", "registrations"]

   def self.generate_snapshot
      done = false
      snapshot = nil

      while !done && (seconds = remaining_inactive_seconds()) > 5
         logger.info "Starting snapsnot generation with #{seconds} seconds of server inactivity remaining."

         master_pos = replication_master_position()
         sleep 1.0 # QQQ configurable! Howto?
         slave_pos = replication_slave_position()

         # all activity has been processed
         if slave_pos.present? && master_pos.present? && slave_pos >= master_pos
            snapshot = insert_snapshot_info(Time.now())
            # Reload snapshot here, because Mysql sets sub seconds presicion to zero's
            snapshot.reload
            stop_slave_sql_thread()
            begin
               set_log_bookmark(snapshot)
               create_table_snapshot(snapshot.database_name)
            ensure
               start_slave_sql_thread()
            end
            create_counter_snapshot(snapshot)
            done = true
            logger.info "Accounts database snapshot created: " + snapshot.database_name.to_s
            cleanup_snapshots()
         else
            logger.warn("Could not get slave position [#{slave_pos}] ahead or equal of master position [#{master_pos}]")
         end
      end

      if !done then logger.fatal "No snapshot generated (seconds #{seconds})" end

      # QQQ Log snapshot creation failure?
      # No rollback, rely on snapshot cleanup
      snapshot
   end

   def self.remaining_inactive_seconds()
      result = -1

      begin
         # QQQ The certificate won't match the ip address used. Ignore that.
         # QQQ ssl_verify_mode requires 1.9 it seems, so no go
         #result = open(APP_CONFIG['inactivity_interval_url'], :ssl_verify_mode => OpenSSL::SSL::VERIFY_NONE).read.to_i

         uri = URI.parse(APP_CONFIG['inactivity_interval_url'])
         http = Net::HTTP.new(uri.host, uri.port)
         http.use_ssl = (uri.scheme.casecmp("https") == 0 ? true : false)
         http.verify_mode = OpenSSL::SSL::VERIFY_NONE
         response = http.request_get uri.path
         if Net::HTTPSuccess === response
            result = response.body.to_i
         else
            logger.warn "Unexpected HTTP response: #{response.to_s}"
         end

         # Most Net::HTTP methods have something like "This method never raises Net::* exceptions."
         # Left this handler though, since it does no harm but will become necessary when switching to open-uri
      rescue OpenURI::HTTPError => e
         logger.error e.message
      end

      result
   end

   def self.replication_master_position()
      # QQQ check master position on both Accounts and Logs nodes?
      # For now we only check the logs master.
      return @@no_replication_position if !ApplicationHelper::reporting_server?

      #Not necessarily a different connection, but to allow for different user (privileges!) do this anyway.
      mysql_res = LogMaster.connection().execute("SHOW MASTER STATUS")

      position = nil # Failed to obtain position or replication broken
      mysql_res.each(:as => :hash) do |row|
         position = row['File'] + '@' + row['Position'].to_s
      end

      #QQQ NoMethod ?
      #mysql_res.free

      position
   end

   def self.replication_slave_position()
      return @@no_replication_position if !ApplicationHelper::reporting_server?

      mysql_res = LogSlave.connection().execute("SHOW SLAVE STATUS")

      position = nil # Failed to obtain position or replication broken
      mysql_res.each(:as => :hash) do |row|
         if row['Slave_IO_Running'] == 'Yes' and row['Slave_SQL_Running'] == 'Yes' and row['Seconds_Behind_Master'].to_s == '0'
            position = row['Relay_Master_Log_File'] + '@' + row['Exec_Master_Log_Pos'].to_s
         elsif row['Seconds_Behind_Master'].blank?
            logger.fatal <<-MSG.strip_heredoc
            Replication error. Seconds_Behind_Master is blank.
            Slave_IO_Running:#{row['Slave_IO_Running'].to_s}
            Slave_SQL_Running:#{row['Slave_SQL_Running'].to_s}
            Last_IO_Errno:#{row['Last_IO_Errno'].to_s}
            Last_IO_Error:#{row['Last_IO_Error'].to_s}
            Last_SQL_Errno:#{row['Last_SQL_Errno'].to_s}
            Last_SQL_Error:#{row['Last_SQL_Error'].to_s}
            MSG
         else
            logger.warn "Replication lags. Seconds_Behind_Master:#{row['Seconds_Behind_Master'].to_s}"
         end
      end

      #QQQ NoMethod ?
      #mysql_res.free

      position
   end

   def self.insert_snapshot_info(ts)
      logger.info "Inserting snapshot info."
      # QQQ Further attributes?
      database_name = "digid" + Rails.env + "_snap_" + ts.strftime("%Y%m%d%H%M")
      snapshot = Snapshot.create :database_name => database_name, :created_at => Time.now, :updated_at => Time.now

      logger.info "Inserted snapshot: #{snapshot.inspect}"

      snapshot
   end

   def self.set_log_bookmark(snapshot)
      #QQQ Would it be safer to get MAX from LogMaster?
      max = Log.maximum(:id)
      snapshot.log_id = max.present? ? max : 0
      snapshot.save!
   end

   def self.stop_slave_sql_thread()
      return nil if !ApplicationHelper::reporting_server?

      logger.info "Stopping slave SQL thread."

      # QQQ Only stop the slave sql thread on the accounts slave (i.e. keep it running on the logs slave)
      # QQQ May have to disable autocommit here because of (ERROR 1192 (HY000): Can’t execute the given command because you have active locked tables or an active transaction)
      mysql_res = AccountSlave.connection().execute("STOP SLAVE SQL_THREAD")
      logger.info "Slave SQL thread stopped, result: #{mysql_res.inspect()}"
      #QQQ NoMethod ?
      #mysql_res.free

      # QQQ status/return value?
   end

   def self.start_slave_sql_thread()
      # QQQ apart from "START" identical to stop_slave_sql_thread(). Refactor!
      return nil if !ApplicationHelper::reporting_server?

      logger.info "Starting slave SQL thread."

      mysql_res = AccountSlave.connection().execute("START SLAVE SQL_THREAD")
      logger.info "Slave SQL thread started, result: #{mysql_res.inspect()}"
      #QQQ NoMethod ?
      #mysql_res.free

      # QQQ status/return value?
   end

   def self.create_table_snapshot(database_name)
      # The CREATE DATABASE runs on the accounts slave instance which also hosts snapshot (local slave)
      connection = AccountSnapshotBase.dyn_connection nil

      # QQQ table name quoting for database names?
      quoted_source_database_name = connection.quote_table_name(connection.current_database)
      quoted_destination_database_name = connection.quote_table_name(database_name)
      logger.info "Creating snapshot database: #{quoted_destination_database_name}."
      connection.execute("CREATE DATABASE #{quoted_destination_database_name}")
      # Assume no grants needed on database since current user (creator) will be the only one to access it anyway

      #We have to "reconnect" to the new database to set the right context (MySQL USE) to avoid replication problems
      connection = AccountSnapshotBase.dyn_connection database_name
      logger.info "connected to #{connection.current_database}"

      # QQQ Copy outside of (database) transaction! How?
      @@snapshot_tables.each do |table_name|
         quoted_table_name = connection.quote_table_name(table_name)
         logger.info "Creating snapshot table: #{quoted_table_name}."
         connection.execute("CREATE TABLE #{quoted_destination_database_name}.#{quoted_table_name} LIKE #{quoted_source_database_name}.#{quoted_table_name};")
         connection.execute("INSERT INTO #{quoted_destination_database_name}.#{quoted_table_name} SELECT * FROM #{quoted_source_database_name}.#{quoted_table_name};")
      end
      logger.info "Snapshot database created."
   end

   def self.create_counter_snapshot(snapshot)
      counter_values = {}
      counters = []

      # Queries run against "dynamically named" snapshot db in <snapshot.database_name>
      ActivationLetterSnapshot.dyn_connection(snapshot.database_name)
      # QQQ is 'init' indeed OK?
      counter_values['TBr1'] = ActivationLetterSnapshot.where(:status => ::ActivationLetter::Status::CREATE).count

      AccountSnapshot.dyn_connection(snapshot.database_name)
      # QQQ is 'opgeschort' indeed OK?
      counter_values['TOps1'] = AccountSnapshot.where(:status => ::Account::Status::SUSPENDED).count
      # QQQ is 'actief' indeed OK?
      counter_values['TAct1'] = AccountSnapshot.where(:status => ::Account::Status::ACTIVE ).count + counter_values['TOps1']

      RegistrationSnapshot.dyn_connection(snapshot.database_name)
      # QQQ is 'init'+'aangevraagd' indeed OK?
      counter_values['TA1'] = RegistrationSnapshot.where(:status => [::Registration::Status::CREATE, ::Registration::Status::AANVRAAG]).count

      previous_snapshot = snapshot_before snapshot

      if previous_snapshot.nil?
         logger.warn "No previous snapshot found for snapshot #{snapshot.id}."
      end

      # Copy *1 values from previous snapshot to *2
      { 'TA1' => 'TA2', 'TAct1' => 'TAct2', 'TOps1' => 'TOps2', 'TBr1' => 'TBr2' }.each do |current, previous|
         prev_counter = nil
         if previous_snapshot.present?
            prev_counter = SnapshotCounter.where(:snapshot_id => previous_snapshot.id).where(:name => current).first
         end

         if prev_counter.present?
            counter_values[previous] = prev_counter.value
         else
            logger.warn "No #{current} counter record for previous snapshot. Setting #{previous} to 0."
            counter_values[previous] = 0
         end
      end

      if previous_snapshot.present?
         log_range = Log.where(:id => previous_snapshot.log_id+1..snapshot.log_id)

         counter_values["NMB_D"] = log_range.where(:code => lookup_codes('uc1.aanvraag_account_gelukt')).count
         counter_values["NMB_B"] = log_range.where(:code => lookup_codes('uc1.aanvraag_account_balie_gelukt')).count
         counter_values["NMB_Adm"] = log_range.where(:code => lookup_codes(nil, 'uc19.testaccount_aanmaken_gelukt')).count
         counter_values["NH"] = log_range.where(:code => lookup_codes('uc6.herstellen_wachtwoord_brief_gelukt')).count
         counter_values["NS"] = log_range.where(:code => lookup_codes('cronjobs.brief_genereren_gelukt')).count
         # QQQ uc1.Oude aanvraag opgeheven brief nog niet verstuurd => bestaat niet => 0
         counter_values["NV"] = log_range.where(:code => lookup_codes('uc5.uitbreidingsaanvraag_intrekken_gelukt')).count + 0
         counter_values["NAanD"] = log_range.where(:code => lookup_codes(['uc1.heraanvraag_account_gelukt', 'uc1.aanvraag_account_gelukt', 'uc1.aanvraag_account_balie_gelukt', 'uc1.aanvraag_buitenland_heraanvraag_gelukt'])).count
         # Aantal succesvolle aanvragen (nieuw, her-) via de Admin interface mbv AanvragenSectorAccount
         # De Admin interface voor het uitbreiden met SMS ontbreekt in 4.01;
         # de Admin interface voor het aanmaken van Testaccounts maakt direct
         # geactiveerde accounts aan waardoor deze geen aanvraag status hebben.
         counter_values["NAct"] = log_range.where(:code => lookup_codes('uc3.activeren_gelukt')).count
         counter_values["NVerl"] = log_range.where(:code => lookup_codes('cronjobs.aanvraaggegevens_opschonen_gelukt')).count
         # QQQ uc1.aanvraag_opgeheven_brief_niet_verstuurd => bestaat niet => 0
         counter_values["NIng"] = log_range.where(:code => lookup_codes('uc1.aanvraag_vervangen')).count + 0
         counter_values["NOph"] = log_range.where(:code => lookup_codes('uc5.opheffen_digid_gelukt')).count
         counter_values["NVervang"] = log_range.where(:code => lookup_codes('uc3.activeren_gelukt_bestaande_opgeheven')).count
         counter_values["NVerloop"] = log_range.where(:code => lookup_codes('cronjobs.account_status_vervallen_gelukt')).count
         counter_values["NDelBeh"] = log_range.where(:code => lookup_codes(nil, 'uc16.account_verwijderen_gelukt')).count
         counter_values["NCrAdm"] = counter_values["NMB_Adm"]
         # QQQ Not possible? So 0.
         counter_values["NCrBeh"] = 0
         counter_values["NDelAdm"] = log_range.where(:code => lookup_codes(nil, 'uc20.testaccount_revoceren_gelukt')).count
         counter_values["NOpsAan"] = log_range.where(:code => lookup_codes(nil, 'uc16.account_wijzigen_gelukt')).count
         counter_values["NOpsUit"] = log_range.where(:code => lookup_codes(nil, 'uc16.account_opschorten_ongedaan_maken_gelukt')).count
         counter_values["NOpsDelBeh"] = log_range.where(:code => lookup_codes(nil, 'uc16.account_verwijderen_gelukt')).count
         counter_values["NOpsDelAdm"] = counter_values["NDelAdm"]
      else
         logger.warn "No previous snapshot, no log based counters could be determined for #{snapshot.id}."
      end

      ActiveRecord::Base.transaction do
         counter_values.each do |key, value|
            counter = SnapshotCounter.create :snapshot_id => snapshot.id, :name => key, :value => value, :created_at => Time.now, :updated_at => Time.now
            counters << counter
         end
      end

      counters
   end

   def self.cleanup_snapshots()
      # This runs on the accounts slave instance which also hosts snapshot (local slave)
      # QQQ Assume that DROP DATABASE stmts don't cause replication problems when run from a replicated db context.
      connection = AccountSnapshotBase.dyn_connection nil
      connection.execute("SHOW DATABASES").each do |db|
         snap = Snapshot.find_by_database_name(db)
         if snap.present?  && snap.created_at < 7.days.ago # QQQ 7=>configurable!
            quoted_database_name = connection.quote_table_name(snap.database_name)
            logger.info "Dropping snapshot database: #{quoted_database_name}."
            connection.execute("DROP DATABASE #{quoted_database_name}")
         end
      end
      #QQQ Cleanup old snapshot info + counters? For now leave 'em
   end
end
