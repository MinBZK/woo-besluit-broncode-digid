
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

Nationality.seed_once(:nationalitycode, [
    {nationalitycode: 1, description_nl: "Nederlandse", description_en: "Dutch", eer: 1, start_date: Date.today, position: 1 },
    {nationalitycode: 27, description_nl: "Slowaakse", description_en: "Slovak", eer: 1, start_date: Date.today },
    {nationalitycode: 28, description_nl: "Tsjechische", description_en: "Czech", eer: 1, start_date: Date.today },
    {nationalitycode: 42, description_nl: "Sloveense", description_en: "Slovenian", eer: 1, start_date: Date.today },
    {nationalitycode: 43, description_nl: "Kroatische", description_en: "Croatian", eer: 1, start_date: Date.today },
    {nationalitycode: 44, description_nl: "Letse", description_en: "Latvian", eer: 1, start_date: Date.today },
    {nationalitycode: 45, description_nl: "Estische", description_en: "Estonian", eer: 1, start_date: Date.today },
    {nationalitycode: 46, description_nl: "Litouwse", description_en: "Lithuanian", eer: 1, start_date: Date.today },
    {nationalitycode: 52, description_nl: "Belgische", description_en: "Belgian", eer: 1, start_date: Date.today, position: 2 },
    {nationalitycode: 53, description_nl: "Bulgaarse", description_en: "Bulgarian", eer: 1, start_date: Date.today },
    {nationalitycode: 54, description_nl: "Deense", description_en: "Danish", eer: 1, start_date: Date.today },
    {nationalitycode: 55, description_nl: "Burger van de Bondsrepubliek Duitsland", description_en: "Citizen of the Federal Republic of Germany", eer: 1, start_date: Date.today, position: 3 },
    {nationalitycode: 56, description_nl: "Finse", description_en: "Finnish", eer: 1, start_date: Date.today },
    {nationalitycode: 57, description_nl: "Franse", description_en: "French", eer: 1, start_date: Date.today, position: 4 },
    {nationalitycode: 59, description_nl: "Griekse", description_en: "Greek", eer: 1, start_date: Date.today },
    {nationalitycode: 61, description_nl: "Hongaarse", description_en: "Hungarian", eer: 1, start_date: Date.today },
    {nationalitycode: 62, description_nl: "Ierse", description_en: "Irish", eer: 1, start_date: Date.today },
    {nationalitycode: 63, description_nl: "IJslandse", description_en: "Icelandic", eer: 1, start_date: Date.today },
    {nationalitycode: 64, description_nl: "Italiaanse", description_en: "Italian", eer: 1, start_date: Date.today },
    {nationalitycode: 66, description_nl: "Liechtensteinse", description_en: "Liechtenstein", eer: 1, start_date: Date.today },
    {nationalitycode: 67, description_nl: "Luxemburgse", description_en: "Luxembourgish", eer: 1, start_date: Date.today },
    {nationalitycode: 68, description_nl: "Maltese", description_en: "Maltese", eer: 1, start_date: Date.today },
    {nationalitycode: 70, description_nl: "Noorse", description_en: "Norwegian", eer: 1, start_date: Date.today },
    {nationalitycode: 71, description_nl: "Oostenrijkse", description_en: "Austrian", eer: 1, start_date: Date.today },
    {nationalitycode: 72, description_nl: "Poolse", description_en: "Polish", eer: 1, start_date: Date.today },
    {nationalitycode: 73, description_nl: "Portugese", description_en: "Portuguese", eer: 1, start_date: Date.today },
    {nationalitycode: 74, description_nl: "Roemeense", description_en: "Romanian", eer: 1, start_date: Date.today },
    {nationalitycode: 77, description_nl: "Spaanse", description_en: "Spanish", eer: 1, start_date: Date.today },
    {nationalitycode: 80, description_nl: "Zweedse", description_en: "Swedish", eer: 1, start_date: Date.today },
    {nationalitycode: 308, description_nl: "Cyprische", description_en: "Cypriot", eer: 1, start_date: Date.today }
  ]
)
