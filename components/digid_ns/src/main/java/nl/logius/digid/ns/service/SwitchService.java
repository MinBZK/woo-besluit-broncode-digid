
/*
  Deze broncode is openbaar gemaakt vanwege een Woo-verzoek zodat deze
  gericht is op transparantie en niet op hergebruik. Hergebruik van 
  de broncode is toegestaan onder de EUPL licentie, met uitzondering 
  van broncode waarvoor een andere licentie is aangegeven.
  
  Het archief waar dit bestand deel van uitmaakt is te vinden op:
    https://github.com/MinBZK/woo-besluit-broncode-digid
  
  Eventuele kwetsbaarheden kunnen worden gemeld bij het NCSC via:
    https://www.ncsc.nl/contact/kwetsbaarheid-melden
  onder vermelding van "Logius, openbaar gemaakte broncode DigiD" 
  
  Voor overige vragen over dit Woo-besluit kunt u mailen met open@logius.nl
  
  This code has been disclosed in response to a request under the Dutch
  Open Government Act ("Wet open Overheid"). This implies that publication 
  is primarily driven by the need for transparence, not re-use.
  Re-use is permitted under the EUPL-license, with the exception 
  of source files that contain a different license.
  
  The archive that this file originates from can be found at:
    https://github.com/MinBZK/woo-besluit-broncode-digid
  
  Security vulnerabilities may be responsibly disclosed via the Dutch NCSC:
    https://www.ncsc.nl/contact/kwetsbaarheid-melden
  using the reference "Logius, publicly disclosed source code DigiD" 
  
  Other questions regarding this Open Goverment Act decision may be
  directed via email to open@logius.nl
*/

package nl.logius.digid.ns.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;

import nl.logius.digid.ns.model.Switch;
import nl.logius.digid.ns.model.SwitchStatus;
import nl.logius.digid.ns.repository.SwitchRepository;

@Service
public class SwitchService extends DatabaseService<Switch> implements ApplicationListener<ContextRefreshedEvent> {
    private final static String OBJECT_TYPE = "switch";
    private final static String FILE_LOCATION = "data/switches.yml";
    private final static Class MAPPING_CLASS = Switch.class;

    @Autowired
    public SwitchService(SwitchRepository repository) {
        super(repository);
    }

    @Override
    protected Map<String, String> compareObjects(Switch oldSwitch, Switch updatedSwitch) {
        final Map<String, String> changes = new HashMap<>();

        if (!oldSwitch.getName().equals(updatedSwitch.getName())) {
            oldSwitch.setName(updatedSwitch.getName());
            changes.put("name", oldSwitch.getName());
        }
        if (!oldSwitch.getDescription().equals(updatedSwitch.getDescription())) {
            oldSwitch.setDescription(updatedSwitch.getDescription());
            changes.put("description", oldSwitch.getDescription());
        }
        if (oldSwitch.getPilotGroupId() != updatedSwitch.getPilotGroupId()) {
            oldSwitch.setPilotGroupId(updatedSwitch.getPilotGroupId());
            changes.put("pilot_group_id", String.valueOf(oldSwitch.getPilotGroupId()));
        }

        if (!changes.isEmpty()) {
            oldSwitch.setDates();
        }

        return changes;
    }

    @Override
    public String getObjectType() {
        return OBJECT_TYPE;
    }

    @Override
    public String getFileLocation() {
        return FILE_LOCATION;
    }

    @Override
    public Class getMappingClass() {
        return MAPPING_CLASS;
    }

    public boolean isMnsSwitchActive(){
        Optional<Switch> sw = repository.findById(Switch.MNS_SWITCH_ID);
        return (sw.isPresent() && sw.get().getStatus() != SwitchStatus.INACTIVE);
    }

    public boolean isFcmSwitchActive(){
        Optional<Switch> sw = repository.findById(Switch.FCM_SWITCH_ID);
        return (sw.isPresent() && sw.get().getStatus() != SwitchStatus.INACTIVE);
    }

    public boolean isApnsViaFcmSwitchActive(){
        Optional<Switch> sw = repository.findById(Switch.APNS_SWITCH_ID);
        return (isFcmSwitchActive() && sw.isPresent() && sw.get().getStatus() == SwitchStatus.APNS_VIA_FCM );
    }

    public boolean isApnsDirectSwitchActive(){
        Optional<Switch> sw = repository.findById(Switch.APNS_SWITCH_ID);
        return (sw.isPresent() && sw.get().getStatus() != SwitchStatus.INACTIVE);
    }
}
