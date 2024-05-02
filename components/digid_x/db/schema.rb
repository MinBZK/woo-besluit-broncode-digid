
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

ActiveRecord::Schema.define(version: 2023_02_07_222337) do

  create_table "account_histories", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.integer "account_id"
    t.string "gebruikersnaam"
    t.string "mobiel_nummer"
    t.string "email_adres"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["account_id"], name: "index_account_histories_on_account_id"
    t.index ["email_adres"], name: "index_account_histories_on_email_adres"
    t.index ["gebruikersnaam"], name: "index_account_histories_on_gebruikersnaam"
    t.index ["mobiel_nummer"], name: "index_account_histories_on_mobiel_nummer"
  end

  create_table "accounts", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.string "status"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.datetime "current_sign_in_at"
    t.datetime "last_sign_in_at"
    t.integer "zekerheidsniveau"
    t.datetime "blocked_till"
    t.integer "herkomst"
    t.integer "weak_password_skip_count", default: 0
    t.datetime "blocking_data_timestamp_first_failed_login_attempt"
    t.string "locale", default: "nl"
    t.string "issuer_type", default: "regulier"
    t.datetime "reason_suspension_updated_at"
    t.string "reason_suspension"
    t.datetime "email_requested"
    t.datetime "last_change_security_level_at"
    t.boolean "replace_account_blocked", default: false
    t.index ["updated_at"], name: "index_accounts_on_updated_at"
  end

  create_table "activation_letter_files", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.text "filename"
    t.text "xml_content", size: :medium
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.string "filename_csv"
    t.string "csv_content"
    t.string "status"
    t.string "processed_file"
    t.text "processed_xml"
  end

  create_table "activation_letters", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.integer "registration_id"
    t.text "gba"
    t.string "letter_type"
    t.string "status"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.string "controle_code"
    t.integer "geldigheidsduur"
    t.string "postcode"
    t.boolean "aangetekend", default: false
    t.index ["letter_type"], name: "index_activation_letters_on_letter_type"
    t.index ["registration_id"], name: "index_activation_letters_on_registration_id"
  end

  create_table "alarm_notifications", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.string "name"
    t.string "description"
    t.string "identifier"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["name"], name: "index_alarm_notifications_on_name", unique: true
  end

  create_table "alarm_notifications_managers", id: false, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.integer "manager_id"
    t.integer "alarm_notification_id"
    t.index ["alarm_notification_id"], name: "fk_rails_5374866604"
    t.index ["manager_id"], name: "fk_rails_294ea74f4f"
  end

  create_table "app_authenticators", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.integer "account_id", null: false
    t.string "user_app_id", null: false
    t.string "status", null: false
    t.string "device_name"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.datetime "last_sign_in_at"
    t.datetime "activated_at"
    t.string "instance_id"
    t.string "user_app_public_key"
    t.string "symmetric_key"
    t.string "masked_pin"
    t.datetime "substantieel_activated_at"
    t.boolean "hardware_support"
    t.boolean "nfc_support"
    t.string "substantieel_document_type"
    t.string "issuer_type"
    t.timestamp "requested_at"
    t.string "activation_code"
    t.string "geldigheidstermijn"
    t.string "pip", limit: 1023
    t.string "signature_of_pip"
    t.datetime "wid_activated_at"
    t.string "wid_document_type"
    t.index ["account_id"], name: "fk_rails_08083aaf19"
    t.index ["instance_id"], name: "index_app_authenticators_on_instance_id"
    t.index ["issuer_type"], name: "index_app_authenticators_on_issuer_type"
    t.index ["user_app_id"], name: "index_app_authenticators_on_user_app_id", unique: true
  end

  create_table "app_versions", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.string "operating_system", null: false
    t.string "version", null: false
    t.date "not_valid_before", null: false
    t.date "not_valid_on_or_after"
    t.date "kill_app_on_or_after"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.datetime "four_eyes_updated_at"
    t.string "release_type", null: false
    t.index ["version", "operating_system", "release_type"], name: "index_app_versions_on_version_and_os_and_release", unique: true
  end

  create_table "aselect_certificates", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.integer "aselect_webservice_id"
    t.string "distinguished_name"
    t.text "cached_certificate"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["aselect_webservice_id"], name: "index_aselect_certificates_on_aselect_webservice_id"
    t.index ["distinguished_name"], name: "index_aselect_certificates_on_distinguished_name"
  end

  create_table "aselect_requests", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.integer "session_id"
    t.string "aselect_type"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["session_id"], name: "index_aselect_requests_on_session_id"
    t.index ["updated_at"], name: "index_aselect_requests_on_updated_at"
  end

  create_table "aselect_sessions", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.string "rid"
    t.text "app_url"
    t.string "app_id"
    t.integer "webservice_id"
    t.string "aselect_credentials"
    t.datetime "verify_before"
    t.boolean "verified", default: false
    t.integer "account_id"
    t.string "sector_code"
    t.string "sectoraal_nummer"
    t.integer "betrouwbaarheidsniveau"
    t.string "result_code"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["account_id"], name: "index_aselect_sessions_on_account_id"
    t.index ["aselect_credentials", "verify_before", "verified"], name: "checked_credentials"
    t.index ["aselect_credentials"], name: "index_aselect_sessions_on_aselect_credentials"
    t.index ["rid"], name: "index_aselect_sessions_on_rid"
    t.index ["updated_at"], name: "index_aselect_sessions_on_updated_at"
    t.index ["webservice_id"], name: "index_aselect_sessions_on_webservice_id"
  end

  create_table "aselect_shared_secrets", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.integer "aselect_webservice_id"
    t.string "shared_secret"
    t.boolean "confirmed"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["aselect_webservice_id"], name: "index_aselect_shared_secrets_on_aselect_webservice_id"
    t.index ["shared_secret"], name: "index_aselect_shared_secrets_on_shared_secret"
  end

  create_table "aselect_webservices", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.string "app_id"
    t.integer "webservice_id"
    t.integer "assurance_level"
    t.boolean "active"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["webservice_id"], name: "index_aselect_webservices_on_webservice_id", unique: true
  end

  create_table "at_requests", options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.string "status", null: false
    t.string "document_type", null: false
    t.string "authorization", null: false
    t.string "sequence_no", null: false
    t.string "reference"
    t.binary "raw", null: false
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.integer "created_by_id", null: false
    t.integer "approved_by_id"
    t.datetime "approved_at"
    t.integer "sent_by_id"
    t.datetime "sent_at"
    t.index ["document_type", "sequence_no"], name: "index_at_requests_on_document_type_and_sequence_no", unique: true
  end

  create_table "attempts", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.string "attemptable_type"
    t.integer "attemptable_id"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.string "attempt_type"
    t.index ["attemptable_id"], name: "index_attempts_on_attemptable_id"
    t.index ["attemptable_type"], name: "index_attempts_on_attemptable_type"
  end

  create_table "beveiligde_bezorging_postcodes", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.string "postcode_gebied"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["postcode_gebied"], name: "index_beveiligde_bezorging_postcodes_on_postcode_gebied", unique: true
  end

  create_table "blacklisted_phone_numbers", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.string "prefix", null: false
    t.string "description", null: false
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["prefix"], name: "index_blacklisted_phone_numbers_on_prefix", unique: true
  end

  create_table "bulk_order_bsns", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.integer "bulk_order_id"
    t.string "bsn"
    t.integer "status"
    t.text "gba_data"
    t.boolean "gba_timeout"
    t.index ["bulk_order_id"], name: "index_bulk_order_bsns_on_bulk_order_id"
  end

  create_table "bulk_orders", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.string "name", null: false
    t.integer "account_status_scope", default: 0, null: false
    t.integer "status", default: 0, null: false
    t.datetime "status_updated_at"
    t.integer "manager_id", null: false
    t.datetime "approved_at"
    t.integer "approval_manager_id"
    t.datetime "rejected_at"
    t.integer "reject_manager_id"
    t.datetime "order_started_at"
    t.datetime "order_finished_at"
    t.datetime "brp_started_at"
    t.datetime "finalized_at"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.integer "bulk_order_bsns_count"
    t.datetime "brp_last_run_at"
    t.string "bulk_type", null: false
    t.index ["approval_manager_id"], name: "fk_rails_785a382b81"
    t.index ["manager_id"], name: "fk_rails_b6179a085e"
    t.index ["reject_manager_id"], name: "fk_rails_4566c25c25"
  end

  create_table "certificates", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.integer "webservice_id"
    t.string "distinguished_name"
    t.text "cached_certificate"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.string "fingerprint"
    t.datetime "not_after"
    t.datetime "not_before"
    t.string "cert_type", default: "TLS"
    t.index ["distinguished_name", "cert_type"], name: "index_certificates_on_distinguished_name_and_cert_type", unique: true
    t.index ["fingerprint"], name: "index_certificates_on_fingerprint"
    t.index ["webservice_id"], name: "index_certificates_on_webservice_id"
  end

  create_table "distribution_entities", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.integer "account_id"
    t.integer "balie_id"
    t.string "status"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.string "functie_scheiding_type"
    t.index ["account_id"], name: "index_distribution_entities_on_account_id", unique: true
  end

  create_table "email_deliveries", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.integer "account_id"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.datetime "scheduled"
    t.index ["account_id"], name: "index_email_deliveries_on_account_id"
    t.index ["scheduled"], name: "index_email_deliveries_on_scheduled"
  end

  create_table "emails", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.integer "account_id"
    t.string "adres"
    t.string "controle_code"
    t.string "status"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.datetime "confirmed_at"
    t.index ["account_id"], name: "index_emails_on_account_id", unique: true
  end

  create_table "external_codes", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.integer "sector_id", null: false
    t.string "sectoraalnummer", null: false
    t.string "external_code", null: false
    t.index ["sector_id"], name: "index_external_codes_on_sector_id"
    t.index ["sectoraalnummer"], name: "index_external_codes_on_sectoraalnummer"
  end

  create_table "flyway_schema_history", primary_key: "installed_rank", id: :integer, default: nil, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci", force: :cascade do |t|
    t.string "version", limit: 50
    t.string "description", limit: 200, null: false
    t.string "type", limit: 20, null: false
    t.string "script", limit: 1000, null: false
    t.integer "checksum"
    t.string "installed_by", limit: 100, null: false
    t.timestamp "installed_on", default: -> { "CURRENT_TIMESTAMP" }, null: false
    t.integer "execution_time", null: false
    t.boolean "success", null: false
    t.index ["success"], name: "flyway_schema_history_s_idx"
  end

  create_table "four_eyes_reports", options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci", force: :cascade do |t|
    t.integer "manager_id", null: false
    t.integer "creator_manager_id", null: false
    t.integer "acceptor_manager_id", null: false
    t.datetime "changed_at", null: false
    t.string "description"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
  end

  create_table "four_eyes_reviews", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.integer "record_id"
    t.string "record_table"
    t.string "action"
    t.integer "manager_id"
    t.text "serialized_object", size: :medium
    t.datetime "created_at"
    t.datetime "updated_at"
    t.string "uniq_hashes"
    t.string "note"
    t.index ["manager_id"], name: "index_four_eyes_reviews_on_manager_id"
    t.index ["record_id", "record_table"], name: "index_four_eyes_reviews_on_record_id_and_record_table"
  end

  create_table "gba_blocks", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.string "blocked_data"
    t.datetime "blocked_till"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["blocked_data"], name: "index_gba_blocks_on_blocked_data"
  end

  create_table "historical_reports", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.string "base_name"
    t.text "report_data"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.timestamp "counted_till"
  end

  create_table "id_check_documents", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci", force: :cascade do |t|
    t.string "document_number", null: false
    t.string "document_type", null: false
    t.integer "account_id", null: false
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.string "user_app_id", null: false
  end

  create_table "kiosks", options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci", force: :cascade do |t|
    t.string "kiosk_id", null: false
    t.string "status"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.string "naam"
    t.string "adres"
    t.string "woonplaats"
    t.string "postcode"
    t.datetime "four_eyes_updated_at"
  end

  create_table "manager_warnings", options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci", force: :cascade do |t|
    t.string "name"
    t.text "description"
    t.text "data"
    t.boolean "active", default: false
  end

  create_table "managers", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.string "name"
    t.string "surname"
    t.string "email"
    t.string "mobile_number"
    t.boolean "active", default: false, null: false
    t.boolean "notify_sms", default: false, null: false
    t.boolean "notify_email", default: false, null: false
    t.datetime "current_sign_in_at"
    t.datetime "last_sign_in_at"
    t.boolean "superuser", default: false, null: false
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.string "account_name", null: false
    t.datetime "inactive_at"
    t.datetime "session_time"
    t.string "distinguished_name"
    t.string "last_accounts"
    t.string "last_webservices"
    t.datetime "four_eyes_updated_at"
    t.string "fingerprint"
    t.index ["account_name"], name: "index_managers_on_account_name", unique: true
    t.index ["distinguished_name"], name: "index_managers_on_distinguished_name"
    t.index ["fingerprint"], name: "index_managers_on_fingerprint"
  end

  create_table "managers_roles", id: false, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.integer "manager_id"
    t.integer "role_id"
    t.index ["manager_id"], name: "index_managers_roles_on_manager_id"
    t.index ["role_id"], name: "index_managers_roles_on_role_id"
  end

  create_table "nationalities", options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci", force: :cascade do |t|
    t.integer "nationalitycode"
    t.string "description_nl"
    t.boolean "eer", default: false
    t.date "start_date"
    t.date "end_date"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.string "description_en"
    t.integer "position"
  end

  create_table "news_items", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.string "name_nl"
    t.text "body_nl"
    t.boolean "active", default: false
    t.datetime "active_from"
    t.datetime "active_until"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.string "name_en"
    t.text "body_en"
    t.string "os"
    t.string "browser"
  end

  create_table "news_items_news_locations", id: false, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.integer "news_location_id"
    t.integer "news_item_id"
    t.index ["news_item_id"], name: "index_news_items_news_locations_on_news_item_id"
    t.index ["news_location_id"], name: "index_news_items_news_locations_on_news_location_id"
  end

  create_table "news_locations", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.string "name"
    t.string "relative_path"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["relative_path"], name: "index_news_locations_on_relative_path", unique: true
  end

  create_table "organizations", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.string "name"
    t.text "description"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["name"], name: "index_organizations_on_name", unique: true
  end

  create_table "password_tools", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.integer "account_id", null: false
    t.string "username"
    t.string "status"
    t.string "activation_code"
    t.string "geldigheidstermijn"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.string "encrypted_password"
    t.string "password_salt"
    t.integer "policy", default: 0
    t.string "issuer_type"
    t.index ["account_id"], name: "index_password_tools_on_account_id", unique: true
    t.index ["issuer_type"], name: "index_password_tools_on_issuer_type"
    t.index ["username"], name: "index_password_tools_on_username"
  end

  create_table "permissions", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.string "name"
    t.string "identifier"
  end

  create_table "permissions_roles", id: false, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.integer "permission_id"
    t.integer "role_id"
    t.index ["permission_id"], name: "index_permissions_roles_on_permission_id"
    t.index ["role_id"], name: "index_permissions_roles_on_role_id"
  end

  create_table "questions", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.string "page", null: false
    t.integer "position", null: false
    t.string "question", null: false
    t.text "answer", null: false
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.string "locale", default: "nl"
    t.index ["page", "position"], name: "index_questions_on_page_and_position"
  end

  create_table "recovery_attempts", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.text "attempt"
    t.text "attempt_type"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
  end

  create_table "recovery_codes", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.integer "account_id"
    t.boolean "used"
    t.text "herstelcode"
    t.text "geldigheidstermijn"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.text "bearer"
    t.boolean "via_brief"
    t.index ["account_id"], name: "index_recovery_codes_on_account_id"
    t.index ["herstelcode"], name: "index_recovery_codes_on_herstelcode", length: 8
  end

  create_table "registrations", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.string "burgerservicenummer"
    t.string "geboortedatum"
    t.string "postcode"
    t.string "huisnummer"
    t.string "huisnummertoevoeging"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.string "gba_status"
    t.string "status"
    t.integer "webservice_id"
    t.string "baliecode"
    t.string "balie_payload"
    t.string "id_number"
    t.date "valid_until"
    t.integer "nationality_id"
    t.index ["burgerservicenummer"], name: "index_registrations_on_burgerservicenummer"
    t.index ["webservice_id"], name: "index_registrations_on_webservice_id"
  end

  create_table "roles", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.string "name"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.datetime "four_eyes_updated_at"
    t.index ["name"], name: "index_roles_on_name", unique: true
  end

  create_table "saml_federations", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.string "federation_name"
    t.string "session_key"
    t.string "authn_context_level"
    t.string "subject"
    t.string "address"
    t.datetime "created_at"
    t.datetime "updated_at"
    t.boolean "allow_sso"
    t.integer "sso_domain_id"
    t.integer "account_id"
    t.index ["account_id"], name: "index_saml_federations_on_account_id"
    t.index ["session_key", "federation_name"], name: "index_saml_federations_on_session_key_and_federation_name"
    t.index ["updated_at"], name: "index_saml_federations_on_updated_at"
  end

  create_table "saml_providers", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.string "entity_id"
    t.boolean "allow_sso"
    t.string "metadata_url"
    t.text "cached_metadata"
    t.integer "sso_domain_id"
    t.datetime "created_at"
    t.datetime "updated_at"
    t.integer "webservice_id"
    t.index ["entity_id"], name: "index_saml_providers_on_entity_id"
    t.index ["webservice_id"], name: "index_saml_providers_on_webservice_id", unique: true
  end

  create_table "saml_sp_sessions", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.integer "federation_id"
    t.integer "provider_id"
    t.text "authn_request_xml"
    t.string "artifact"
    t.datetime "resolve_before"
    t.datetime "created_at"
    t.datetime "updated_at"
    t.string "status_code"
    t.string "substatus_code"
    t.boolean "active"
    t.string "session_key"
    t.index ["artifact"], name: "index_saml_sp_sessions_on_artifact"
    t.index ["federation_id"], name: "index_saml_sp_sessions_on_federation_id"
    t.index ["provider_id"], name: "index_saml_sp_sessions_on_provider_id"
    t.index ["session_key", "provider_id"], name: "index_saml_sp_sessions_on_session_key_and_provider_id"
    t.index ["updated_at"], name: "index_saml_sp_sessions_on_updated_at"
  end

  create_table "saml_sso_domains", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.string "name"
    t.text "description"
    t.integer "session_time", default: 15
    t.integer "grace_period_time", default: 15
    t.boolean "show_current_session_screen"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
  end

  create_table "saml_wid_extensions", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.string "document_type", null: false
    t.string "artifact", null: false
    t.text "data"
    t.datetime "resolve_before", null: false
    t.datetime "created_at"
    t.datetime "updated_at"
    t.index ["artifact"], name: "index_saml_wid_extensions_on_artifact", unique: true
  end

  create_table "scheduler_blocks", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.string "name"
    t.string "key"
    t.integer "serial"
  end

  create_table "sector_authentications", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.integer "webservice_id"
    t.integer "sector_id"
    t.integer "position"
    t.boolean "test"
    t.boolean "revocation"
    t.boolean "registration"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["sector_id"], name: "index_sector_authentications_on_sector_id"
    t.index ["webservice_id"], name: "index_sector_authentications_on_webservice_id"
  end

  create_table "sectorcodes", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.integer "account_id"
    t.integer "sector_id"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.string "sectoraalnummer"
    t.index ["account_id"], name: "index_sectorcodes_on_account_id"
    t.index ["sector_id"], name: "index_sectorcodes_on_sector_id"
    t.index ["sectoraalnummer"], name: "index_sectorcodes_on_sectoraalnummer"
  end

  create_table "sectorcodes_histories", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.integer "account_id"
    t.integer "sector_id"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.string "sectoraalnummer"
    t.index ["account_id"], name: "index_sectorcodes_on_account_id"
    t.index ["sector_id"], name: "index_sectorcodes_on_sector_id"
    t.index ["sectoraalnummer"], name: "index_sectorcodes_on_sectoraalnummer"
  end

  create_table "sectors", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.string "name"
    t.string "number_name"
    t.boolean "active", default: false
    t.boolean "test", default: false
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.integer "expiration_time"
    t.integer "expire_time"
    t.integer "valid_for"
    t.integer "warn_before"
    t.string "pretty_name"
    t.index ["name"], name: "index_sectors_on_name", unique: true
    t.index ["number_name"], name: "index_sectors_on_number_name", unique: true
  end

  create_table "sent_emails", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.integer "account_id"
    t.string "reason"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["account_id"], name: "index_sent_emails_on_account_id"
  end

  create_table "shared_secrets", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.integer "aselect_webservice_id"
    t.string "shared_secret"
    t.boolean "confirmed"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["aselect_webservice_id"], name: "index_shared_secrets_on_aselect_webservice_id"
  end

  create_table "sms_challenges", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.integer "account_id"
    t.string "code"
    t.string "status"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.integer "attempt", default: 0
    t.string "action"
    t.string "mobile_number"
    t.integer "lock_version"
    t.integer "webservice_id"
    t.boolean "spoken", default: false
    t.index ["account_id"], name: "index_sms_challenges_on_account_id"
    t.index ["spoken", "account_id"], name: "index_sms_challenges_on_spoken_and_account_id"
    t.index ["status", "mobile_number"], name: "status_mobile_number_index"
  end

  create_table "sms_fail_overs", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.integer "time_block"
    t.integer "regular_failures"
    t.integer "spoken_failures"
    t.integer "regular_active_time_block"
    t.integer "spoken_active_time_block"
    t.string "gateway"
  end

  create_table "sms_tools", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.integer "account_id", null: false
    t.string "status"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.string "issuer_type", null: false
    t.string "activation_code"
    t.string "geldigheidstermijn"
    t.boolean "gesproken_sms"
    t.string "phone_number"
    t.datetime "activated_at"
    t.integer "times_changed", default: 0
    t.index ["account_id", "status"], name: "index_sms_tools_on_account_id_and_status", unique: true
    t.index ["issuer_type"], name: "index_sms_tools_on_issuer_type"
    t.index ["phone_number"], name: "index_sms_tools_on_phone_number"
  end

  create_table "ssl_certificates", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci", force: :cascade do |t|
    t.string "subject", null: false
    t.text "raw_certificate", null: false
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
  end

  create_table "ssl_sessions", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.string "ssl_session_id"
    t.datetime "blocked_until"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["ssl_session_id"], name: "index_ssl_sessions_on_ssl_session_id"
  end

  create_table "subscribers", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.string "bsn"
    t.integer "subscription_id"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["bsn"], name: "index_subscribers_on_bsn"
    t.index ["subscription_id"], name: "index_subscribers_on_subscription_id"
  end

  create_table "subscriptions", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.string "name"
    t.string "description"
    t.string "type"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
  end

  create_table "switches", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.string "name"
    t.string "description", limit: 1024
    t.integer "status", limit: 1, default: 0, null: false
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.integer "pilot_group_id"
    t.index ["name"], name: "index_switches_on_name", unique: true
  end

  create_table "third_party_apps", options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci", force: :cascade do |t|
    t.string "user_agent"
    t.string "return_url"
    t.datetime "four_eyes_updated_at"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
  end

  create_table "user_ghosts", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.string "gebruikersnaam"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.datetime "blocked_till"
    t.datetime "blocking_data_timestamp_first_failed_login_attempt"
    t.index ["gebruikersnaam"], name: "index_user_ghosts_on_gebruikersnaam"
  end

  create_table "web_registrations", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.string "geldigheidsdsduur"
    t.string "taalvoorkeur"
    t.string "webadres"
    t.text "aanvraag"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.string "sectoraalnummer"
    t.string "aanvraagnummer"
    t.integer "sector_id"
    t.integer "webdienst_id"
    t.string "anummer"
    t.index ["sector_id"], name: "index_web_registrations_on_sector_id"
  end

  create_table "webservices", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.string "name"
    t.text "description"
    t.integer "organization_id"
    t.boolean "active", default: false
    t.datetime "active_from"
    t.datetime "active_until"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.string "authentication_method", default: ""
    t.string "website_url"
    t.string "redirect_url_domain", default: ""
    t.boolean "check_redirect_url", default: true
    t.boolean "substantieel_active", default: true
    t.datetime "four_eyes_updated_at"
    t.boolean "app_to_app", default: false, null: false
    t.string "assurance_from"
    t.string "assurance_to"
    t.datetime "assurance_date"
    t.string "app_to_app_return_url"
    t.text "apps"
    t.boolean "cluster", default: false
    t.string "init_authn_url"
    t.boolean "show_in_app", default: false
    t.index ["organization_id"], name: "index_webservices_on_organization_id"
  end

  create_table "whitelisted_phone_numbers", options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci", force: :cascade do |t|
    t.string "phone_number"
    t.string "description"
    t.datetime "created_at", precision: 6, null: false
    t.datetime "updated_at", precision: 6, null: false
    t.datetime "four_eyes_updated_at"
  end

  create_table "wids", options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.string "card_type"
    t.string "sequence_no"
    t.datetime "blocked_till"
    t.datetime "blocking_data_timestamp_first_failed_login_attempt"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.string "unblock_code"
    t.string "action"
    t.integer "registration_id"
    t.index ["registration_id"], name: "index_wids_on_registration_id", unique: true
  end

  add_foreign_key "alarm_notifications_managers", "alarm_notifications"
  add_foreign_key "alarm_notifications_managers", "managers"
  add_foreign_key "app_authenticators", "accounts", on_delete: :cascade
  add_foreign_key "bulk_order_bsns", "bulk_orders"
  add_foreign_key "bulk_orders", "managers"
  add_foreign_key "bulk_orders", "managers", column: "approval_manager_id"
  add_foreign_key "bulk_orders", "managers", column: "reject_manager_id"
  add_foreign_key "saml_sp_sessions", "saml_federations", column: "federation_id", on_delete: :cascade
  add_foreign_key "subscribers", "subscriptions"
end
