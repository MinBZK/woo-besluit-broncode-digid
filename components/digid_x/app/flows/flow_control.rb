
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

module FlowControl
  module InstanceMethods
    def [](key)
      @flow[key]
    end

    def []=(key, value)
      @flow[key] = value
    end

    def completed?
      @state == :completed
    end

    def complete_step!(step, controller:)
      raise FlowError, "unknown step #{step}" unless flow_specification.key?(step)
      send("#{step}_completed", controller) if respond_to?("#{step}_completed")
    end

    def page_name
      @flow[@state][:page_name]
    end

    def redirect_to
      @flow[@state][:redirect_to]
    end

    def error
      @error
    end

    def error=(error)
      @error = error
    end

    def state
      @state
    end

    def verified?
      @state == :verified
    end

    def transition_to!(new_state, &block)
      Rails.logger.debug "Flow transition from #{@state} to #{new_state}"

      return if @state == new_state
      raise FlowError, "unknown state #{new_state}" unless flow_specification.key?(new_state)
      if flow_specification[@state].present?
        raise FlowError, "only allowed to transition to states #{flow_specification[@state][:transitions]} from current state #{@state}" unless flow_specification[@state][:transitions].include?(new_state)
      else
        raise FlowError, "currrent state #{@state} is not in flow_specification #{flow_specification}"
      end
      yield if block
      @state = new_state
    end

    private

    def flow_specification
      self.class.const_get(:FLOW)
    end
  end

  def self.included(klass)
    klass.send :include, InstanceMethods
  end
end
