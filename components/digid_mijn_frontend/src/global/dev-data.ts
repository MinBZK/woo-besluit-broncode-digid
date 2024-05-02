export const mockedAppResponse = {
  app_authenticators: [
    {
      account_id: 14,
      activated_at: '2021-11-29T16:01:49.000+01:00',
      created_at: '2021-11-29T16:01:49.000+01:00',
      device_name: 'Huawei P30 Pro Lite Gold',
      id: 17,
      app_code: 'SSSSSSSS',
      instance_id: 'SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS',
      last_sign_in_at: '2021-11-29T16:01:49.000+01:00',
      status: 'active',
      substantieel_activated_at: '2021-11-29T16:01:49.000+01:00',
      substantieel_document_type: null,
    },
    {
      account_id: 14,
      activated_at: null,
      created_at: '2021-11-29T16:01:49.000+01:00',
      device_name: 'PPPPPPPPPPPPPPP',
      id: 19,
      app_code: 'SSSSSSSS',
      instance_id: 'SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS',
      last_sign_in_at: '2022-01-13T12:21:22.000+01:00',
      status: 'pending',
      substantieel_activated_at: '2021-11-29T16:01:49.000+01:00',
      substantieel_document_type: 'passport',
    },
  ],
  status: 'OK',
};

export const mockedNoAppResponse = {
  app_authenticators: [],
  status: 'OK',
};

export const mockAccountDataResponse = {
  status: 'OK',
  combined_account_data: {
    account_id: 14,
    bsn: 'PPPPPPPPP',
    email: {
      id: 9,
      account_id: 14,
      adres: 'PPPPPPPPPPPPPPPPPPPPPPP',
      controle_code: 'SSSSSSSSS',
      status: 'verified',
      created_at: '2021-12-02T13:37:30.000+01:00',
      updated_at: '2021-12-02T13:38:03.000+01:00',
      confirmed_at: '2021-12-02T13:38:03.000+01:00',
    },
    gesproken_sms: false,
    last_sign_in_at: '2021-12-28T13:47:34.000+01:00',
    phone_number: 'PPPPPPPPPPPP',
    pref_lang: 'nl',
    sms: 'active',
    two_faactivated_date: '2021-12-28T13:47:34.000+01:00',
    username: 'PPPPPPPPPPP',
    zekerheidsniveau: 10,
    deceased: false,
  },
};

export const mockNoAccountDataResponse = {
  status: 'OK',
  combined_account_data: {},
};

export const mockDocumentdataResponse = {
  // status_mu alleen mogelijk bij rijbewijzen
  // status_mu opties: 'actief', 'niet_actief', 'gerevorceerd'
  status: 'OK',
  pin_reset_driving_licences: true,
  show_driving_licences: false,
  show_identity_cards: true,
  rdw_error: null,
  rvig_error: null,
  driving_licences: [
    {
      doc_num: 'PPPPPPPP',
      sequence_no: 'SSSSSSSSSSSS',
      activated_at: '2021-10-24T16:01:00+02:00',
      active: false,
      status: 'niet_actief',
      status_mu: 'niet_actief',
      existing_unblock_request: true,
      paid: true,
      documentType: 'licence',
    },
  ],
  identity_cards: [
    {
      doc_num: 'PPPPPPPP',
      sequence_no: 'SSSSSSSSSSSS',
      activated_at: '2021-11-24T16:01:00+02:00',
      active: true,
      status: 'actief',
      existing_unblock_request: true,
      paid: true,
      documentType: 'id',
    },
    {
      doc_num: 'PPPPPPPP',
      sequence_no: 'SSSSSSSSSSSS',
      activated_at: '2021-10-25T16:01:00+02:00',
      active: false,
      status: 'uitgereikt',
      existing_unblock_request: false,
      paid: true,
      documentType: 'id',
    },
    {
      doc_num: 'PPPPPPPP',
      sequence_no: 'SSSSSSSSSSSS',
      activated_at: '2021-10-26T16:01:00+02:00',
      active: false,
      status: 'ingetrokken',
      existing_unblock_request: false,
      paid: true,
      documentType: 'id',
    },
  ],
};

export const mockRevokedDocumentdataResponse = {
  status: 'OK',
  show_driving_licences: true,
  show_identity_cards: true,
  rdw_error: 'contact',
  rvig_error: null,
  driving_licences: [],
  identity_cards: [
    {
      doc_num: 'PPPPPPPP',
      sequence_no: 'SSSSSSSSSSSS',
      activated_at: '2021-10-24T16:01:00+02:00',
      active: false,
      status: 'ingetrokken',
      existing_unblock_request: false,
      paid: true,
      documentType: 'id',
    },
  ],
};

export const mockNoDocumentdataResponse = {
  status: 'OK',
  driving_licences: [],
  identity_cards: [],
  show_identity_cards: true,
  show_driving_licences: true,
  rdw_error: null,
  rvig_error: null,
};

export const mockedUserHistoryResponse = {
  status: 'OK',
  total_items: 10,
  total_pages: 1,
  account_logs: [
    { id: 1, name: 'Nieuwe SSO sessie domein  geïnitieerd', created_at: '2021-03-25T18:02:56.000+01:00' },
    { id: 2, name: 'Webdienst authenticatieverzoek SAML actie: login', created_at: '2021-03-25T18:02:56.000+01:00' },
    { id: 3, name: 'Inloggen start', created_at: '2021-03-25T18:02:56.000+01:00' },
    {
      id: 4,
      name: 'Inloggen met rijbewijs of identiteitskaart via de DigiD app gekozen',
      created_at: '2021-03-25T18:03:00.000+01:00',
    },
    {
      id: 5,
      name: 'Inloggen met rijbewijs niet gelukt, technische error',
      created_at: '2021-03-25T18:03:03.000+01:00',
    },
    { id: 6, name: 'Inloggen start', created_at: '2021-03-25T18:03:05.000+01:00' },
    { id: 7, name: 'Inloggen beheermodule gelukt', created_at: '2021-03-25T18:03:08.000+01:00' },
    { id: 8, name: 'Dashboard inzien gelukt', created_at: '2021-03-25T18:03:08.000+01:00' },
    {
      id: 9,
      name: 'Beheersessie verlopen, time-out vanwege inactiviteit',
      created_at: '2021-03-25T18:18:15.000+01:00',
    },
    {
      id: 10,
      name: 'Inloggen met rijbewijs of identiteitskaart via de DigiD app gekozen',
      created_at: '2021-03-25T18:18:55.000+01:00',
    },
  ],
};

export const mockHistorySuggestions = JSON.stringify([
  { title: 'test' },
  { title: 'new test' },
  { title: 'other test' },
]);

const devNotification = {
  warning: 'You are on DEV mode and test data is used. Switch test users with the buttons',
};

const devConfig = {
  analytics: {
    enabled: false,
    host: 'SSSSSSSSSSSSSSSSSSS',
    site_id: 11,
  },
};

export default {
  1: {
    'notification': { ...devNotification },
    'apps': { ...mockedAppResponse },
    'accountdata': { ...mockAccountDataResponse },
    'documentdata': { ...mockDocumentdataResponse },
    'accounthistory?page=1&query=': { ...mockedUserHistoryResponse },
    'config': { ...devConfig },
  },
  2: {
    'notification': { ...devNotification },
    'apps': { ...mockedNoAppResponse },
    'accountdata': { ...mockAccountDataResponse },
    'documentdata': { ...mockNoDocumentdataResponse },
    'accounthistory?page=1&query=': { ...mockedUserHistoryResponse },
    'config': { ...devConfig },
  },
  3: {
    'notification': { ...devNotification },
    'apps': { ...mockedAppResponse },
    'accountdata': { ...mockNoAccountDataResponse },
    'documentdata': { ...mockRevokedDocumentdataResponse },
    'accounthistory?page=1&query=': { ...mockedUserHistoryResponse },
    'config': { ...devConfig },
  },
};
