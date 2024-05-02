
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

# This file is auto-generated from the current state of the database. Instead
# of editing this file, please use the migrations feature of Active Record to
# incrementally modify your database, and then regenerate this schema definition.
#
# This file is the source Rails uses to define your schema when running `rails
# db:schema:load`. When creating a new database, `rails db:schema:load` tends to
# be faster and is potentially less error prone than running all of your
# migrations from scratch. Old migrations may fail to apply correctly if those
# migrations use external dependencies or application code.
#
# It's strongly recommended that you check this file into your version control system.

ActiveRecord::Schema.define(version: 2017_08_29_074023) do

  create_table "account_logs", id: :bigint, unsigned: true, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci", force: :cascade do |t|
    t.integer "account_id", null: false
    t.string "name", null: false
    t.string "ip_address", null: false
    t.integer "code", null: false
    t.datetime "created_at", null: false
    t.index ["account_id", "created_at"], name: "index_account_logs_on_account_id_and_created_at"
  end

  create_table "admin_reports", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci", force: :cascade do |t|
    t.integer "report_id"
    t.string "name"
    t.string "report_type"
    t.datetime "interval_start"
    t.datetime "interval_end"
    t.text "csv_content", size: :long
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.integer "manager_id"
    t.datetime "batch_started_at"
    t.datetime "report_started_at"
    t.datetime "report_ended_at"
    t.boolean "result"
    t.integer "lines"
    t.index ["manager_id"], name: "index_admin_reports_on_manager_id"
    t.index ["report_id", "report_type"], name: "index_admin_reports_on_report_id_and_report_type"
  end

  create_table "logs", id: :bigint, unsigned: true, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci", force: :cascade do |t|
    t.string "name", null: false
    t.integer "code", null: false
    t.string "ip_address", null: false
    t.integer "account_id"
    t.integer "webservice_id"
    t.integer "manager_id"
    t.integer "sector_number"
    t.string "transaction_id"
    t.string "session_id"
    t.datetime "created_at"
    t.string "subject_type"
    t.integer "subject_id"
    t.string "sector_name"
    t.string "authentication_level"
    t.string "pseudoniem"
    t.index ["account_id"], name: "index_logs_on_account_id"
    t.index ["code"], name: "index_logs_on_code"
    t.index ["created_at"], name: "index_logs_on_created_at"
    t.index ["manager_id"], name: "index_logs_on_manager_id"
    t.index ["sector_number"], name: "index_logs_on_sector_number"
    t.index ["subject_id", "subject_type"], name: "index_logs_on_subject_id_and_subject_type"
    t.index ["transaction_id"], name: "index_logs_on_transaction_id"
    t.index ["webservice_id"], name: "index_logs_on_webservice_id"
  end

  create_table "manager_logs", id: :bigint, unsigned: true, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci", force: :cascade do |t|
    t.string "name", null: false
    t.integer "code", null: false
    t.string "ip_address", null: false
    t.integer "account_id"
    t.integer "webservice_id"
    t.integer "manager_id"
    t.integer "sector_number"
    t.string "transaction_id"
    t.string "session_id"
    t.datetime "created_at"
    t.string "subject_type"
    t.integer "subject_id"
    t.string "sector_name"
    t.string "authentication_level"
    t.string "pseudoniem"
    t.index ["account_id"], name: "index_manager_logs_on_account_id"
    t.index ["code"], name: "index_manager_logs_on_code"
    t.index ["created_at"], name: "index_manager_logs_on_created_at"
    t.index ["id"], name: "id", unique: true
    t.index ["manager_id"], name: "index_manager_logs_on_manager_id"
    t.index ["sector_number"], name: "index_manager_logs_on_sector_number"
    t.index ["subject_id", "subject_type"], name: "index_manager_logs_on_subject_id_and_subject_type"
    t.index ["transaction_id"], name: "index_manager_logs_on_transaction_id"
    t.index ["webservice_id"], name: "index_manager_logs_on_webservice_id"
  end

  create_table "metrics", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci", force: :cascade do |t|
    t.string "name"
    t.integer "average", default: 0
    t.integer "total", default: 0
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["name"], name: "index_metrics_on_name"
  end

  create_table "snapshot_counters", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci", force: :cascade do |t|
    t.integer "counter_id"
    t.integer "snapshot_id"
    t.string "name"
    t.integer "value"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["snapshot_id"], name: "index_snapshot_counters_on_snapshot_id"
  end

  create_table "snapshots", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci", force: :cascade do |t|
    t.integer "snapshot_id"
    t.string "database_name"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.bigint "log_id"
    t.index ["log_id"], name: "index_snapshots_on_log_id"
    t.index ["snapshot_id"], name: "index_snapshots_on_snapshot_id"
  end

end
