
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

# frozen_string_literal: true

Rails.application.routes.draw do
  # notice: custom middleware used (see `rake middleware`)
  # use HostHeaderRewrite

  root to: 'verifications#new'

  if APP_CONFIG['dot_environment']
    get :api_docs, to: 'home#api_docs', defaults: { format: 'json' }
  end

  scope 'saml/sp', controller: 'sessions' do
    if APP_CONFIG.dig("eherkenning", "enabled")
      get :eherkenning_authentication, to: 'sessions#new', as: 'authenticate'
    else
      get :eherkenning_authentication, to: 'scifi/sessions#new', as: 'authenticate'
      post :scifi_assertion_consumer_service, to: 'scifi/sessions#create'

      # Environments which have eHerkenning disabled by default, should still
      # be able to use eHerkenning.
      get 'eh', to: 'sessions#new'
    end
    get :assertion_consumer_service, to: 'sessions#artifact'

    get :logout, to: 'sessions#logout'

    get :metadata
  end

  resource :front_desk_relations, only: %i[new create]

  resources :verifications,
            except: :index,
            path: 'aanvraag',
            path_names: { new: 'ophalen', edit: 'controle' } do
              get :show_letter, on: :member
              post :sms_received, on: :member
              post :finish, on: :member
              resources :verification_corrections, path: 'correctie', only: %i[new create], path_names: { new: '' }
            end

  resources :audits, only: %i[index show update], path: 'controle'

  put '/aanvraag/:id/reject', to: 'verifications#reject', as: 'reject_verification'

  namespace 'iapi', defaults: { format: :json } do
    resources :activations, only: %i[index update]
    resources :front_desks, except: %i[new edit]
    resources :front_desk_relations, only: %i[destroy show]
    resources :verifications, only: %i[index show update] do
      get :fraud_suspicion, on: :collection
      get :unaudited, on: :collection
    end
    post "task", to: "tasks#task"
  end
end
