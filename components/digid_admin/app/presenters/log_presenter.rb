
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

class LogPresenter < BasePresenter
  presents :log

  delegate :name, :ip_address, :account_id, to: :log

  FRONT_DESK_RESOURCES = ['FrontDesk', 'Balie']
  EID_RESOURCES = ['Eid::Certificate', 'Eid::Crl']
  RDA_RESOURCES = ['Rda::Certificate', 'Rda::Crl']
  DC_RESOURCES = ["Dc::Organization", "Dc::Connection", "Dc::Service"]

  def manager
    if log.manager.present? && can?(:read, Manager)
      four_eyes_show_link(log.manager.account_name, log.manager, icon: false)
    elsif log.manager.present?
      log.manager.account_name
    elsif log.manager.nil? && !log.manager_id.nil? && log.manager_id != -1
      # Got an ID but manager was removed
      'Verwijderde beheerder'
    elsif log.manager_id == -1 || log.manager_id.nil?
      'Systeem'
    else
      'Onbekend' # Should never get here
    end
  end

  def pseudoniem
    link_to(truncate(log.pseudoniem, length: 15), pseudonym_front_desks_path(log.pseudoniem), remote: true) if log.pseudoniem
  end

  def subject
    if FRONT_DESK_RESOURCES.include?(log.subject_type)
      link_to('FrontDesk', "/front_desks/#{log.subject_id}")
    elsif EID_RESOURCES.include?(log.subject_type)
      name = log.subject_type.gsub('Eid::', '').downcase
      link_to(log.subject_type.gsub('::', '-'), "/eid/#{name == 'crl' ? name : name.pluralize}/#{log.subject_id}")
    elsif RDA_RESOURCES.include?(log.subject_type)
      name = log.subject_type.gsub('Rda::', '').downcase
      link_to(log.subject_type.gsub('::', '-'), "/rda/#{name == 'crl' ? name : name.pluralize}/#{log.subject_id}")
    elsif DC_RESOURCES.include?(log.subject_type)
      log.subject_type.constantize.find(log.subject_id).try(:name)
    else
      if [Manager].include?(log.subject.class)
        log.subject ? four_eyes_show_link(log.subject.name, log.subject, icon: false) : ''
      else
        log.subject.try(:name)
      end
    end
  end

  def account
    if log.account && can?(:read, Account)
      link_to("#{log.sector_number.to_s.rjust(9, '0')} (#{log.sector_name})", log.account)
    elsif log.account_id
      "#{log.sector_number.to_s.rjust(9, '0')} (#{log.sector_name})"
    end
  end

  def webservice
    if can?(:read, Webservice)
      log.webservice ? four_eyes_show_link(log.webservice.name, log.webservice, icon: false) : ''
    else
      log.webservice ? log.webservice.name : ''
    end
  end

  def created_at(time_zone = ::Time.zone)
    l(log.created_at.in_time_zone(time_zone), format: :default)
  end
end
