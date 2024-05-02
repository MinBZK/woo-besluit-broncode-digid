
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

require "forwardable"

class Translations
  extend Forwardable

  delegate [:[], :each, :include?] => :@table

  class << self
    def locale_files(git_revision)
      IO.popen(["git", "ls-tree", "-r", "--name-only", git_revision, "config/locales"]) do |io|
        io.read.split
      end
    end

    def from_git_revision(git_revision)
      new.tap do |t|
        locale_files(git_revision).each do |file|
          yield file
          t.add_from_git(git_revision, file)
        end
      end
    end
  end

  def initialize
    @table = {}
  end

  def add_from_git(git_revision, path)
    result = IO.popen(["git", "show", "#{git_revision}:#{path}"]) do |io|
      YAML.load(io.read)
    end
    add_from_hash(result["nl"]) if result["nl"]
  end

  def -(other)
    deleted = {}
    changed = {}
    added = {}

    each do |key, value|
      if !other.include?(key)
        added[key] = value
       elsif other[key] != value
        changed[key] = value
      end
    end

    other.each do |key, value|
      if !self.include?(key)
        deleted[key] = value
      end
    end

    { "added" => added, "changed" => changed, "deleted" => deleted }
  end

  private

  def add_from_hash(result, prefix=[])
    result.each do |key, value|
      if value.is_a?(Hash)
        add_from_hash(value, prefix + [key])
      else
        @table[(prefix + [key]).join(".")] = value
      end
    end
  end
end
