import i18next from '../utils/i18next';

/**
 * Parse an ISO date into a readable date
 * @includeDay: whether a readable day is added, false by default
 * @longMonth: whether the full month is added, false by default
 */
export function parseDateFromISO(ISOdate, includeDay = false, longMonth = false) {
  if (!ISOdate) {
    return null;
  }
  const dutch = i18next.language === 'nl';
  const date = new Date(ISOdate);
  let day: string;
  //Compute the month and remove a dot at the end
  let month = date.toLocaleString('default', { month: longMonth ? 'long' : 'short' }).split('.')[0];

  //Compute the day string
  if (includeDay) {
    day = date.toDateString().split(' ')[0];

    //Translate the day into the correct language
    day = dutch ? returnDutchDay(day) : returnEnglishDay(day);
  }

  //Translate the month into the correct language
  month = dutch ? returnDutchMonth(month) : returnEnglishMonth(month);

  return (includeDay ? day + ' ' : '') + date.getDate() + ' ' + month + ' ' + date.getFullYear();
}

/**
 * Parse an ISO date into a readable time
 */
export function parseTimeFromISO(ISOdate, seconds = false) {
  const date = new Date(ISOdate);
  if (seconds) {
    return date.toTimeString().split(' ')[0];
  } else {
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false }).split(' ')[0];
  }
}

/**
 * Parse an ISO date into a combined date and time string
 */
export function getCompleteTime(date, longMonth) {
  return (
    parseDateFromISO(date, false, longMonth) +
    ' ' +
    i18next.t('general.at') +
    ' ' +
    parseTimeFromISO(date) +
    ' ' +
    i18next.t('general.hour')
  );
}

//Return Dutch version of a day
function returnDutchDay(day) {
  return (
    {
      Mon: 'ma',
      Tue: 'di',
      Wed: 'wo',
      Thu: 'do',
      Fri: 'vr',
      Sat: 'za',
      Sun: 'zo',
    }[day] ||
    //Return the day unaltered if it was in the Dutch format already
    day
  );
}

//Return English version of a day
function returnEnglishDay(day) {
  return (
    {
      ma: 'Mon',
      di: 'Tue',
      wo: 'Wed',
      do: 'Thu',
      vr: 'Fri',
      za: 'Sat',
      zo: 'Sun',
    }[day] ||
    //Return the day unaltered if it was in the English format already
    day
  );
}

//Return dutch version of long and short months
function returnDutchMonth(month) {
  return (
    {
      January: 'januari',
      February: 'februari',
      March: 'maart',
      Mar: 'mrt',
      May: 'mei',
      June: 'juni',
      July: 'juli',
      August: 'augustus',
      October: 'oktober',
      Oct: 'okt',
    }[month] ||
    //If none of the months fit, return the lowercase month
    month.toLowerCase()
  );
}

//Return dutch version of long and short months
function returnEnglishMonth(month) {
  return (
    {
      januari: 'January',
      februari: 'February',
      maart: 'March',
      mrt: 'Mar',
      mei: 'May',
      juni: 'June',
      juli: 'July',
      augustus: 'August',
      oktober: 'October',
      okt: 'Oct',
    }[month] ||
    //If none of the months fit, return the month with the first letter capitalized
    month.charAt(0).toUpperCase() + month.slice(1)
  );
}
