
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

# System gems
require "socket"
require "openssl"
require "ostruct"
require "pathname"
require "yaml"

# Third party gems
require "active_support"
require "active_model"
require "rails/railtie"
require "savon"
require "redis"
require "httpclient"

# Manual
require_relative "digid_utils/version"
require_relative "digid_utils/railtie"

module DigidUtils
  extend ActiveSupport::Autoload

  eager_autoload do
    autoload :BlockScheduler
    autoload :BSN
    autoload :Crypto
    autoload :Dc
    autoload :DhMu
    autoload :FlatHash
    autoload :Iapi
    autoload :HealthCheck
    autoload :Hsm
    autoload :Railtie
    autoload :SharedServices
    autoload :Property
    autoload :Settings
    autoload :PhoneNumber
    autoload :Ns
  end

  def self.eager_load!
    super.tap do
      Crypto.eager_load!
      DhMu.eager_load!
    end
  end

  mattr_accessor :rails_project

  def self.info
    @info ||= {}.tap do |info|
      # rubocop:disable Style/RescueModifier
      info["ruby_version"] = rails_version
      info["git_version"] = git_version
      info["config"] = defined?(APP_CONFIG) ? APP_CONFIG : nil
      # rubocop:enable Style/RescueModifier
    end
    yield @info if block_given?
    @info
  end

  def self.gems
    Bundler.definition.specs_for(%i[default production]).each_with_object({}) do |spec, gems|
      gems[spec.name.to_s] = if spec.source.is_a?(Bundler::Source::Git)
                               "#{spec.version}-git-#{spec.source.revision}"
                             else
                               spec.version.to_s
                             end
    end
  end

  def self.hostname
    @hostname ||= Socket.gethostname
  end

  def self.logger
    @logger ||= defined?(Rails) ? ::Rails.logger : Logger.new(STDOUT)
  end

  def self.logger=(logger)
    @logger = logger
  end

  def self.git_version
    Rails.root.join(".git-version").read.strip rescue ENV['GIT_TAG'] or nil
  end

  def self.rails_version
    Rails.root.join(".ruby-version").read.strip rescue nil
  end

  def self.redis_options
    @redis_options ||= begin
      config = redis_config
      (config.include?(Rails.env) ? config[Rails.env] : {}).deep_symbolize_keys.tap do |options|
        options[:password] ||= Rails.application.secrets[:redis_auth]
      end
    end
    @redis_options.dup
  end

  def self.redis_config
    return {} unless Rails.root
    path = Rails.root.join("config", "redis.yml")
    return {} unless path.exist?
    # rubocop:disable Security/YAMLLoad
    YAML.load(ERB.new(File.read(path)).result)
    # rubocop:enable Security/YAMLLoad
  end

  def self.redis_sidekiq_options
    @redis_sidekiq_options ||= begin
      config = redis_sidekiq_config
      (config.include?(Rails.env) ? config[Rails.env] : {}).deep_symbolize_keys.tap do |options|
        options[:password] ||= Rails.application.secrets[:redis_auth]
      end
    end
    @redis_sidekiq_options.dup
  end

  def self.redis_sidekiq_config
    return {} unless Rails.root
    path = Rails.root.join("config", "redis-sidekiq.yml")
    path = Rails.root.join("config", "redis.yml") unless path.exist?
    return {} unless path.exist?
    # rubocop:disable Security/YAMLLoad
    YAML.load(ERB.new(File.read(path)).result)
    # rubocop:enable Security/YAMLLoad
  end
end
