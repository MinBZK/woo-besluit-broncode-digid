import i18next from '../../utils/i18next';
import {
  setGlobalApps,
  setGlobalCards,
  setGlobalLicences,
  setPinResetDrivingLicences,
} from '../../global/global-methods';
import { defaultFetch } from './dd.service';

export default {
  getApps() {
    return defaultFetch('apps').then(data => {
      setGlobalApps(data.app_authenticators);
      return data;
    });
  },
  getAccountData() {
    return defaultFetch('accountdata');
  },
  getDocumentData() {
    return defaultFetch('documentdata').then(data => {
      if (data.driving_licences || data.identity_cards) {
        //Parse each driving licence
        data.driving_licences.forEach(licence => {
          licence.name = i18next.t('id.driving-licence');
          licence.documentType = 'licence';
        });

        //Parse each Identity card
        data.identity_cards.forEach(id => {
          id.name = i18next.t('id.id-card');
          id.documentType = 'id';
        });

        setGlobalCards(data.identity_cards);
        setGlobalLicences(data.driving_licences);

        function moveActiveDocToFront(array) {
          array.forEach((item, i) => {
            if (item.status !== 'ingetrokken') {
              array.splice(i, 1);
              array.unshift(item);
            }
          });
        }

        //If the user has multiple documents, move active documents to the front of the array
        moveActiveDocToFront(data.driving_licences);
        moveActiveDocToFront(data.identity_cards);

        setPinResetDrivingLicences(data.pin_reset_driving_licences);
      }
      return data;
    });
  },
};
