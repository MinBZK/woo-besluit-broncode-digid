
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

module DigidUtils
  module Dc
    class Base
      include ActiveModel::Model
      include Dc::DateHelper
      attr_accessor :destroyed, :updated_at, :created_at

      def initialize(attributes = nil)
        assign_attributes(attributes || {})
      end

      class << self
        def base_path
          raise "No base path is set for this mdoel"
        end

        def create_path
          base_path
        end

        def update_path
          "#{base_path}/%{id}"
        end

        def find(id)
          begin
            new(client.get("#{base_path}/#{id}").result)
          rescue DigidUtils::Iapi::StatusError
            nil
          end
        end

        def find_by(id:)
          find(id)
        end

        def all(page: nil)
          page ||= 1
          begin
            Result.new(client.get("#{base_path}?page=#{page.to_i-1}&size=30").result, self)
          rescue DigidUtils::Iapi::StatusError
            Result.new([], self)
          end
        end

        def search(query)
          page = query[:page] || 1

          if query.nil?
            all(page)
          else
            page ||= 1
            query = delete_if_no_value(query.to_hash)
            query = convert_dates(query)
            Result.new(client.post("#{base_path}/search?page=#{page.to_i-1}&size=30", query).result, self)
          end
        end

        def create(params)
          new(params || {}).tap(&:save)
        end

        alias_method :where, :search

        def client
          DigidUtils::Dc.client
        end

        private

        def delete_if_no_value(hash)
          hash.each do |key, value|
            hash[key] = delete_if_no_value value if value.is_a?(Hash)
          end
          hash.delete_if{|key, value| value.to_s.blank?}
        end

        def convert_dates(hash)
          hash.each do |key, value|
            hash[key] = convert_dates value if value.is_a?(Hash)
          end
          if hash["(3i)"].present?
            hash.values.join("-")
          else
            hash
          end
        end
      end

      def new_record?
        id.blank?
      end

      def persisted?
        !new_record?
      end

      def save(validate: true)
        return false if validate && !valid?
        params = attributes.dup

        if new_record?
          response = self.class.client.post(self.class.create_path, params)
          assign_attributes(response.result)
        else
          self.class.client.patch(self.class.update_path % params, params)
        end

        true
      end

      def destroy
        unless new_record? || undestroyable?
          self.class.client.delete("#{self.class.base_path}/#{id}", {})
          @destroyed = true
          @id = nil
          return @destroyed
        end

        false
      end

      def destroyed?
        @destroyed
      end

      def undestroyable?
        false
      end

      def marked_for_destruction?
        false
      end

      def assign_attributes(attributes = {})
        # convert date so we can see it in the four_eyes_review
        attributes.keys.find_all {|key| key.to_s.ends_with?("(1i)")}.each do |key|
          date_helper attributes, key.gsub("(1i)", "")
        end
        attributes.each do |key, value|
          public_send("#{key}=", value) if respond_to?("#{key}=")
        end
      end

      def update(attributes = {})
        assign_attributes(attributes)
        save
      end

      def save!
        save || raise(RecordNotSaved.new("Failed to save the record", self))
      end

      def status
        @status = @status.is_a?(Dc::Status) ? @status : Dc::Status.new(@status || {})
      end

      delegate :active, :active_from, :active_until, to: :status

      def unique_oin
        errors.add(:oin, "is al in gebruik") if new_record? && self.class.where(oin: oin).any?
      end

      def unique_uuid
        errors.add(:uuid, "is al in gebruik") if new_record? && self.class.where(uuid: uuid).any?
      end

      def check_status
        errors.merge!(status.errors) unless status.valid?
      end

      def check_certificate
        if certificates.empty?
          errors.add :certificates, "vereist"
        else
          certificates.each do |cert|
            errors.merge!(cert.errors) unless cert.valid?
          end
        end
      end

      def organization_roles
        @organization_roles = (@organization_roles || []).map {|i| i.is_a?(Dc::OrganizationRole) ? i : Dc::OrganizationRole.new(i) }.compact
      end

      def certificates
        (@certificates || []).map {|c| c.is_a?(Dc::Certificate) ? c : Dc::Certificate.new(c)}
      end

      class Result
        attr_accessor :content, :pageable, :total_pages, :sort, :number_of_elements, :empty, :parent
        attr_writer :first, :size, :last, :number, :total_elements

        def initialize(attributes = {}, parent = nil)
          attributes.each do |key, value|
            public_send("#{key}=", value)
          end
          @content.map!{|i| parent.new(i)}
        end

        def method_missing(m, *args, &block)
          content.send(m, *args, &block)
        end

        def count
          @total_elements
        end

        def current_page
          @number + 1
        end

        def limit_value
          @size
        end

        def first_page?
          @first
        end

        def last_page?
          @last
        end

        def next_page
          !last_page? && (current_page + 1)
        end

        def prev_page
          !first_page? && (current_page - 1)
        end
      end
    end
  end
end
