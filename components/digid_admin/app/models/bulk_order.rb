
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

class BulkOrder < AccountBase
  attr_accessor :csv_upload

  belongs_to :manager
  belongs_to :approval_manager, foreign_key: :approval_manager_id, class_name: 'Manager'
  belongs_to :rejection_manager, foreign_key: :reject_manager_id, class_name: 'Manager'
  has_many :bulk_order_bsns, dependent: :destroy

  # Rails master contains a _suffix and _prefix option
  # http://edgeapi.rubyonrails.org/classes/ActiveRecord/Enum.html
  # enum also creates scopes, etc.
  enum account_status_scope: %w(active suspended initial_or_requested) # Warning: This creates #initial_or_requested?,
                                                                                  # active?, suspended? .. methods.
  ALLOWED_TYPES_FOR_ACTIVE    =  ["verwijderen", "opschorten"]
  ALLOWED_TYPES_FOR_SUSPENDED            =  ["verwijderen", "opschorten ongedaan maken"]
  ALLOWED_TYPES_FOR_INITIAL_OR_REQUESTED =  ["verwijderen"]

  enum status: %w(
    invalid_status
    created_status
    approved_status
    rejected_status
    order_started_status
    order_finished_status
    brp_started_status
    finalized_status
    exceptional_status
  )

  before_validation :check_if_bulk_type_allowed, on: :create
  before_validation :parse_csv_upload, on: :create
  before_validation :process_csv_upload, on: :create

  validates :name,                    presence: true, length: { maximum: 255 }
  validates :csv_upload,              presence: true, on: :create
  validates :account_status_scope,    presence: true
  validates :status,                  presence: true
  validates :manager,                 presence: true
  validates :bulk_order_bsns,       presence: true
  validates :bulk_type,               presence: true
  validates_associated :bulk_order_bsns
  validates :uncached_bulk_order_bsns_count, numericality: { greater_than: 0, less_than_or_equal_to: :bsn_count_max }

  validate :validate_csv_upload_header,        on: :create
  validate :validate_csv_upload_content_size,  on: :create

  before_save :before_save_callback

  # TODO: Move these to the relevant classes and call from there?!
  def self.execute_brp_details_removal
    BulkOrder.finalized_status.where('finalized_at < ?', Bulk::AddressRemover::MAX_BRP_CACHE_DAYS.days.ago.beginning_of_day).find_each do |bulk_order|
      Bulk::AddressRemover.new(bulk_order).perform!
    end
  end

  def gba_request_bsns_to_schedule
    bulk_order_bsns.finished.pluck(:bsn)
  end

  def human_status
    I18n.translate(status, scope: 'bulk_order.statuses', default: status)
  end

  def current_status_by
    if invalid_status? || created_status?
      manager.full_name
    elsif approved_status?
      approval_manager.full_name
    elsif rejected_status?
      rejection_manager.full_name
    else
      I18n.translate('system', scope: 'bulk_order')
    end
  end

  def allow_address_list_download?
    finalized_status? && (finalized_at > Bulk::AddressRemover::MAX_BRP_CACHE_DAYS.days.ago.beginning_of_day) # FIXME: Make gba_deleted status
  end

  def csv_contains_too_much_bsns?
    csv_upload_content_size > bsn_count_max
  end

  def other_scope_count
    if account_status_scope ==  "suspended"
      bulk_order_bsns.other_scope.count
    else
      bulk_order_bsns.other_scope.count + bulk_order_bsns.suspended.count
    end
  end

  def approved_count
    bulk_order_bsns.approved.count + bulk_order_bsns.finished.count
  end

  def no_account_count
    bulk_order_bsns.no_account.count
  end

  def not_found_count
    bulk_order_bsns.not_found.count
  end

  def invalid_bsn_count
    bulk_order_bsns.invalid_bsn.count
  end

  private

  def invalid_bsns?
    bulk_order_bsns.detect(&:invalid_bsn?).present? # Cannot use AR here
  end

  def before_save_callback
    if new_record? # on create
      # This is not done in before_create (because they're run after before_save)
      self.status = if invalid_bsns?
                      :invalid_status
                    else
                      :created_status
                    end
      status_will_change! # Make sure we set a status_updated_at!
    end
    self.status_updated_at = Time.zone.now if status_changed?

    # Model.touch won't touch new records
    # Manager actions
    self.rejected_at = Time.zone.now if rejected_at.nil? && rejected_status?
    self.approved_at = Time.zone.now if approved_at.nil? && approved_status?
    # Status update timestamps
    self.order_started_at  = Time.zone.now if order_started_at.nil? && order_started_status?
    self.order_finished_at = Time.zone.now if order_finished_at.nil? && order_finished_status?
  end

  def check_if_bulk_type_allowed
    case self.account_status_scope
    when 'active'
      return if ALLOWED_TYPES_FOR_ACTIVE.include?(self.bulk_type)
    when 'suspended'
      return if ALLOWED_TYPES_FOR_SUSPENDED.include?(self.bulk_type)
    when 'initial_or_requested'
      return if ALLOWED_TYPES_FOR_INITIAL_OR_REQUESTED.include?(self.bulk_type)
    end

    errors.add(:opdracht_niet_toegestaan, I18n.t('activerecord.errors.models.bulk_order.attributes.bulk_status_type_combination.invalid'))
  end

  # before_validation
  def parse_csv_upload
    return true unless csv_upload.is_a?(ActionDispatch::Http::UploadedFile) || csv_upload.is_a?(Rack::Test::UploadedFile) # Return true to still validate!
    @csv_content_type = csv_upload.content_type
    csv_content = CSV.parse(csv_upload.read).flatten
    @csv_upload_header = csv_content.shift
    @csv_upload_content = csv_content
    true
  rescue CSV::MalformedCSVError
    errors.add(:content_type, I18n.t('activerecord.errors.models.bulk_order.attributes.csv_upload_content_type.invalid'))
  end

  # before_validation
  def process_csv_upload
    return if @csv_upload_content.blank? || csv_contains_too_much_bsns?
    @csv_upload_content.each do |bsn|
      bulk_order_bsns.build(bsn: bsn)
    end
  end

  def validate_csv_upload_header
    errors.add(:kolom_header, I18n.t('activerecord.errors.models.bulk_order.attributes.csv_upload.invalid_header')) if @csv_upload_header != 'Burgerservicenummer'
  end

  def validate_csv_upload_content_size
    errors.add(:minimaal_aantal, I18n.t('activerecord.errors.models.bulk_order.attributes.csv_upload.min_amount')) if csv_upload_content_size < 1
    errors.add(:maximaal_aantal, I18n.t('activerecord.errors.models.bulk_order.attributes.csv_upload.max_amount')) if csv_upload_content_size > bsn_count_max
  end

  def csv_upload_content_size
    @csv_upload_content.try(:size) || 0
  end

  def bsn_count_max
    ::Configuration.get_int('csv_bsn_list_maximum')
  end

  def uncached_bulk_order_bsns_count
    bulk_order_bsns.size
  end
end
