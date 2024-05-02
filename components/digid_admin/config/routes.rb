
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

# required for loading the module the first time when using multiple threads handling the [connections.upload_csv -> iapi.retrieve_legacy_ids] request
require_relative '../app/controllers/iapi/dc_controller.rb'

Rails.application.routes.draw do

  class Reviewable
     def initialize(defaults = {})
      @defaults = defaults
    end

    def call(mapper, options = {})
      controller = mapper.instance_variable_get("@scope").instance_variable_get("@hash")[:controller]
      mapper.collection do
        mapper.get "/review/:id" => "#{controller}#review", as: "review"
        mapper.get "/review/:id/edit" => "#{controller}#edit_review", as: "edit_review"
        mapper.put "/review/:id" => "#{controller}#update_review", as: "update_review"
        mapper.delete "/review/:id" => "#{controller}#withdraw_review", as: "withdraw_review"
        mapper.post "/review/:id/accept" => "#{controller}#accept_review", as: "accept_review"
        mapper.delete "/review/:id/reject" => "#{controller}#reject_review", as: "reject_review"
      end
    end
  end

  # MuninMailer Metrics
  get '/munin/status/:file' => 'munin#index', constraints: { file: /.*/ }
  post '/munin/alarm' => 'munin#alarm'
  get '/munin/:metric_name/:metric_type' => 'munin#show'
  get '/munin/:metric_name/:metric_type/config' => 'munin#configuration'

  # Microservices health
  resources :microservices, only: :index

  # Questions
  get '/questions/log_search' => "questions#log_search"
  resources :questions, only: [:index, :new, :create, :show, :edit, :update, :destroy] do
    get 'preview', on: :collection
  end

  resources :four_eyes_reviews, only: :index

  resources :pages, only: [:new, :create]

  resources :nationalities

  # Accounts
  resources :accounts, only: [:index, :show, :destroy] do
    match '/search', action: 'index', on: :collection, via: [:get, :post]
    post :convert_sofi, on: :member
    post :suspend, on: :member
    post :block_account_replace, on: :member, as: "block_replace"
    get :gba_request, on: :member
    get :last_activation_data, on: :member
    get :generate_pdf_letter, on: :member
    get :resend_letter_registered, on: :member
    get 'export_logs' => 'logs#export', on: :member, as: 'export_logs'
    get '/transactions/:id' => 'transactions#index', on: :collection, as: 'transactions'
    get :histories, on: :member
    get :sent_emails, on: :member

  end

  namespace :eid do
    resources 'at_requests', only: [:index, :show, :new, :create] do
      get :download, on: :member
      patch :approve, on: :member
      patch :reject, on: :member
      patch :send_email, on: :member
      patch :abort, on: :member
    end
    resources 'certificates', only: [:index, :show, :create] do
      get :download, on: :member
    end
    resources 'crl', only: [:index, :show, :create] do
      get :download, on: :member
    end

    get '/view_wids/:id', action: 'view_wids', as: 'view_wids'
    post '/block_wid/:document_type/:id/:seq_nr', action: 'block_wid', as: 'block_wid'
    post '/pin_wid/:document_type/:id/:seq_nr', action: 'pin_wid', as: 'pin_wid'
    post '/deblock_wid/:document_type/:id/:seq_nr', action: 'deblock_wid', as: 'deblock_wid'
  end

  namespace :rda do
    resources 'certificates', only: [:index, :show, :create] do
      get :download, on: :member
    end
    resources 'crl', only: [:index, :show, :create] do
      get :download, on: :member
    end
  end

  resources :admin_reports, only: [:index, :show] do
    post :alarm, on: :collection
    get 'download_bundle', on: :collection
    get :overview, on: :collection
  end

  resources :afmeldlijsten, only: [:index] do
    resources :afmeldingen, only: [:new, :create, :destroy]
  end

  resources :beveiligde_bezorging_postcodes, only: [:index, :create, :destroy]

  get 'omgevingen' => "omgevingen#index", as: "omgevingen"

  resources :logs, only: [:index, :create] do
    match '/search', action: 'search', on: :collection, via: [:get, :post]
    get '/transactions/:id' => 'logs#transactions', on: :collection, as: 'transactions'
  end

  resources 'four_eyes_reports', only: [:index] do
    match '/search', action: 'search', on: :collection, via: [:get, :post]
    get '/export', action: 'export', on: :collection
  end

  resources :certificates, only: [:show, :index] do
    get 'white_list', on: :collection
    get 'fingerprints', on: :collection
  end

  resources :news_items
  resources :sso_domains
  resources :sectors
  resources :saml_providers, only: [:update] do
    get 'download_metadata', on: :member
    get 'edit_metadata', on: :member
    get 'show_metadata', on: :member
  end

  concern :reviewable, Reviewable.new
  concern :searchable do
    collection do
      get :search
      post :search
    end
  end

  namespace :dc do
    resources :organizations, concerns: [:searchable, :reviewable]
    post "upload_csv", to: "organizations#upload_csv"

    resources :local_metadata_process_results, only: [:show] do
      member do
        get :metadata
      end
    end

    resources :connections, concerns: [:searchable, :reviewable] do
      member do
        get :fetch_metadata
      end
      collection do
        post :upload_csv
      end

      resources :services, only: [:new]
    end

    resources :services, concerns: [:searchable, :reviewable], except: [:new]
    resources :certificates, only: [:index], concerns: [:searchable]
  end

  resources :webservices, concerns: :reviewable do
    get '/authorize_sector/:sector_id' => 'webservices#authorize_sector', on: :collection, as: 'authorize_sector'
    match '/search', action: 'index', on: :collection, via: [:get, :post]
    resources :shared_secrets, except: [:index, :show, :edit, :update, :destroy]
  end

  resources :shared_secrets, except: [:index, :show, :edit, :update, :destroy]

  resources :organizations do
    match '/search', action: 'index', on: :collection, via: [:get, :post]
  end

  resources :front_desk_relations, only: :destroy
  resources :front_desks do
    put :block, on: :member
    put :unblock, on: :member
    get '/pseudonym_show/:pseudonym' => 'front_desks#pseudonym_show', on: :collection, as: :pseudonym
    resources :verifications, only: [:edit, :show] do
      put :fraud_correct
      put :fraud_incorrect
    end
  end

  resources :roles, concerns: :reviewable
  resources :managers, except: [:destroy], concerns: :reviewable do
    match '/search', action: 'index', on: :collection, via: [:get, :post]
    get :show_own, on: :member
    get :edit_own, on: :member
    patch :update_own, on: :member
  end

  # Sent e-mails
  resources :sent_emails, only: [:index] do
    match '/search', action: 'index', on: :collection, via: [:get, :post]
  end

  # SMS
  resources :sms_challenges, only: [:index] do
    match '/search', action: 'index', on: :collection, via: [:get, :post]
  end

  # Letters
  resources :activation_letter_files, only: [:index] do
    post :preferences,       on: :collection
    get :download_xml,       on: :member
    get :download_csv,       on: :member
    get :download_processed, on: :member
    post :reupload_letter,    on: :member
  end

  resources :activation_letters, only: [:download_not_sent] do
    get :download_not_sent, on: :collection
  end
  resources :gba_blocks, only: [:destroy]

  resources :fraud_reports, only: [:index, :new, :create]
  resources :pilot_groups, only: [:edit, :index, :show, :update] do
    resources :subscribers, only: [:create, :destroy] do
      delete :destroy_all, on: :collection
    end
  end

  resources :configurations, only: [:index]
  get '/iapi/configurations/:name' => 'configurations#get'

  resources :kiosks, only: [:index, :edit, :update, :destroy], concerns: :reviewable

  match '/search' => 'search_engine#search', via: [:get, :post]
  match '/view_gba_popup' => 'accounts#view_gba_popup', via: [:get, :post]

  resource :session, only: [:new, :create, :destroy]
  match '/sign_in' => 'sessions#new', via: [:get, :post]
  match '/sign_out' => 'sessions#destroy', via: [:get, :post]
  match '/destroy_session' => 'sessions#destroy', via: [:get, :post]
  post '/session_ping' => 'sessions#session_ping'

  resources :ns_switches, except: [:index, :new, :create, :destroy]
  resources :app_switches, except: [:index, :new, :create, :destroy]
  resources :switches, except: [:new, :create, :destroy]
  resources :bulk_orders, only: [:index, :new, :create, :show, :destroy] do
    # Collection
    get :download_overview, on: :collection
    get '/transactions/:id' => 'bulk_orders#transactions', on: :collection, as: 'transactions'
    # Member
    patch :approve, on: :member
    patch :reject, on: :member
    get '/download/invalid_bsn',    on: :member, action: 'download_invalid_bsn'
    get '/download/account_status', on: :member, action: 'download_account_status'
    get '/download/address_list',   on: :member, action: 'download_address_list'
  end

  resources :app_versions, only: [:index, :new, :create, :edit, :update, :destroy], concerns: :reviewable do
    patch :update_warning
    put :update_warning
  end
  resources :third_party_apps, only: [:index, :new, :create, :edit, :update, :destroy], concerns: :reviewable do
    patch :update_warning
    put :update_warning
  end

  resources :blacklisted_phone_numbers, except: [:show]
  resources :whitelisted_phone_numbers, except: [:show], concerns: :reviewable

  namespace(:iapi) do
    # Digid DC
    post "retrieve_legacy_ids", to: "dc#retrieve_legacy_ids"
    post "task", to: "task#task"
  end

  root to: 'dashboard#index'
end
