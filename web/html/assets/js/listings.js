const spinner = $('#spinner');
const showSpinner = () => spinner.removeClass('d-none');
const hideSpinner = () => spinner.addClass('d-none');
const API_URL = 'https://api.poe.watch';
const SEARCH = {
  account: null,
  league: null,
  results: {}
};

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
  });

  $('#search-btn').on('click', function (e) {
    if (!SEARCH.league) {
      SEARCH.league = $('#search-league')[0].children[0].value
    }

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
    showSpinner();
    makeGetRequest(SEARCH.league, SEARCH.account);
  });

  $('#search-league').on('change', function (e) {
    SEARCH.league = e.target.value;
    console.log('League: ' + SEARCH.league);
  });
}

/**
 * Makes request to api
 *
 * @param league
 * @param account
 */
function makeGetRequest(league, account) {
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

    fillTable(json);
    hideSpinner();
  });

  request.fail(function (response) {
    console.log(response);
    SEARCH.results[account] = null;

    statusMsg(response.responseJSON.error);
    hideSpinner();
  });
}

/**
 * Formats a timestamp string
 *
 * @param timeStamp
 * @returns {string}
 */
function timeSince(timeStamp) {
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
 * Fills main table with data
 *
 * @param items
 */
function fillTable(items) {
  const table = $('#search-results > tbody');
  let tableRows = [];

  for (let i = 0; i < items.length; i++) {
    const name = formatName(items[i]),
      properties = formatProperties(items[i]);
    let price = '';

    if (items[i].buyout.length > 0) {
      price = items[i].buyout[0].price + " " + items[i].buyout[0].currency
    }

    let row = `<tr>
  <td>
    <div class="d-flex align-items-center">
      <div class="img-container img-container-xs text-center mr-1">
        <img src="${items[i].icon}" alt="...">
      </div>
      <div>${name}</div>
    </div>
  </td>
  <td class="text-nowrap custom-text-gray-lo">
    <span class="badge p-0">${properties}</span>
  </td>
  <td class="text-nowrap custom-text-gray-lo">
    <span class="badge p-0">${items[i].count}</span>
  </td>
  <td class="text-nowrap custom-text-gray-lo">
    <span class="badge p-0">${timeSince(items[i].discovered)}</span>
  </td>
  <td class="text-nowrap custom-text-gray-lo">
    <span class="badge p-0">${timeSince(items[i].updated)}</span>
  </td>
  <td class="text-nowrap custom-text-gray-lo">
    <span class="badge p-0">${price}</span>
  </td>
</tr>`;

    tableRows.push(row);
  }

  table.html(tableRows.join(''));
}

/**
 * Creates formatted title for the item
 *
 * @param item
 * @returns {string}
 */
function formatName(item) {
  // If item is enchantment, insert enchant values for display purposes
  if (item.category === 'enchantment') {
    // Min roll
    if (item.name.includes('#') && item.enchantMin !== null) {
      item.name = item.name.replace('#', item.enchantMin);
    }

    // Max roll
    if (item.name.includes('#') && item.enchantMax !== null) {
      item.name = item.name.replace('#', item.enchantMax);
    }
  }

  // Begin builder
  let builder = item.name;

  if (item.type) {
    builder += `<span class='subtext-1'>, ${item.type}</span>`;
  }

  if (item.frame === 9) {
    builder = `<span class='item-foil'>${builder}</span>`;
  } else if (item.category === 'base') {
    if (item.baseIsShaper) {
      builder = `<span class='item-shaper'>${builder}</span>`;
    } else if (item.baseIsElder) {
      builder = `<span class='item-elder'>${builder}</span>`;
    }
  }

  return builder;
}

/**
 * Creates formatted properties for the item
 *
 * @param item
 * @returns {string}
 */
function formatProperties(item) {
  // Begin builder
  let builder = '';

  if (item.variation) {
    builder += `${item.variation}, `;
  }

  if (item.category === 'map' && item.mapTier) {
    builder += `Tier ${item.mapTier}, `;
  }

  if (item.baseItemLevel) {
    builder += `iLvl ${item.baseItemLevel}, `;
  }

  if (item.linkCount) {
    builder += `${item.linkCount} links, `;
  }

  if (item.category === 'gem') {
    builder += `Lvl ${item.gemLevel}, `;
    builder += `${item.gemQuality} quality, `;

    if (item.gemIsCorrupted) {
      builder += "<span class='custom-text-red'>Corrupted</span>, ";
    }
  }

  if (builder) {
    builder = builder.substring(0, builder.length - 2);
  }

  return builder;
}


defineListeners();