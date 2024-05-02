
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

class Account < AccountBase
  module Status
    INITIAL   = "initial"   # Initieel
    REQUESTED = "requested" # Aangevraagd
    ACTIVE    = "active"    # Actief
    REVOKED   = "revoked"   # Gerevoceerd door bemiddelende instantie (bijv. SVB)
    SUSPENDED = "suspended" # Opgeschort
    EXPIRED   = "expired"   # Vervallen
    REMOVED   = "removed"   # Opgeheven
  end

  include Stateful

  LEVELS = { 9 => "light", 10 => "basis", 20 => "midden", 25 => "substantieel", 30 => "hoog" }.freeze

  has_many :logs
  has_one :email, dependent: :destroy
  has_one :distribution_entity
  has_one :password_tool, dependent: :destroy
  has_many :sms_tools, dependent: :destroy
  has_many :app_authenticators, dependent: :destroy, class_name: "Authenticators::AppAuthenticator"
  has_many :sms_challenges
  has_many :recovery_code_attempts,
           -> { where attempt_type: "recover" },
           as: :attemptable,
           dependent: :destroy
  has_many :login_attempts,
           -> { where attempt_type: "login" },
           as: :attemptable,
           dependent: :destroy
  has_many :sectorcodes, dependent: :destroy
  has_many :sectorcodes_history # Never destroy for logging purposes
  has_many :account_histories
  has_many :sent_emails

  delegate :adres, to: :email
  delegate :balie_id, to: :distribution_entity, prefix: true

  alias_attribute :name, :gebruikersnaam

  # Destroy / delete callbacks
  before_destroy :register_sectorcodes_history, prepend: true
  before_destroy :remove_afnemersindicatie, prepend: true
  before_destroy :create_account_history, prepend: true

  def register_sectorcodes_history
    return if SectorcodesHistory.exists?(account_id: id)

    sectorcodes = Sectorcode.select(:sector_id, :sectoraalnummer).where(account_id: id)
    sectorcodes.each do |sectorcode|
      SectorcodesHistory.create(account_id: id, sector_id: sectorcode.sector_id, sectoraalnummer: sectorcode.sectoraalnummer)
    end
  end

  def remove_afnemersindicatie
    other_bsn_accounts = Account.with_bsn(bsn).where.not(id: id)
    if other_bsn_accounts.count == 0
      sectorcode_a_nummer = Sectorcode.where("account_id = ?", id).find_by("sector_id = ?", Sector.get("a-nummer"))
      if (sectorcode_a_nummer&.sectoraalnummer?)
        response = DigidUtils::Iapi::Client.new(url: APP_CONFIG["urls"]["internal"]["dgl"], timeout: 30).delete("/iapi/afnemersindicatie/" + sectorcode_a_nummer.sectoraalnummer)
        if(response.code == 200)
          Log.instrument("1496", account_id: id, hidden: true)
        else
          Log.instrument("1495", account_id: id, hidden: true)
        end
      end
    end
  end

  def create_account_history
    account_histories.create gebruikersnaam: password_tool&.username, mobiel_nummer: phone_number, email_adres: email.try(:adres)
    remove_data = {
      username: password_tool&.username, phone_number: phone_number, spoken_sms: I18n.t(gesproken_sms),
      email: email.try(:adres), email_checked: I18n.t(email.try(:status) == Email::Status::VERIFIED),
      phone_name: app_authenticators.pluck(:device_name).join(","),
      sub_id_type: app_authenticators.pluck(:substantieel_document_type).join(",")
    }
    if app_authenticator_active? && app_authenticators.substantieel.any?
      remove_data[:betrouwbaarheidsniveau] = 25
    elsif sms_tools.active? || app_authenticators.any?
      remove_data[:betrouwbaarheidsniveau] = 20
    else
      remove_data[:betrouwbaarheidsniveau] = 10
    end
    Log.instrument("412", account_id: id, hidden: true, **remove_data)
  end

  def gebruikersnaam
    password_tool&.username
  end

  GBA_STATUS_MAPPINGS = {
    valid: "Actief",
    not_found: "Niet gevonden",
    investigate: "In onderzoek",
    deceased: "Overleden",
    emigrated: "Geëmigreerd",
    rni: "Register Niet Ingezetenen (RNI)",
    suspended_error: "Inschrijving opgeschort wegens fout",
    suspended_unknown: "Inschrijving opgeschort, reden onbekend",
    error: "Error"
  }.freeze

  GBA_TITLE_MAPPINGS = {
    B: "Baron",    BS: "Barones",
    G: "Graaf",    GI: "Gravin",
    H: "Hertog",   HI: "Hertogin",
    JH: "Jonkheer", JV: "Jonkvrouw",
    M: "Markies",  MI: "Markiezin",
    P: "Prins",    PI: "Prinses",
    R: "Ridder"
  }.freeze

  GBA_GENDER_MAPPINGS = {
    M: "Man",
    V: "Vrouw",
    O: "Onzijdig"
  }.freeze

  GBA_TRAVEL_DOCUMENT_TYPE_MAPPINGS = {
    NI: "Nederlandse identiteitskaart (NI)",
    PN: "Nationaal paspoort (PN)",
    TN: "Tweede paspoort (TN)",
    ZN: "Nationaal paspoort – zakenpaspoort (ZN)",
    TE: "Tweede paspoort – zakenpaspoort (TE)",
    PF: "Faciliteitenpaspoort (PF)",
    PB: "Reisdocument voor vreemdelingen (PB)",
    PV: "Reisdocument voor vluchtelingen (PV)",
    PD: "Diplomatiek paspoort (PD)",
    PZ: "Dienstpaspoort (PZ)",
    LP: "Laissez-passer (LP)",
    NN: "Noodpaspoort (NN)"
  }.freeze

  GBA_TRAVEL_DOCUMENT_STATE_MAPPINGS = {
    I: "Ingehouden / ingeleverd (I)",
    R: "Van rechtswege vervallen (R)",
    V: "Vermist (V)"
  }.freeze

  GBA_NATIONALITY_MAPPINGS = {
    "0000": "Onbekend",
    "0001": "Nederlandse",
    "0002": "Behandeld als Nederlander",
    "0027": "Slowaakse",
    "0028": "Tsjechische",
    "0029": "Burger van Bosnië-Herzegovina",
    "0030": "Burger van Georgië",
    "0031": "Burger van Toerkmenistan",
    "0032": "Burger van Tadzjikistan",
    "0033": "Burger van Oezbekistan",
    "0034": "Burger van Oekraine",
    "0035": "Burger van Kyrgyzstan",
    "0036": "Burger van Moldavië",
    "0037": "Burger van Kazachstan",
    "0038": "Burger van Belarus (Wit-Rusland)",
    "0039": "Burger van Azerbajdsjan",
    "0040": "Burger van Armenië",
    "0041": "Burger van Rusland",
    "0042": "Burger van Slovenië",
    "0043": "Burger van Kroatië",
    "0044": "Letse",
    "0045": "Estnische",
    "0046": "Litouwse",
    "0047": "Burger van de Marshalleilanden",
    "0048": "Myanmarese",
    "0049": "Namibische",
    "0050": "Albanese",
    "0051": "Andorrese",
    "0052": "Belgische",
    "0053": "Bulgaarse",
    "0054": "Deense",
    "0055": "Burger van de Bondsrepubliek Duitsland",
    "0056": "Finse",
    "0057": "Franse",
    "0058": "Jemenitische",
    "0059": "Griekse",
    "0060": "Brits burger",
    "0061": "Hongaarse",
    "0062": "Ierse",
    "0063": "IJslandse",
    "0064": "Italiaanse",
    "0065": "Joegoslavische",
    "0066": "Liechtensteinse",
    "0067": "Luxemburgse",
    "0068": "Maltese",
    "0069": "Monegaskische",
    "0070": "Noorse",
    "0071": "Oostenrijkse",
    "0072": "Poolse",
    "0073": "Portugese",
    "0074": "Roemeense",
    "0075": "Burger Sovjetunie",
    "0076": "Sanmarinese",
    "0077": "Spaanse",
    "0078": "Tsjechoslowaakse",
    "0079": "Vaticaanse",
    "0080": "Zweedse",
    "0081": "Zwitserse",
    "0082": "Oostduitse",
    "0083": "Brits onderdaan",
    "0084": "Eritrese",
    "0085": "Brits overzees burger",
    "0086": "Macedonische",
    "0087": "Burger van Kosovo",
    "0100": "Algerijnse",
    "0101": "Angolese",
    "0104": "Burundische",
    "0105": "Botswaanse",
    "0106": "Burger van Burkina Faso",
    "0108": "Centrafrikaanse",
    "0109": "Comorese",
    "0110": "Kongolese",
    "0111": "Beninse",
    "0112": "Egyptische",
    "0113": "Equatoriaalguinese",
    "0114": "Etiopische",
    "0115": "Djiboutiaanse",
    "0116": "Gabonese",
    "0117": "Gambiaanse",
    "0118": "Ghanese",
    "0119": "Guinese",
    "0120": "Ivoriaanse",
    "0121": "Kaapverdische",
    "0122": "Kameroense",
    "0123": "Kenyaanse",
    "0124": "Zaïrese",
    "0125": "Lesothaanse",
    "0126": "Liberiaanse",
    "0127": "Libische",
    "0128": "Malagassische",
    "0129": "Malawische",
    "0130": "Malinese",
    "0131": "Marokkaanse",
    "0132": "Burger van Mauritanië",
    "0133": "Burger van Mauritius",
    "0134": "Mozambiquaanse",
    "0135": "Swazische",
    "0136": "Burger van Niger",
    "0137": "Burger van Nigeria",
    "0138": "Ugandese",
    "0139": "Guineebissause",
    "0140": "Zuidafrikaanse",
    "0142": "Zimbabwaanse",
    "0143": "Rwandese",
    "0144": "Burger van São Tomé en Principe",
    "0145": "Senegalese",
    "0147": "Sierraleoonse",
    "0148": "Soedanese",
    "0149": "Somalische",
    "0151": "Tanzaniaanse",
    "0152": "Togolese",
    "0154": "Tsjadische",
    "0155": "Tunesische",
    "0156": "Zambiaanse",
    "0157": "Zuidsoedanese",
    "0200": "Bahamaanse",
    "0202": "Belizaanse",
    "0204": "Canadese",
    "0205": "Costaricaanse",
    "0206": "Cubaanse",
    "0207": "Burger van Dominicaanse Republiek",
    "0208": "Salvadoraanse",
    "0211": "Guatemalteekse",
    "0212": "Haïtiaanse",
    "0213": "Hondurese",
    "0214": "Jamaicaanse",
    "0216": "Mexicaanse",
    "0218": "Nicaraguaanse",
    "0219": "Panamese",
    "0222": "Burger van Trinidad en Tobago",
    "0223": "Amerikaans burger",
    "0250": "Argentijnse",
    "0251": "Barbadaanse",
    "0252": "Boliviaanse",
    "0253": "Braziliaanse",
    "0254": "Chileense",
    "0255": "Colombiaanse",
    "0256": "Ecuadoraanse",
    "0259": "Guyaanse",
    "0261": "Paraguayaanse",
    "0262": "Peruaanse",
    "0263": "Surinaamse",
    "0264": "Uruguayaanse",
    "0265": "Venezolaanse",
    "0267": "Grenadaanse",
    "0268": "Burger van Saint Kitts-Nevis",
    "0300": "Afghaanse",
    "0301": "Bahreinse",
    "0302": "Bhutaanse",
    "0303": "Burmaanse",
    "0304": "Bruneise",
    "0305": "Kambodjaanse",
    "0306": "Srilankaanse",
    "0307": "Chinese",
    "0308": "Cyprische",
    "0309": "Filipijnse",
    "0310": "Taiwanese",
    "0312": "Burger van India",
    "0313": "Indonesische",
    "0314": "Iraakse",
    "0315": "Iraanse",
    "0316": "Israëlische",
    "0317": "Japanse",
    "0318": "Noordjemenitische",
    "0319": "Jordaanse",
    "0320": "Koeweitse",
    "0321": "Laotiaanse",
    "0322": "Libanese",
    "0324": "Maldivische",
    "0325": "Maleisische",
    "0326": "Mongolische",
    "0327": "Omanitische",
    "0328": "Nepalese",
    "0329": "Noordkoreaanse",
    "0331": "Pakistaanse",
    "0333": "Katarese",
    "0334": "Saoediarabische",
    "0335": "Singaporaanse",
    "0336": "Syrische",
    "0337": "Thaise",
    "0338": "Burger van de Ver.  Arabische Emiraten",
    "0339": "Turkse",
    "0340": "Zuidjemenitische",
    "0341": "Zuidkoreaanse",
    "0342": "Viëtnamese",
    "0345": "Burger van Bangladesh",
    "0400": "Australische",
    "0401": "Burger van Papua-Nieuwguinea",
    "0402": "Nieuwzeelandse",
    "0404": "Westsamoaanse",
    "0421": "Burger van Antigua en Barbuda",
    "0424": "Vanuatuse",
    "0425": "Fijische",
    "0429": "Burger van Britse afhankelijke gebieden",
    "0430": "Tongaanse",
    "0431": "Nauruaanse",
    "0437": "Amerikaans onderdaan",
    "0442": "Solomoneilandse",
    "0444": "Seychelse",
    "0445": "Kiribatische",
    "0446": "Tuvaluaanse",
    "0447": "Sintluciaanse",
    "0448": "Burger van Dominica",
    "0449": "Burger van Sint Vincent en de Grenadinen",
    "0450": "British National (overseas)",
    "0451": "Zaïrese (Congolese)",
    "0452": "Burger van Timor Leste",
    "0453": "Burger van Servië en Montenegro",
    "0454": "Burger van Servië",
    "0455": "Burger van Montenegro",
    "0499": "Staatloos",
    "0500": "Vastgesteld niet-Nederlander"
  }.freeze

  scope :with_bsn, lambda { |bsn|
    joins(:sectorcodes)
      .where(sectorcodes: { sectoraalnummer: bsn, sector_id: Sector.get("bsn") })
  }

  def email_address_present?
    email&.adres.present?
  end

  # check to see if this account is allowed to send emails to users
  def send_email?
    email&.state&.verified?
  end

  # returns the expiry date, which is one year after last logon
  def vervaldatum
    digid_expires_in = expire_after_months
    if digid_expires_in.positive?
      if current_sign_in_at
        I18n.l(current_sign_in_at + digid_expires_in.months, format: :slash)
      else
        I18n.l(created_at + digid_expires_in.months, format: :slash)
      end
    else
      I18n.t("niet_van_toepassing")
    end
  end

  # Amount of authentication attempts
  def login_pogingen
    login_attempts.count
  end

  # Amount of recovery attempts
  def recovery_attempts
    recovery_code_attempts.count
  end

  def mobiel_kwijt_in_progress_activatable?
    sms_tools.pending? && sms_tools.active?
  end

  def mobiel_kwijt_in_progress?
    mobiel_kwijt_in_progress_activatable? && !pending_sms_tool.expired?
  end

  # return if the account is in sms upgrade
  def sms_in_uitbreiding?
    sms_tool.present? && sms_tool.state.pending? && password_tool.present? && !password_tool.expired?
  end

  def sms_tool_active?
    active_sms_tool.present?
  end

  def active_sms_tool
    sms_tools.active.first
  end

  def pending_sms_tool
    sms_tools.pending.first
  end

  def valid_sms_tools
    sms_tools.where("status in (?) or activation_code IS NOT NULL", [SmsTool::Status::ACTIVE, SmsTool::Status::PENDING])
  end

  def sms_tool
    Rails.logger.info "INFO > sms_tool via authenticator requested"
    Rails.logger.warn "WARN > More then one sms_tool found" if sms_tools.count > 1
    sms_tools.last
  end

  def pending_phone_number
    pending_sms_tool.try(:phone_number)
  end

  def phone_number
    sms_tools.active.first.try(:phone_number)
  end

  def pending_gesproken_sms
    pending_sms_tool.try(:gesproken_sms) || false
  end

  def gesproken_sms
    active_sms_tool.try(:gesproken_sms) || false
  end

  def app_authenticator_active?
    app_authenticators.active.any?
  end

  def app_authenticator_pending?
    app_authenticators.pending.any?
  end

  def active_midden_authenticators
    list = []
    list << two_factor.active_authenticators
    list << app_authenticators.active if app_authenticator_active?
    list.flatten.compact
  end

  def type_account
    if status == "requested"
      if app_authenticators.pending.find(&:substantieel?)
        return I18n.t("accounts.fields.labels.substantieel")
      elsif app_authenticators.pending.any? || pending_sms_tool
        return I18n.t("accounts.fields.labels.midden")
      end
    elsif app_authenticators.active.find(&:substantieel?)
      return I18n.t("accounts.fields.labels.substantieel")
    elsif active_midden_authenticators.any?
      return I18n.t("accounts.fields.labels.midden")
    end

    I18n.t("accounts.fields.labels.basis")
  end

  def human_status
    I18n.t(status, scope: "accounts.states")
  end

  def show_front_desk_name
    front_desk = FrontDesk.find(distribution_entity.balie_id)
    front_desk.name
  rescue StandardError
  end

  # return a burgerservicenummer
  def bsn
    # FIXME: We now seem to 'hope' that we've inserted
    # a BSN as first Sectorcode.
    sectorcodes.first.sectoraalnummer
  rescue NoMethodError
    # FIXME: We raise a not understandable error when there is no BSN.
    raise inspect.to_s
  end

  # Amount of recovery attempts
  def recovery_requests_last_month
    Registration.month_count(bsn)
  end

  # Amount of recovery attempts through wachtwoordherstel per email
  def recovery_requests_by_email
    sent_emails.where(reason: SentEmail::Reason::RECOVERY).where("created_at > ?", Time.now - 1.month).count
  end

  # Amount of sms challenges attempts
  def sms_attempts
    sms_challenges.sum("attempt")
  end

  # Get the last known date for a Sms challenge attempt
  def sms_last_date_attempt(sms_tool)
    sms_challenges.find_by(mobile_number: sms_tool.phone_number).try(:updated_at)
  end

  def via_balie?
    distribution_entity.present?
  end

  # change the sector for accounts whom now are bsn
  def convert_sofi_to_bsn
    sectorcode = Sectorcode.where("account_id = ?", id).find_by("sector_id = ?", Sector.get("sofi"))
    sectorcode.sector_id = Sector.get("bsn")
    sectorcode.save!
  end

  # try to add an a-nummer to the account, you can't add an a-nummer when it occurs in another digid account
  def add_a_nummer?(a_nummer)
    accounts_with_this_a_nr = Sectorcode.where(sector_id: Sector.get("a-nummer")).where(sectoraalnummer: a_nummer)

    account_ids = accounts_with_this_a_nr.map(&:account_id)
    # we can add a_nr if count=0 and return true to signal set_sofi_to_bsn
    if account_ids.empty?
      sectorcodes.create do |code|
        code.sector_id = Sector.get("a-nummer")
        code.sectoraalnummer = a_nummer
      end
      true
    elsif account_ids.size == 1 && account_ids.first == id # if count=1 and it's the same account, do nothing and return true to signal set_sofi_to_bsn
      true
    else
      false # a_nummer occurs in another digid account, notify admin and return false to deny set_sofi_to_bsn
    end
  end

  # tells if an account is suspended, but could still be unsuspended
  def unsuspendable
    expire_date = (Time.now - expire_after_months.months).to_date
    # Can't determine how old an account if it's never logged in
    return false unless current_sign_in_at

    current_sign_in_at > expire_date
  end

  def expire_after_months
    # Get the expire time for each sector this account is in
    expire_times = [0] << sectorcodes.map { |sectorcode| sectorcode.sector.valid_for }
    # Return the high expiration date
    expire_times.flatten.compact.max
  end

  def initial?
    status == Status::INITIAL
  end

  def blocked?
    blocked_till && blocked_till > Time.now
  end

  def all_blocked?
    blocked? && !app_authenticator_active?
  end

  def email_activated?
    email_address_present? && email.state.verified?
  end

  def two_factor
    @two_factor ||= Authenticators::TwoFactor.new(self)
  end

  def wachtwoordherstel_allowed?
    email_activated? && sms_tool_active?
  end

  def replace_blockable?
    [Status::ACTIVE, Status::SUSPENDED].include?(status)
  end
end
