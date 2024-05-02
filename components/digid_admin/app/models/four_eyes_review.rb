
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

class FourEyesReview < AccountBase
  module Action
    CREATE = 'create'.freeze    # Nieuw record aanmaken
    UPDATE = 'update'.freeze    # Een record wijzigen
    DESTROY = 'destroy'.freeze  # Een record verwijderen
  end

  belongs_to :manager

  validates_presence_of :manager, :record_table, :action
  validates_presence_of :serialized_object, unless: -> { action == Action::DESTROY }
  validate :manager_not_changed, on: :update

  serialize :serialized_object

  attr_accessor :original, :updated

  def manager_not_changed
    errors.add(:manager, "Je kan alleen je eigen 'te accorderen' record wijzigen") if manager_id_changed?
  end

  def accept(manager:)
    success = case action
      when Action::CREATE
        params = self.serialized_object.merge!({four_eyes_review_id: self.id}).merge(four_eyes_updated_at: Time.now)
        @original = self.record_table.constantize.new(params)
        @original.save
      when Action::UPDATE
        self.original.update(self.serialized_object.merge(four_eyes_updated_at: Time.now))
      when Action::DESTROY
        self.original.destroy
    end

    if success
      self.destroy
      return true
    end

    false
  end

  def assign_serialized_object(params:)
    self.record_table.constantize.new params # sanitizing params via the new method
    self.serialized_object = params
  end

  def defined?(attribute)
    original.respond_to?(attribute) && original.send(attribute) || updated.respond_to?(attribute) && updated.send(attribute)
  end

  def equals?(attribute, value)
    original.send(attribute) == value || updated.send(attribute) == value
  end

  def original
    if self.record_id
      @original ||= self.record_table.constantize.find(self.record_id)
    else
      @original ||= self.record_table.constantize.new
    end
  end

  def update_review(params:)
    if self.record_id
      instance = self.record_table.constantize.find(self.record_id)
      instance.sanitized_params = params
        AccountBase.transaction do
          instance.assign_attributes(instance.sanitized_params)
          raise ActiveRecord::Rollback
        end
    else
      instance = self.record_table.constantize.new params
    end
    instance.four_eyes_review_id = self.id # required for FourEyesReview uniqness validation check
    instance
  end

  def assign_updated_attributes
    tmp_original = self.record_table.constantize.find(self.record_id)
    AccountBase.transaction do
      tmp_original.assign_attributes(self.serialized_object) if tmp_original.present? # simulate a save for validates
      raise ActiveRecord::Rollback
    end
    tmp_original
  end

  def serialized_hash
    name = self.record_table == "Manager" ? self.updated.account_name : self.updated.name
    { subject_action: I18n.t("four_eyes_review_mailer.subject_actions.#{self.action.underscore.downcase}"),
      action: I18n.t("four_eyes_review_mailer.actions.#{self.action.underscore.downcase}"),
      model: I18n.t("four_eyes_review_mailer.models.#{self.record_table.underscore.downcase}"),
      name: name,
      creator_account_name: self.manager.account_name,
      created_at: self.created_at,
      manager_email: self.manager.email
    }
  end

  def updated
    if self.record_id
      if self.action == Action::DESTROY
        @new ||= self.record_table.constantize.new
      else
        @new ||= assign_updated_attributes
      end
    else
      # set four_eyes_review id in case of newly created webservice that you are trying to update
      params = self.serialized_object.merge!({four_eyes_review_id: self.id})
      @new ||= self.record_table.constantize.new(params)
    end
  end
end
