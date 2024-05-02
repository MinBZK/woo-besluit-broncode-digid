
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

module FourEyes
  extend ActiveSupport::Concern

  attr_accessor :note
  attr_accessor :sanitized_params
  attr_accessor :four_eyes_review_id # required to get access to a review from a new webservice

  included do
    validate :four_eyes_uniq_validation
    validates :note, presence: true, length: {maximum: 255}, if: Proc.new {|x| self.four_eyes_change? }
  end

  class_methods do
    def any_reviews?(ids)
      FourEyesReview.where(record_table: name, record_id: ids).exists?
    end

    def get_reviews
      FourEyesReview.where(record_table: name)
    end

    def freeze_under_review(attribute)
      method_name = "check_frozen_#{attribute.to_s}".to_sym
      define_method(method_name) do
        check_frozen_four_eyes_association(attribute)
      end
      self.validate method_name

      # create a was method for the old values
      ids_method = "#{attribute.to_s.singularize}_ids"
      class_eval { attr_accessor "#{ids_method}_was" }
      define_method("#{ids_method}=") do |ids|
        public_send("#{ids_method}_was=", public_send(ids_method))
        super(ids)
      end
    end
  end

  def initialize(params=nil)
    self.sanitized_params = params.to_h.with_indifferent_access if params
    super
  end

  def under_review?
    !!self.review
  end

  def review
    # This allows the method to be called for new webservices (without an id) and for existing webservices (or else it will find reviews without record_ids)
    if @four_eyes_review_id
      @review ||= FourEyesReview.find(@four_eyes_review_id)
    else
      @review ||= self.id ? FourEyesReview.find_by(record_table: self.class.name, record_id: self.id) : nil
    end
  end

  def destroy_for_review(manager:)
    @four_eyes_change = true
    unless self.try(:undestroyable?)
      unless FourEyesReview.where(action: FourEyesReview::Action::DESTROY, record_table: self.class.name, record_id: self.id).exists?
        _create_review(action: FourEyesReview::Action::DESTROY, manager: manager)
      end
    end
  end

  def update_for_review(attributes, manager:)

    @four_eyes_change = true
    self.sanitized_params = attributes.to_h.with_indifferent_access if attributes

    success = false

    if self.is_a?(ActiveRecord::Base)
      AccountBase.transaction do
        success = self.update(self.sanitized_params) # simulate a save for validates
        raise ActiveRecord::Rollback
      end
    else
      self.assign_attributes(self.sanitized_params)
      success = self.valid?
    end

    if success
      _create_review(action: FourEyesReview::Action::UPDATE, manager: manager)
      return true
    end
    false
  end

  def save_for_review(manager:)
    @four_eyes_change = true
    if self.valid?
      _create_review(action: FourEyesReview::Action::CREATE, manager: manager)
      return true
    end
    false
  end

  def check_frozen_four_eyes_association(attribute)

    param_ids = "#{attribute.to_s.singularize}_ids"
    association_class = self.class.reflections[attribute.to_s].klass

    if review
      ids = review.original.send(param_ids) + review.updated.send(param_ids)
    else
      ids = send(param_ids) + ( send("#{param_ids}_was") || [] )
    end

    if !ids.empty? && association_class.any_reviews?(ids.uniq) && four_eyes_change?
      errors.add(param_ids, I18n.t('four_eyes.frozen'))
    end
  end

  def calculate_uniq_hash(validator)
    digest_values = []

    validator.attributes.each do |field|
      value = self.send(field)
      value = value.downcase if !validator.options[:case_sensitive] && value.respond_to?(:downcase)

      unless validator.options[:allow_blank] && value.blank?
        if value.nil?
          digest_values << "#{field}_nil"
        else
          digest_values << "#{field}:#{value}"
        end
      end
      if validator.options[:scope]
        if validator.options[:scope].instance_of?(Array)
          validator.options[:scope].each do |scope|
            digest_values << "#{scope}:#{self.send(scope)}"
          end
        else
          digest_values << "#{validator.options[:scope]}:#{self.send(validator.options[:scope])}"
        end
      end
    end

    Digest::MD5.hexdigest(digest_values.join("-")) unless digest_values.join("").blank?
  end

  def four_eyes_uniq_validation
    uniq_validators = self.class.validators.select{ |obj| obj.class == ActiveRecord::Validations::UniquenessValidator }
    uniq_validators.each do |validator|
      hash = calculate_uniq_hash(validator)

      uniq_query = FourEyesReview.where("uniq_hashes LIKE ?", "%#{hash}%")
      uniq_query = uniq_query.where("id != ?", self.review.id) if self.review
      if hash.present? && uniq_query.exists?
        validator.attributes.each { |attribute| errors.add(attribute, "is al in gebruik") }
      end
    end
  end

  def note
    review ? review.note : @note
  end

  def four_eyes_change?
    @four_eyes_change
  end

  ###################################
    private
  ###################################

  def _create_review(action:, manager:)
    @review = FourEyesReview.create!(serialized_object: self.sanitized_params, note: note, action: action, manager: manager, record_table: self.class.name, record_id: self.id, uniq_hashes: _generate_uniq_hashes )
  end

  def _generate_uniq_hashes
    uniq_hash_arr = []
    uniq_validators = self.class.validators.select { |obj| obj.class == ActiveRecord::Validations::UniquenessValidator }
    uniq_validators.each { |validator| uniq_hash_arr << self.calculate_uniq_hash(validator) }
    uniq_hash_arr.compact.join(',')
  end
end
