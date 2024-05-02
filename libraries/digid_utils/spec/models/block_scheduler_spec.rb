
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

class MockScheduler
  include ActiveModel::AttributeMethods

  attr_accessor :name, :key, :serial

  class << self
    def instances
      @instances ||= {}
    end

    def find_by(options)
      instances[options[:name]]
    end

    def clear
      @instances = {}
    end
  end

  def initialize(attributes)
    attributes.each do |key, value|
      send(:"#{key}=", value)
    end
  end

  def attributes
    { name: name, key: key, serial: serial }
  end

  def with_lock
    yield
  end

  def save!
    self.class.instances[name] = self
  end
end

##
# Simple scheduler that uses maximum_per_interval and different interval
# Works the same as the BRP jobs in X
class SimpleScheduler < MockScheduler
  include DigidUtils::BlockScheduler

  def maximum_per_interval
    15
  end

  def interval
    15.minutes
  end
end

#
# Advanced scheduler has different maximum depending on time
# Works the same as the Bulk BRP jobs in Admin
class AdvancedScheduler < MockScheduler
  include DigidUtils::BlockScheduler

  def calculate_key(time)
    (time.min < 45 ? "L" : "H") + number_from_time(time, 1.hour).to_s
  end

  def candidate
    delay = key[0] == "L" ? 180 : 60
    time_from_number(key[1..-1].to_i, delay)
  end

  def serial_of_new_block(time)
    time.min < 45 ? 1 : 46
  end
end

describe DigidUtils::BlockScheduler do
  before do
    Timecop.freeze(Time.zone.local(2017, 5, 19, 13, 37))
  end

  after do
    Timecop.return
  end

  context "simple usage" do
    subject do
      SimpleScheduler.schedule("test")
    end

    after do
      SimpleScheduler.clear
    end

    describe "when new" do
      it "should schedule directly if new" do
        expect(subject).to eq(Time.zone.local(2017, 5, 19, 13, 37))
      end

      it "saves the block" do
        subject
        expect(SimpleScheduler.instances["test"].attributes).to eq(
          name: "test", key: "1495200600", serial: 1
        )
      end
    end

    describe "when existing" do
      before do
        SimpleScheduler.instances["test"] = SimpleScheduler.new(
          name: "test", key: "1495200600", serial: serial
        )
      end

      context "if previous room in block is not used" do
        let(:serial) { 1 }

        it "should schedule directly" do
          expect(subject).to eq(Time.zone.local(2017, 5, 19, 13, 37))
        end

        it "should update the block" do
          subject
          expect(SimpleScheduler.instances["test"].attributes).to eq(
            name: "test", key: "1495200600", serial: 2
          )
        end
      end

      context "if previous room in block is used up" do
        let(:serial) { 10 }

        it "should schedule correctly in future" do
          expect(subject).to eq(Time.zone.local(2017, 5, 19, 13, 40))
        end

        it "should update the block" do
          subject
          expect(SimpleScheduler.instances["test"].attributes).to eq(
            name: "test", key: "1495200600", serial: 11
          )
        end
      end

      context "if all room in block is used up" do
        let(:serial) { 15 }

        it "should schedule correctly in future" do
          expect(subject).to eq(Time.zone.local(2017, 5, 19, 13, 45))
        end

        it "should update the block" do
          subject
          expect(SimpleScheduler.instances["test"].attributes).to eq(
            name: "test", key: "1495201500", serial: 1
          )
        end
      end

      context "if candidate is more than maximum allowed wait time away" do
        let(:serial) { 11 }

        subject do
          SimpleScheduler.schedule("test", 3.minutes)
        end

        it "should not schedule" do
          expect(subject).to eq(nil)
        end

        it "should not update the block" do
          subject
          expect(SimpleScheduler.instances["test"].attributes).to eq(
            name: "test", key: "1495200600", serial: 11
          )
        end
      end
    end
  end

  context "advanced usage" do
    subject do
      AdvancedScheduler.schedule("test")
    end

    after do
      AdvancedScheduler.clear
    end

    describe "when new" do
      it "should schedule directly if new" do
        expect(subject).to eq(Time.zone.local(2017, 5, 19, 13, 37))
      end

      it "saves the block" do
        subject
        expect(AdvancedScheduler.instances["test"].attributes).to eq(
          name: "test", key: "L1495198800", serial: 1
        )
      end
    end

    describe "when existing" do
      before do
        AdvancedScheduler.instances["test"] = AdvancedScheduler.new(
          name: "test", key: "L1495198800", serial: serial
        )
      end

      context "if previous room in block is not used" do
        let(:serial) { 11 }

        it "should schedule directly" do
          expect(subject).to eq(Time.zone.local(2017, 5, 19, 13, 37))
        end

        it "should update the block" do
          subject
          expect(AdvancedScheduler.instances["test"].attributes).to eq(
            name: "test", key: "L1495198800", serial: 12
          )
        end
      end

      context "if previous room in block is used up" do
        let(:serial) { 13 }

        it "should schedule correctly in future" do
          expect(subject).to eq(Time.zone.local(2017, 5, 19, 13, 39))
        end

        it "should update the block" do
          subject
          expect(AdvancedScheduler.instances["test"].attributes).to eq(
            name: "test", key: "L1495198800", serial: 14
          )
        end
      end

      context "if all room in block is used up" do
        let(:serial) { 15 }

        it "should schedule correctly in future" do
          expect(subject).to eq(Time.zone.local(2017, 5, 19, 13, 45, 0))
        end

        it "should update the block" do
          subject
          expect(AdvancedScheduler.instances["test"].attributes).to eq(
            name: "test", key: "H1495198800", serial: 46
          )
        end
      end

      context "multiple with room in block till next block" do
        let(:serial) { 11 }

        subject do
          AdvancedScheduler.schedule_multiple("test", 6)
        end

        it "should schedule correctly" do
          expect(subject).to eq([
            Time.zone.local(2017, 5, 19, 13, 37, 0),
            Time.zone.local(2017, 5, 19, 13, 37, 0),
            Time.zone.local(2017, 5, 19, 13, 39, 0),
            Time.zone.local(2017, 5, 19, 13, 42, 0),
            Time.zone.local(2017, 5, 19, 13, 45, 0),
            Time.zone.local(2017, 5, 19, 13, 46, 0)
          ])
        end

        it "should update the block" do
          subject
          expect(AdvancedScheduler.instances["test"].attributes).to eq(
            name: "test", key: "H1495198800", serial: 47
          )
        end
      end
    end
  end
end
