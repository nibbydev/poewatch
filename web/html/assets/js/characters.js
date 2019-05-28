/**
 * Table row
 */
class UserRow {
  constructor(user) {
    this.user = user;

    this.search = SEARCH.search;
    this.mode = SEARCH.mode;
  }

  /**
   * Builds the table row for a user entry
   *
   * @returns {string} Row HTML
   */
  buildRow() {
    // get correct name depending on the mode
    const account = this.mode === 'account' ? this.search : this.user.account;
    const character = this.mode === 'character' ? this.search : this.user.character;

    const accountDisplay = this.mode === 'account'
      ? `<span class="custom-text-orange">${account}</span>`
      : `<span>${account}</span>`;
    const characterDisplay = this.mode === 'character'
      ? `<span class="custom-text-orange">${character}</span>`
      : `<span>${character}</span>`;

    // build the table row
    return `<tr>
  <td class="text-nowrap">
    <a href='characters?mode=account&search=${account}'>
      <span class="custom-text-gray-lo">${accountDisplay}</span>
    </a>
    <a class="custom-text-gray" href='https://www.pathofexile.com/account/view-profile/${account}' target="_blank">⬈</a>
  </td>
  <td>
    <a href='characters?mode=character&search=${character}'>
      <span class="custom-text-gray-lo">${characterDisplay}</span>
    </a>
  </td>
  <td class="badge">${this.user.league ? this.user.league : '-'}</td>
  <td class="text-nowrap custom-text-gray-lo">
    <span class="badge p-0">${UserRow.timeSince(this.user.found)}</span>
  </td>
  <td class="text-nowrap custom-text-gray-lo">
    <span class="badge p-0">${UserRow.timeSince(this.user.seen)}</span>
  </td>
</tr>`;
  }

  /**
   * Formats a timestamp string
   *
   * @param timeStamp
   * @returns {string}
   */
  static timeSince(timeStamp) {
    timeStamp = new Date(timeStamp);

    let now = new Date(),
      secondsPast = (now.getTime() - timeStamp.getTime()) / 1000;

    if (secondsPast < 60) {
      return parseInt(secondsPast) + 's';
    }

    if (secondsPast < 3600) {
      return parseInt(secondsPast / 60) + 'm';
    }

    if (secondsPast <= 86400) {
      return parseInt(secondsPast / 3600) + 'h';
    }

    if (secondsPast > 86400) {
      let day = timeStamp.getDate();
      let month = timeStamp.toDateString().match(/ [a-zA-Z]*/)[0].replace(" ", "");
      let year = timeStamp.getFullYear() == now.getFullYear() ? "" : " " + timeStamp.getFullYear();
      return day + " " + month + year;
    }
  }
}

/**
 * Deals with handling and writing query parameters
 */
class QueryAccessor {
  /**
   * Set query param
   *
   * @param key
   * @param value
   */
  static updateQueryParam(key, value) {
    let url = document.location.href;
    let re = new RegExp("([?&])" + key + "=.*?(&|#|$)(.*)", "gi");
    let hash;

    if (re.test(url)) {
      if (typeof value !== 'undefined' && value !== null) {
        url = url.replace(re, '$1' + key + "=" + value + '$2$3');
      } else {
        hash = url.split('#');
        url = hash[0].replace(re, '$1$3').replace(/([&?])$/, '');

        if (typeof hash[1] !== 'undefined' && hash[1] !== null) {
          url += '#' + hash[1];
        }
      }
    } else if (typeof value !== 'undefined' && value !== null) {
      let separator = url.indexOf('?') !== -1 ? '&' : '?';

      hash = url.split('#');
      url = hash[0] + separator + key + '=' + value;

      if (typeof hash[1] !== 'undefined' && hash[1] !== null) {
        url += '#' + hash[1];
      }
    }

    history.replaceState({}, "foo", url);
  }

  /**
   * Read query param
   *
   * @param key
   * @returns {string|null}
   */
  static parseQueryParam(key) {
    let url = window.location.href;
    key = key.replace(/[\[\]]/g, '\\$&');

    let regex = new RegExp('[?&]' + key + '(=([^&#]*)|&|#|$)');
    let results = regex.exec(url);

    if (!results) return null;
    if (!results[2]) return '';

    return decodeURIComponent(results[2].replace(/\+/g, ' '));
  }
}

const API_URL = 'https://api.poe.watch';
const SEARCH = {
  search: null,
  mode: 'account',
  results: {}
};

$(document).ready(function () {
  defineListeners();
  parseQueryParams();

  if (SEARCH.mode && SEARCH.search) {
    makeGetRequest(SEARCH.mode, SEARCH.search);
  }
});


/**
 * Shows (or hides) a status message
 *
 * @param msg
 * @param isError
 */
function statusMsg(msg, isError) {
  const div = $('#search-status');

  // Clear all classes
  div.removeClass();

  // If no message is provided, hide the status div
  if (msg) {
    div.addClass(isError ? 'custom-text-red' : 'custom-text-green');
    div.html(msg);
  } else {
    div.addClass('d-none');
  }
}

/**
 * Creates listener events
 */
function defineListeners() {
  $('#search-input').on('input', function (e) {
    if (e.target.value.trim()) {
      SEARCH.search = e.target.value.trim();
      console.log('Search: ' + SEARCH.search);
    } else {
      SEARCH.search = null;
      console.log('Search cleared');
    }

    QueryAccessor.updateQueryParam('search', SEARCH.search);
  });

  $('#search-btn').on('click', function (e) {
    QueryAccessor.updateQueryParam('mode', SEARCH.mode);

    if (!SEARCH.search) {
      statusMsg('Enter an account name', true);
      return;
    } else if (SEARCH.search.length < 3) {
      statusMsg('Name is too short', true);
      return;
    } else if (SEARCH.search.length > 64) {
      statusMsg('Name is too long', true);
      return;
    }

    // If same search has already been made
    const json = SEARCH.results[SEARCH.mode + SEARCH.search];
    if (json !== undefined) {
      statusMsg(`Loaded ${json.length} entries from memory`);
      fillTable(json);

      return;
    }

    statusMsg();
    makeGetRequest(SEARCH.mode, SEARCH.search);
  });

  $('#search-mode').on('change', function (e) {
    SEARCH.mode = e.target.value;
    console.log('Mode: ' + SEARCH.mode);
    QueryAccessor.updateQueryParam('mode', SEARCH.mode);
  });
}

/**
 * Loads and processes query parameters on initial page load
 */
function parseQueryParams() {
  const mode = QueryAccessor.parseQueryParam('mode');
  if (mode) {
    SEARCH.mode = mode;
    $('#search-mode').val(mode);
  }

  const search = QueryAccessor.parseQueryParam('search');
  if (search) {
    $('#search-input').val(search);
    SEARCH.search = search;
  }
}

/**
 * Makes request to api
 *
 * @param mode
 * @param search
 */
function makeGetRequest(mode, search) {
  const spinner = $('#spinner');
  spinner.removeClass('d-none');

  const endpoint = mode === 'account' ? 'characters' : 'accounts';
  const payload = {};
  payload[mode] = search;

  let request = $.ajax({
    url: `${API_URL}/${endpoint}`,
    data: payload,
    type: "GET",
    async: true,
    dataTypes: "json"
  });

  request.done(function (json) {
    SEARCH.results[mode + search] = json;

    if (json.length === 0) {
      statusMsg(`No characters found`, true);
    } else {
      statusMsg(`Found ${json.length} characters`);
    }

    $('#search-results').removeClass('d-none');

    // Sort descending
    json.sort(SEARCH.sortFunction);
    fillTable(json);
    spinner.addClass('d-none');
  });

  request.fail(function (response) {
    console.log(response);
    SEARCH.results[mode + search] = null;

    statusMsg(response.responseJSON.error);
    spinner.addClass('d-none');
  });
}

/**
 * Fills main table with data
 *
 * @param users
 */
function fillTable(users) {
  const table = $('#search-results > tbody');

  // if no data was provided, clear the table
  if (!users) {
    table.html();
  }

  let builder = '';
  for (let i = 0; i < users.length; i++) {
    // If the row hasn't been processed yet
    if (!users[i].html) {
      const user = new UserRow(users[i]);
      users[i].html = user.buildRow();
    }

    // add to builder
    builder += users[i].html;
  }

  table.html(builder);
}
