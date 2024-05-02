
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

class NewsItem < AccountBase
  # ASSOCIATIONS
  has_and_belongs_to_many :news_locations, join_table: :news_items_news_locations # rubocop:disable Rails/HasAndBelongsToMany

  # SCOPES
  default_scope { order(updated_at: :desc) }
  scope :active, lambda {
    where(':today >= active_from AND :today <= active_until', today: DateTime.now)
      .where(active: true)
  }

  # VALIDATIONS
  validates :body_nl, presence: true
  validates :body_en, presence: true
  validates :name_nl, presence: true
  validates :name_en, presence: true
  validates :news_locations, presence: true
  validates :active_until, date: { after: :active_from }

  def os_versions
    os.presence&.split(";")&.map{|i|i.split(":")} || [[]]
  end

  def browser_versions
    browser.presence&.split(";")&.map{|i|i.split(":")} || [[]]
  end

  class << self
    def browser_list
      @browser_list ||= config_list("Browser::")
    end

    def os_list
      @os_list ||= config_list("Browser::Platform::")
    end

    private
    def config_list(prefix)
      prefix.constantize.constants.filter do |i|
        klass = "#{prefix}#{i}".constantize
        klass.is_a?(Class) && klass.superclass == "#{prefix}Base".constantize
      end.map{|i| [i.to_s, "#{prefix}#{i}".constantize.new("").id] }
    end
  end
end
