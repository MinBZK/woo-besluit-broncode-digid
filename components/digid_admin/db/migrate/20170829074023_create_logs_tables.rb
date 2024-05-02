
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

class CreateLogsTables < ActiveRecord::Migration[4.2]
  def change
    create_table :logs, id: :bigint, unsigned: true do |t|
      t.string :name, null: false
      t.integer :code, null: false
      t.string :ip_address, null: false
      t.integer :account_id
      t.integer :webservice_id
      t.integer :manager_id
      t.integer :sector_number
      t.string :transaction_id
      t.string :session_id
      t.datetime :created_at
      t.string :subject_type
      t.integer :subject_id
      t.string :sector_name
      t.string :authentication_level
      t.string :pseudoniem

      t.index [:account_id], using: :btree
      t.index [:code], using: :btree
      t.index [:created_at], using: :btree
      t.index [:manager_id], using: :btree
      t.index [:sector_number], using: :btree
      t.index [:subject_id, :subject_type], using: :btree
      t.index [:transaction_id], using: :btree
      t.index [:webservice_id], using: :btree
    end

    create_table :account_logs, id: :bigint, unsigned: true do |t|
      t.integer :account_id, null: false
      t.string :name, null: false
      t.string :ip_address, null: false
      t.integer :code, null: false
      t.datetime :created_at, null: false

      t.index [:account_id, :created_at], using: :btree
    end

    create_table :manager_logs, id: :bigint, unsigned: true do |t|
      t.string :name, null: false
      t.integer :code, null: false
      t.string :ip_address, null: false
      t.integer :account_id
      t.integer :webservice_id
      t.integer :manager_id
      t.integer :sector_number
      t.string :transaction_id
      t.string :session_id
      t.datetime :created_at
      t.string :subject_type
      t.integer :subject_id
      t.string :sector_name
      t.string :authentication_level
      t.string :pseudoniem

      t.index [:account_id], using: :btree
      t.index [:code], using: :btree
      t.index [:created_at], using: :btree
      t.index [:id], name: "id", unique: true, using: :btree
      t.index [:manager_id], using: :btree
      t.index [:sector_number], using: :btree
      t.index [:subject_id, :subject_type], using: :btree
      t.index [:transaction_id], using: :btree
      t.index [:webservice_id], using: :btree
    end

    create_table :admin_reports do |t|
      t.integer  :report_id
      t.string   :name
      t.string   :report_type
      t.datetime :interval_start
      t.datetime :interval_end
      t.text     :csv_content, limit: 4294967295
      t.datetime :created_at, null: false
      t.datetime :updated_at, null: false
      t.integer  :manager_id
      t.datetime :batch_started_at
      t.datetime :report_started_at
      t.datetime :report_ended_at
      t.boolean  :result
      t.integer  :lines
      t.index [:manager_id], using: :btree
      t.index [:report_id, :report_type], using: :btree
    end

    create_table :metrics do |t|
      t.string :name
      t.integer :average, default: 0
      t.integer :total, default: 0
      t.datetime :created_at, null: false
      t.datetime :updated_at, null: false
      t.index [:name],  using: :btree
    end

    create_table :snapshot_counters do |t|
      t.integer :counter_id
      t.integer :snapshot_id
      t.string :name
      t.integer :value
      t.datetime :created_at, null: false
      t.datetime :updated_at, null: false
      t.index [:snapshot_id], using: :btree
    end

    create_table :snapshots do |t|
      t.integer :snapshot_id
      t.string :database_name
      t.datetime :created_at, null: false
      t.datetime :updated_at, null: false
      t.bigint :log_id
      t.index [:log_id], using: :btree
      t.index [:snapshot_id], using: :btree
    end
  end
end
