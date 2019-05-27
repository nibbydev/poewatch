/**
 * Table row
 */
class Row {
  constructor(item) {
    this.item = item;

    this.name = this.formatName();
    this.properties = this.formatProperties();
    this.price = this.formatPrice();
  }

  /**
   * Creates formatted title for the item
   *
   * @returns {string}
   */
  formatName() {
    // If item is enchantment, insert enchant values for display purposes
    if (this.item.category === 'enchantment') {
      // Min roll
      if (this.item.name.includes('#') && this.item.enchantMin !== null) {
        this.item.name = this.item.name.replace('#', this.item.enchantMin);
      }

      // Max roll
      if (this.item.name.includes('#') && this.item.enchantMax !== null) {
        this.item.name = this.item.name.replace('#', this.item.enchantMax);
      }
    }

    // Begin builder
    let builder = this.item.name;
    if (this.item.frame === 3) {
      builder = `<span class='item-unique'>${this.item.name}</span>`;
    }

    if (this.item.type) {
      builder += `<span class='subtext-1'>, ${this.item.type}</span>`;
    }

    if (this.item.frame === 3) {
      builder = `<span class='item-unique'>${builder}</span>`;
    } else if (this.item.frame === 9) {
      builder = `<span class='item-foil'>${builder}</span>`;
    } else if (this.item.frame === 8) {
      builder = `<span class='item-prophecy'>${builder}</span>`;
    } else if (this.item.frame === 4) {
      builder = `<span class='item-gem'>${builder}</span>`;
    } else if (this.item.frame === 5) {
      builder = `<span class='item-currency'>${builder}</span>`;
    } else if (this.item.category === 'base') {
      if (this.item.baseIsShaper) {
        builder = `<span class='item-shaper'>${builder}</span>`;
      } else if (this.item.baseIsElder) {
        builder = `<span class='item-elder'>${builder}</span>`;
      }
    }

    return builder;
  }

  /**
   * Creates formatted properties for the item
   *
   * @returns {string}
   */
  formatProperties() {
    // Begin builder
    let builder = '';

    if (this.item.variation) {
      builder += `${this.item.variation}, `;
    }

    if (this.item.category === 'map' && this.item.mapTier) {
      builder += `Tier ${this.item.mapTier}, `;
    }

    if (this.item.baseItemLevel) {
      builder += `iLvl ${this.item.baseItemLevel}, `;
    }

    if (this.item.linkCount) {
      builder += `Links ${this.item.linkCount}, `;
    }

    if (this.item.category === 'gem') {
      builder += `Level ${this.item.gemLevel}, `;
      builder += `Quality ${this.item.gemQuality}, `;

      if (this.item.gemIsCorrupted) {
        builder += "Corrupted, ";
      }
    }

    if (builder) {
      builder = `(${builder.substring(0, builder.length - 2)})`;
    }

    return builder;
  }

  /**
   * Formats price string for the row
   *
   * @returns {string}
   */
  formatPrice() {
    if (this.item.buyout.length > 0) {
      return this.item.buyout[0].price + " " + this.item.buyout[0].currency;
    }

    return '';
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

  /**
   * Builds the table row for the item
   *
   * @returns {string}
   */
  buildRow() {
    return `<tr>
  <td>
    <div class="d-flex align-items-center">
      <div class="img-container img-container-xs text-center mr-1">
        <img src="${this.item.icon}" alt="...">
      </div>
      <div>
        <span class="custom-text-gray-lo">${this.name}</span>
        <span class="badge custom-text-gray p-0">${this.properties}</span>
      </div>
    </div>
  </td>
  <td class="text-nowrap custom-text-gray-lo text-center">
    <span class="badge p-0">${this.item.count}</span>
  </td>
  <td class="text-nowrap custom-text-gray-lo">
    <span class="badge p-0">${this.price}</span>
  </td>
  <td class="text-nowrap custom-text-gray-lo">
    <span class="badge p-0">${Row.timeSince(this.item.discovered)}</span>
  </td>
  <td class="text-nowrap custom-text-gray-lo">
    <span class="badge p-0">${Row.timeSince(this.item.updated)}</span>
  </td>
</tr>`
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

/**
 * Row sorting
 */
class Sorter {
  /**
   * Handles sorting events
   *
   * @param e Event data
   */
  static sortListener(e) {
    const colName = e.target.innerHTML.toLowerCase();
    const target = $(e.target);
    let order = e.target.attributes.order ? e.target.attributes.order.value : null;
    let color = null;

    // Remove all data from all sort columns
    $('.sort-column').attr('class', 'sort-column').attr('order', null);

    target.attr('class', 'sort-column')
      .attr('order', null);

    // Toggle descriptions and orders
    if (!order) {
      order = 'descending';
      color = 'custom-text-green';
    } else if (order === 'descending') {
      order = 'ascending';
      color = 'custom-text-red';
    } else if (order === "ascending") {
      console.log('Sorting: default');
      SEARCH.sortFunction = Sorter.getSortFunc();
      Sorter.sortResults();

      return;
    }

    // Set clicked col's data
    target.addClass(color).attr('order', order);

    console.log(`Sorting: ${colName} ${order}`);
    SEARCH.sortFunction = Sorter.getSortFunc(colName, order);

    Sorter.sortResults();
  }

  static sortResults() {
    const json = SEARCH.results[SEARCH.league + SEARCH.account];
    json.sort(SEARCH.sortFunction);
    fillTable(json);
  }

  /**
   * Get sort function that matches provided params
   *
   * @param col Column name to sort
   * @param order Sort ordering
   * @returns {*} Comparator function with two arguments
   */
  static getSortFunc(col, order) {
    // If the sort function exists
    if (Sorter.SORT_FUNCTIONS[col]) {
      if (Sorter.SORT_FUNCTIONS[col][order]) {
        return Sorter.SORT_FUNCTIONS[col][order];
      }
    }

    // Otherwise return default
    return Sorter.SORT_FUNCTIONS.default[order]
      ? Sorter.SORT_FUNCTIONS.default[order]
      : Sorter.SORT_FUNCTIONS.default.descending;
  }
}

Sorter.SORT_FUNCTIONS = {
  found: {
    ascending: (a, b) => {
      if (a.discovered < b.discovered) return -1;
      if (a.discovered > b.discovered) return 1;
      return 0;
    },
    descending: (a, b) => {
      if (a.discovered > b.discovered) return -1;
      if (a.discovered < b.discovered) return 1;
      return 0;
    }
  },
  default: {
    ascending: (a, b) => {
      if (a.updated < b.updated) return -1;
      if (a.updated > b.updated) return 1;
      return 0;
    },
    descending: (a, b) => {
      if (a.updated > b.updated) return -1;
      if (a.updated < b.updated) return 1;
      return 0;
    }
  },
  price: {
    ascending: (a, b) => {
      if (a.buyout.length === 0 && b.buyout.length > 0) return 1;
      if (b.buyout.length === 0 && a.buyout.length > 0) return -1;
      if (a.buyout.length === 0 && b.buyout.length === 0) return 0;
      if (a.buyout[0].chaos > b.buyout[0].chaos) return -1;
      if (a.buyout[0].chaos < b.buyout[0].chaos) return 1;
      return 0;
    },
    descending: (a, b) => {
      if (a.buyout.length === 0 && b.buyout.length > 0) return -1;
      if (b.buyout.length === 0 && a.buyout.length > 0) return 1;
      if (a.buyout.length === 0 && b.buyout.length === 0) return 0;
      if (a.buyout[0].chaos > b.buyout[0].chaos) return 1;
      if (a.buyout[0].chaos < b.buyout[0].chaos) return -1;
      return 0;
    }
  },
  count: {
    ascending: (a, b) => {
      if (a.count < b.count) return -1;
      if (a.count > b.count) return 1;
      return 0;
    },
    descending: (a, b) => {
      if (a.count > b.count) return -1;
      if (a.count < b.count) return 1;
      return 0;
    }
  },
  item: {
    ascending: (a, b) => {
      if (a.name < b.name) return -1;
      if (a.name > b.name) return 1;
      return 0;
    },
    descending: (a, b) => {
      if (a.name > b.name) return -1;
      if (a.name < b.name) return 1;
      return 0;
    }
  }
};

const API_URL = 'https://api.poe.watch';
const SEARCH = {
  account: null,
  league: null,
  results: {},
  sortFunction: Sorter.getSortFunc()
};

$(document).ready(function () {
  defineListeners();
  parseQueryParams();
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
    if (e.target.value) {
      SEARCH.account = e.target.value;
      console.log('Username: ' + SEARCH.account);
    } else {
      SEARCH.account = null;
      console.log('Username cleared');
    }

    QueryAccessor.updateQueryParam('account', SEARCH.account);
  });

  $('#search-btn').on('click', function (e) {
    QueryAccessor.updateQueryParam('league', SEARCH.league);

    if (!SEARCH.account) {
      statusMsg('Enter an account name', true);
      return;
    } else if (SEARCH.account.length < 3) {
      statusMsg('Account name is too short', true);
      return;
    } else if (SEARCH.account.length > 64) {
      statusMsg('Account name is too long', true);
      return;
    }

    // If same search has already been made
    if (SEARCH.results[SEARCH.league + SEARCH.account] !== undefined) {
      const json = SEARCH.results[SEARCH.league + SEARCH.account];

      statusMsg(`Loaded ${json.length} items from memory`);
      fillTable(json);

      return;
    }

    statusMsg();
    makeGetRequest(SEARCH.league, SEARCH.account);
  });

  $('#search-league').on('change', function (e) {
    SEARCH.league = e.target.value;
    console.log('League: ' + SEARCH.league);
    QueryAccessor.updateQueryParam('league', SEARCH.league);
  });

  $('.sort-column').on('click', Sorter.sortListener);
}


/**
 * Loads and processes query parameters on initial page load
 */
function parseQueryParams() {
  const league = QueryAccessor.parseQueryParam('league');
  if (league) {
    SEARCH.league = league;
    $('#search-league').val(league);
  } else {
    // Get default option from league selector
    SEARCH.league = $('#search-league>option').val();
  }

  const account = QueryAccessor.parseQueryParam('account');
  if (account) {
    $('#search-input').val(account);
    SEARCH.account = account;
  }

  // Run the request if both a league
  // and account name were provided
  if (SEARCH.league && SEARCH.account) {
    makeGetRequest(SEARCH.league, SEARCH.account);
  }
}

/**
 * Makes request to api
 *
 * @param league
 * @param account
 */
function makeGetRequest(league, account) {
  const spinner = $('#spinner');
  spinner.removeClass('d-none');

  let request = $.ajax({
    url: `${API_URL}/listings`,
    data: {
      league: league,
      account: account
    },
    type: "GET",
    async: true,
    dataTypes: "json"
  });

  request.done(function (json) {
    SEARCH.results[league + account] = json;

    if (json.length === 0) {
      statusMsg(`No items found in that league`, true);
    } else {
      statusMsg(`Found ${json.length} items`);
    }

    $('#search-results').removeClass('d-none');

    // Sort descending
    json.sort(SEARCH.sortFunction);
    fillTable(json);
    spinner.addClass('d-none');
  });

  request.fail(function (response) {
    console.log(response);
    SEARCH.results[account] = null;

    statusMsg(response.responseJSON.error);
    spinner.addClass('d-none');
  });
}

/**
 * Fills main table with data
 *
 * @param items
 */
function fillTable(items) {
  const table = $('#search-results > tbody');

  if (!items) {
    table.html();
  }

  let tableRows = [];
  for (let i = 0; i < items.length; i++) {
    if (!items[i].html) {
      items[i].html = new Row(items[i]).buildRow();
    }

    tableRows.push(items[i].html);
  }

  table.html(tableRows.join(''));
}
