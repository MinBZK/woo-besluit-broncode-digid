
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

class Ability
  include CanCan::Ability

  def initialize(user)
    user ||= Manager.new
    return unless user.active?

    if user.superuser?
      if Rails.env.test?
        can :manage, :all
      else
        can :manage, [Role, Manager]
      end
    end

    # slurp in and declare all roles and permissions belonging to the given (logged in) user
    user.roles.each do |role|
      role.permissions.each { |permission| can(*permission.to_cancan) if permission.to_cancan }
    end

    # a manager can edit his own account even if he has no right to  manage or read Manager instances.
    can [:show, :update], Manager do |instance|
      user.id == instance.id unless instance.nil?
    end

    @user = user
    set_four_eyes_abilities
    shared_secret_abilities
    set_search_abilities
    set_activation_letter_abilities
    set_sector_code_abilities
    set_admin_report_abilities
    set_admin_report_overview_ability
    set_fraud_research_abilities
    set_alert_abilities
    set_front_desk_abilities
    set_subscriber_abilities
    set_log_abilities
    set_bulk_order_abilities
    set_generate_pdf_letter
  end

  private

  def set_generate_pdf_letter
    can(:generate_pdf_letter, Account) if can?(:last_activation_data, Account) && can?(:read, Account)
  end

  def set_four_eyes_abilities
    can(:read, FourEyesReview) if can?(:read, Role)
    can(:read, FourEyesReview) if can?(:read, Manager)
    can(:read, FourEyesReview) if can?(:read, Webservice)
    can(:read, FourEyesReview) if can?(:read, AppVersion)
    can(:read, FourEyesReview) if can?(:read, ThirdPartyApp)
    can(:read, FourEyesReview) if can?(:read, Kiosk)
    can(:read, FourEyesReview) if can?(:read, Dc::Organization)
    can(:read, FourEyesReview) if can?(:read, Dc::Connection)
    can(:read, FourEyesReview) if can?(:read, Dc::Service)

    can(:accorderen, Dc::Organization) if can?(:manage, Dc::Organization)
    can(:accorderen, Dc::Connection) if can?(:manage, Dc::Connection)
    can(:accorderen, Dc::Service) if can?(:manage, Dc::Service)
    can(:accorderen, AppVersion) if can?(:manage, AppVersion)
    can(:accorderen, ThirdPartyApp) if can?(:manage, ThirdPartyApp)
    can(:accorderen, Kiosk) if can?(:manage, Kiosk)

    can(:search, FourEyesReport) if can?(:read, FourEyesReport)

  end

  def shared_secret_abilities
    can :manage, SharedSecret if can? :manage, Webservice
  end

  def set_sector_code_abilities
    can :sector_code, Account if can? :manage, Account
  end

  def set_admin_report_abilities
    can [:read, :download_bundle], AdminReport if can? :read_fraud,     AdminReport
    can [:read, :download_bundle], AdminReport if can? :read_monthly,   AdminReport
    can [:read, :download_bundle], AdminReport if can? :read_std,       AdminReport
    can [:read, :download_bundle], AdminReport if can? :read_weekly,    AdminReport
    can [:read, :download_bundle], AdminReport if can? :read_std,       AdminReport
    can [:read, :download_bundle], AdminReport if can? :read_integrity, AdminReport
    can [:read, :download_bundle], AdminReport if can? :read_adhoc_log, AdminReport
    can [:read, :download_bundle], AdminReport if can? :read_adhoc_gba, AdminReport
  end

  def set_admin_report_overview_ability
    can(:read_overview, AdminReport) if can?(:read_fraud, AdminReport) ||
                                        can?(:read_monthly, AdminReport) ||
                                        can?(:read_std, AdminReport) ||
                                        can?(:read_weekly, AdminReport) ||
                                        can?(:read_integrity, AdminReport) ||
                                        can?(:read_sec, AdminReport)
  end

  def set_fraud_research_abilities
    can :create, FraudReport if can? :create_log,  FraudReport
    can :create, FraudReport if can? :create_gba,  FraudReport
    can :create, FraudReport if can? :create_tx,   FraudReport
  end

  # rubocop:disable all
  def set_alert_abilities
    # uses the result of set_fraud_research_abilities
    if can?(:create_tx, FraudReport) ||
       can?(:create_log, FraudReport) ||
       (can?(:create_gba, FraudReport) && can?(:manage, Account))
      if can?(:read_fraud, AdminReport)
        if can?(:read_adhoc_gba, AdminReport)
          can(:receive_alerts, :ad_hoc_frauderapportages_accounts)
        end
        if can?(:read_adhoc_log, AdminReport)
          can(:receive_alerts, :ad_hoc_frauderapportages_log)
        end
        if can?(:read_adhoc_tx, AdminReport)
          can(:receive_alerts, :ad_hoc_frauderapportages_transacties)
        end
      end
    end

    if can?(:read, FrontDesk) || can?(:manage, FrontDesk)
      can(:receive_alerts, :balie_fraudevermoeden) if can?(:audit, Verification)
      can(:receive_alerts, :balie_controle_uitgiftes)
    end

    if can?(:read, ActivationLetterFile) || can?(:manage, ActivationLetterFile)
      can(:receive_alerts, :briefbestanden)
    end

    if can?(:read_monthly, AdminReport)
      can(:receive_alerts, :maandrapportages)
    end

    if can?(:read_weekly, AdminReport)
      can(:receive_alerts, :weekrapportages)
    end

    if can?(:read_std, AdminReport)
      can(:receive_alerts, :standaardrapportages)
    end

    if can?(:read_fraud, AdminReport)
      can(:receive_alerts, :frauderapportages)
    end

    if can?(:read_integrity, AdminReport)
      can(:receive_alerts, :integriteitsrapportages)
    end

    if can?(:read_sec, AdminReport)
      can(:receive_alerts, :security_rapportages)
    end

    can(:receive_alerts, :munin_functioneel)
    can(:receive_alerts, :munin_technisch)
  end
  # rubocop:enable all

  def set_search_abilities
    can :search, Organization if can? :read, Organization
    can :search, Webservice if can? :read, Webservice
    can :search, Account if can? :read, Account
    can :search, SmsChallenge if can? :read, SmsChallenge
    can :search, SentEmail if can? :read, SentEmail
    can :search, Afmeldlijst if can? :read, Afmeldlijst
    can :search, BeveiligdeBezorgingPostcode if can? :read, BeveiligdeBezorgingPostcode
  end

  def set_activation_letter_abilities
    can :preferences, ActivationLetterFile if can? :manage, ActivationLetterFile
    can :download_csv, ActivationLetterFile if can? :read, ActivationLetterFile
    can :download_xml, ActivationLetterFile if can? :read, ActivationLetterFile
    can :download_not_sent, ActivationLetter if can? :read, ActivationLetterFile
  end

  def set_front_desk_abilities
    can [:block, :unblock], FrontDesk if can? :block, FrontDesk
    can [:read], Verification if can?(:read, FrontDesk) || can?(:audit, Verification)
    can [:update, :fraud_correct, :fraud_incorrect], Verification if can?(:audit, Verification)
  end

  def set_subscriber_abilities
    can :read, Subscription if can? :manage, PilotGroup
    can [:create, :destroy, :destroy_all], Subscriber if can? :manage, PilotGroup
    can [:index, :new, :create, :destroy], Afmelding if can? :manage, Afmeldlijst
    can :create, Afmelding if can? :create, Afmeldlijst
    can :destroy, Afmelding if can? :destroy, Afmeldlijst
  end

  def set_log_abilities
    return unless can?(:read, Log)
    can :search, Log
    can :transactions, Log
    can :export, Log
  end

  def set_bulk_order_abilities
    set_bulk_order_read
    set_bulk_order_downloads

    # Partial permissions
    # FIXME: do a permutation test run on these permissions
    set_bulk_order_approve  if can?(:_approve, BulkOrder)
    set_bulk_order_reject   if can?(:_approve, BulkOrder)
    set_bulk_order_destroy  if can?(:_destroy, BulkOrder)
  end

  def set_bulk_order_read
    # Other read like actions
    return unless can?(:read, BulkOrder) # RuboCop wants a Style/GuardClause
    can :transactions,        BulkOrder
    can :account_status_csv,  BulkOrder
  end

  def set_bulk_order_approve
    cannot :approve, BulkOrder # Reset ability first
    can :approve, BulkOrder do |bulk_order|
      not_me = bulk_order.manager_id && bulk_order.manager_id != @user.id
      bulk_order.created_status? && not_me
    end
  end

  def set_bulk_order_reject
    cannot :reject, BulkOrder # Reset ability first
    can :reject, BulkOrder do |bulk_order|
      not_me = bulk_order.manager_id && bulk_order.manager_id != @user.id
      bulk_order.created_status? && not_me
    end
  end

  def set_bulk_order_destroy
    cannot :destroy, BulkOrder # Reset ability first
    can :destroy, BulkOrder do |bulk_order|
      %w(invalid_status created_status approved_status rejected_status).include?(bulk_order.status)
    end
  end

  def set_bulk_order_downloads
    return unless can?(:read, BulkOrder) # RuboCop wants a Style/GuardClause
    can :download_overview,       BulkOrder
    can :download_invalid_bsn,    BulkOrder
    can :download_account_status, BulkOrder
    can :download_address_list,   BulkOrder
  end
end
