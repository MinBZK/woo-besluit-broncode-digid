
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

module Eid
  class AtRequest < AccountBase
    module Status
      CREATED   = 'created'.freeze
      APPROVED  = 'approved'.freeze
      REJECTED  = 'rejected'.freeze
      SENT      = 'sent'.freeze
      ABORTED   = 'aborted'.freeze
      FINISHED  = 'finished'.freeze
    end

    include Stateful

    LEVELS = { 9 => 'light', 10 => 'basis', 20 => 'midden', 25 => 'substantieel', 30 => 'hoog' }.freeze

    belongs_to :created_by, class_name: 'Manager'
    belongs_to :approved_by, class_name: 'Manager'
    belongs_to :sent_by, class_name: 'Manager'
    has_many :logs

    before_validation do |instance|
      instance.reference = nil if instance.reference.blank?
    end

    validates :document_type, inclusion: DocumentType::ALL
    validates :authorization, inclusion: PolymorphType::ALL
    validate :check_sequence
    validate :check_reference

    class << self
      def initial
        Rails.cache.fetch("eid-at-request-initial", expire_in: 10.seconds) do
          result = {}
          DocumentType::ALL.each do |doc_type|
            result[doc_type] = [nil, []]
          end

          Eid::Certificate.all.sort_by{ |cert| [cert.subject] }.reverse_each do |cert|
            next unless cert.type == 'AT'
            result[cert.documentType][0] ||= next_seq_no(cert.subject[-5..-1])
            result[cert.documentType][1] << cert.subject
          end
          self.all.order(:sequence_no).each do |req|
            result[req.document_type][0] = next_seq_no(req.sequence_no)
          end

          result
        end
      end

      def next_seq_no(no)
        m = /(\D*)(\d+)/.match(no)
        m ? format("%s%0#{m[2].length}d", m[1], m[2].to_i + 1) : no
      end
    end

    def check_sequence
      if !sequence_no.to_s.match?(/^([A-Z]{2}[0-9]{3}|[A-Z]{1}[0-9]{4}|[0-9]{5})$/)
        errors.add(:sequence_no, :invalid)
        return
      end

      initial = self.class.initial.dig(document_type)
      return unless initial
      if initial[0] && sequence_no < initial[0]
        errors.add(:sequence_no, :used, initial: initial[0])
      end
    end

    def check_reference
      initial = self.class.initial.dig(document_type)
      return unless initial
      if reference.present? && !initial[1].include?(reference)
        errors.add(:reference, :inclusion)
      elsif reference.blank? && initial[1].size > 0
        errors.add(:reference, :empty)
      end
    end

    def filename
      "#{document_type}-#{authorization}-#{sequence_no}.cvcreq"
    end

    def human_status
      I18n.t(status, scope: "eid.status")
    end

    def download?
      %w[approved sent finished].include?(status)
    end
  end
end
