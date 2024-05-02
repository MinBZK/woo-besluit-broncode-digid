
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

ActiveRecord::Schema.define(version: 2020_05_15_105058) do

  create_table "audits", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.integer "verification_id"
    t.string "state"
    t.text "motivation"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.string "user_id"
    t.boolean "verification_correct"
    t.index ["user_id"], name: "index_audits_on_user_id"
    t.index ["verification_id"], name: "index_audits_on_verification_id"
  end

  create_table "front_desks", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.string "name"
    t.string "kvk_number"
    t.string "establishment_number"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.string "location"
    t.integer "alarm_unchecked_accounts"
    t.integer "alarm_fraud_suspension"
    t.integer "max_issues"
    t.string "code"
    t.boolean "blocked", default: false, null: false
    t.string "time_zone", default: "Europe/Amsterdam"
  end

  create_table "users", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.string "pseudonym"
    t.integer "front_desk_id"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["front_desk_id"], name: "index_users_on_front_desk_id"
  end

  create_table "verification_corrections", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.integer "verification_id"
    t.string "original_id_number"
    t.date "original_id_expires_at"
    t.text "motivation"
    t.index ["verification_id"], name: "index_verification_corrections_on_verification_id"
  end

  create_table "verifications", id: :integer, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC", force: :cascade do |t|
    t.string "citizen_service_number"
    t.string "id_number"
    t.date "id_expires_at"
    t.boolean "id_established"
    t.boolean "suspected_fraud"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.string "front_desk_code"
    t.string "activation_code"
    t.string "state", null: false
    t.string "salutation"
    t.string "full_name"
    t.date "activation_code_end_date"
    t.string "first_names"
    t.string "surname"
    t.string "birthday"
    t.integer "front_desk_id"
    t.integer "user_id"
    t.integer "front_desk_registration_id"
    t.datetime "activated_at"
    t.boolean "activated"
    t.integer "front_desk_account_id"
    t.boolean "id_signaled"
    t.datetime "front_desk_registration_created_at"
    t.date "front_desk_code_expires_at"
    t.string "locale", default: "nl"
    t.string "nationality", default: "Nederlandse"
    t.index ["front_desk_id"], name: "index_verifications_on_front_desk_id"
    t.index ["user_id"], name: "index_verifications_on_user_id"
  end

end
